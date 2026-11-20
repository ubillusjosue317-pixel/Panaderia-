package com.utp.panaderia.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.Venta;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.VentaRepository;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.ExcelVentasException;
import com.utp.panaderia.service.VentaService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class VentaServiceImpl implements VentaService {

    private static final long MAXIMO_BYTES = 5 * 1024 * 1024;
    private static final int MAXIMO_FILAS = 5000;
    private static final int MAXIMO_ERRORES = 20;
    private static final int MAXIMA_CANTIDAD = 1000;
    private static final BigDecimal PRECIO_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("999999.99");
    private static final LocalDate FECHA_MINIMA = LocalDate.of(2000, 1, 1);
    private static final DateTimeFormatter FORMATO_DIA_PRIMERO = DateTimeFormatter.ofPattern("d/M/uuuu");

    static {
        // proteccion contra archivos "bomba" (zip que al descomprimirse ocupa muchisimo)
        ZipSecureFile.setMinInflateRatio(0.01);
        ZipSecureFile.setMaxEntrySize(100L * 1024 * 1024);
        ZipSecureFile.setMaxTextSize(20L * 1024 * 1024);
    }

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Override
    @Transactional
    public int cargarExcel(MultipartFile archivo) {
        validarArchivo(archivo);

        Map<String, Producto> catalogo = new HashMap<>();
        for (Producto p : productoRepository.findAll()) {
            catalogo.put(p.getNombre().trim().toLowerCase(Locale.ROOT), p);
        }

        String nombreArchivo = limpiarNombre(archivo.getOriginalFilename());
        String usuario = usuarioActual();
        List<Venta> ventas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        DataFormatter formateador = new DataFormatter(Locale.ROOT);

        try (InputStream entrada = archivo.getInputStream(); Workbook libro = new XSSFWorkbook(entrada)) {
            if (libro.getNumberOfSheets() == 0) {
                throw new ExcelVentasException("El archivo no tiene hojas.");
            }
            Sheet hoja = libro.getSheetAt(0);
            int[] columnas = ubicarColumnas(hoja.getRow(hoja.getFirstRowNum()), formateador);

            int ultima = hoja.getLastRowNum();
            if (ultima - hoja.getFirstRowNum() > MAXIMO_FILAS) {
                throw new ExcelVentasException("El archivo tiene más de " + MAXIMO_FILAS + " filas.");
            }

            for (int i = hoja.getFirstRowNum() + 1; i <= ultima; i++) {
                Row fila = hoja.getRow(i);
                if (fila == null || filaVacia(fila, columnas, formateador)) {
                    continue;
                }
                String numero = "Fila " + (i + 1) + ": ";
                try {
                    ventas.add(leerFila(fila, columnas, catalogo, formateador, nombreArchivo, usuario));
                } catch (IllegalArgumentException e) {
                    if (errores.size() < MAXIMO_ERRORES) {
                        errores.add(numero + e.getMessage());
                    }
                }
            }
        } catch (ExcelVentasException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // archivo danado o que no es un .xlsx de verdad
            throw new ExcelVentasException("No se pudo leer el archivo. Verifica que sea un Excel (.xlsx) válido.");
        }

        if (!errores.isEmpty()) {
            throw new ExcelVentasException("No se cargó nada: corrige estas filas y vuelve a subir el archivo.", errores);
        }
        if (ventas.isEmpty()) {
            throw new ExcelVentasException("El archivo no tiene ventas para cargar.");
        }

        ventaRepository.saveAll(ventas);
        BigDecimal total = ventas.stream().map(Venta::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        auditoriaService.registrar("CARGA_VENTAS", "Venta", null,
                ventas.size() + " ventas desde \"" + nombreArchivo + "\" por S/ " + total);
        return ventas.size();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> ultimas() {
        return ventaRepository.findTop200ByOrderByFechaDescIdDesc();
    }

    @Override
    public long contar() {
        return ventaRepository.count();
    }

    @Override
    public BigDecimal totalGeneral() {
        return ventaRepository.sumaTotal();
    }

    @Override
    public byte[] plantilla() {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");
            Row cabecera = hoja.createRow(0);
            String[] titulos = { "Fecha", "Producto", "Cantidad", "Precio" };
            for (int i = 0; i < titulos.length; i++) {
                cabecera.createCell(i).setCellValue(titulos[i]);
                hoja.setColumnWidth(i, 6000);
            }
            Row ejemplo = hoja.createRow(1);
            ejemplo.createCell(0).setCellValue(LocalDate.now().toString());
            ejemplo.createCell(1).setCellValue("Pan francés");
            ejemplo.createCell(2).setCellValue(10);
            ejemplo.createCell(3).setCellValue(2.00);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la plantilla", e);
        }
    }

    // ---------- validaciones del archivo ----------

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ExcelVentasException("Elige un archivo Excel (.xlsx).");
        }
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!nombre.endsWith(".xlsx")) {
            throw new ExcelVentasException("El archivo debe ser un Excel con extensión .xlsx.");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new ExcelVentasException("El archivo no puede pesar más de 5 MB.");
        }
        // un .xlsx es un zip: debe empezar con "PK"; asi no se acepta cualquier cosa renombrada
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(4);
            if (inicio.length < 4 || inicio[0] != 'P' || inicio[1] != 'K' || inicio[2] != 3 || inicio[3] != 4) {
                throw new ExcelVentasException("El archivo no es un Excel (.xlsx) válido.");
            }
        } catch (IOException e) {
            throw new ExcelVentasException("No se pudo leer el archivo.");
        }
    }

    // Busca las columnas Fecha, Producto, Cantidad y Precio (el precio es opcional)
    private int[] ubicarColumnas(Row cabecera, DataFormatter formateador) {
        int[] columnas = { -1, -1, -1, -1 };
        if (cabecera != null) {
            for (Cell celda : cabecera) {
                String titulo = formateador.formatCellValue(celda).trim().toLowerCase(Locale.ROOT);
                if (titulo.startsWith("fecha")) {
                    columnas[0] = celda.getColumnIndex();
                } else if (titulo.startsWith("producto")) {
                    columnas[1] = celda.getColumnIndex();
                } else if (titulo.startsWith("cantidad")) {
                    columnas[2] = celda.getColumnIndex();
                } else if (titulo.startsWith("precio")) {
                    columnas[3] = celda.getColumnIndex();
                }
            }
        }
        if (columnas[0] < 0 || columnas[1] < 0 || columnas[2] < 0) {
            throw new ExcelVentasException("La primera fila debe tener los títulos: Fecha, Producto, Cantidad y Precio (opcional).");
        }
        return columnas;
    }

    private boolean filaVacia(Row fila, int[] columnas, DataFormatter formateador) {
        for (int columna : columnas) {
            if (columna >= 0 && !formateador.formatCellValue(fila.getCell(columna)).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private Venta leerFila(Row fila, int[] columnas, Map<String, Producto> catalogo, DataFormatter formateador,
                           String nombreArchivo, String usuario) {
        LocalDate fecha = leerFecha(fila.getCell(columnas[0]), formateador);
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("la fecha no puede ser futura.");
        }
        if (fecha.isBefore(FECHA_MINIMA)) {
            throw new IllegalArgumentException("la fecha es demasiado antigua.");
        }

        String nombre = formateador.formatCellValue(fila.getCell(columnas[1])).trim();
        Producto producto = catalogo.get(nombre.toLowerCase(Locale.ROOT));
        if (producto == null) {
            throw new IllegalArgumentException("el producto \"" + limitar(nombre) + "\" no existe en el catálogo.");
        }

        BigDecimal cantidadNumero = leerNumero(fila.getCell(columnas[2]), formateador, "la cantidad");
        if (cantidadNumero == null || cantidadNumero.stripTrailingZeros().scale() > 0
                || cantidadNumero.compareTo(BigDecimal.ONE) < 0
                || cantidadNumero.compareTo(BigDecimal.valueOf(MAXIMA_CANTIDAD)) > 0) {
            throw new IllegalArgumentException("la cantidad debe ser un número entero entre 1 y " + MAXIMA_CANTIDAD + ".");
        }

        BigDecimal precio = columnas[3] >= 0 ? leerNumero(fila.getCell(columnas[3]), formateador, "el precio") : null;
        if (precio == null) {
            precio = producto.getPrecio(); // sin precio en el Excel: se usa el del catalogo
        }
        precio = precio.setScale(2, RoundingMode.HALF_UP);
        if (precio.compareTo(PRECIO_MINIMO) < 0 || precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new IllegalArgumentException("el precio debe estar entre 0.01 y 999999.99.");
        }

        Venta venta = new Venta();
        venta.setFecha(fecha);
        venta.setProducto(producto.getNombre());
        venta.setCantidad(cantidadNumero.intValueExact());
        venta.setPrecioUnitario(precio);
        venta.setTotal(precio.multiply(cantidadNumero).setScale(2, RoundingMode.HALF_UP));
        venta.setArchivo(nombreArchivo);
        venta.setCargadoPor(usuario);
        return venta;
    }

    private LocalDate leerFecha(Cell celda, DataFormatter formateador) {
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            throw new IllegalArgumentException("falta la fecha.");
        }
        if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
            return celda.getLocalDateTimeCellValue().toLocalDate();
        }
        String texto = formateador.formatCellValue(celda).trim();
        try {
            return LocalDate.parse(texto); // 2026-11-20
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(texto, FORMATO_DIA_PRIMERO); // 20/11/2026
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("la fecha \"" + limitar(texto) + "\" no es válida (usa 2026-11-20 o 20/11/2026).");
            }
        }
    }

    // Devuelve null si la celda esta vacia. Las formulas se leen por el ultimo valor calculado.
    private BigDecimal leerNumero(Cell celda, DataFormatter formateador, String que) {
        if (celda == null || celda.getCellType() == CellType.BLANK) {
            return null;
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        try {
            if (tipo == CellType.NUMERIC) {
                return BigDecimal.valueOf(celda.getNumericCellValue());
            }
            String texto = formateador.formatCellValue(celda).trim().replace(',', '.');
            return texto.isEmpty() ? null : new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(que + " no es un número válido.");
        }
    }

    private String limpiarNombre(String original) {
        String nombre = original == null ? "ventas.xlsx" : original;
        nombre = nombre.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).replaceAll("[^A-Za-z0-9._ -]", "_");
        return nombre.length() > 100 ? nombre.substring(nombre.length() - 100) : nombre;
    }

    private String limitar(String texto) {
        String limpio = texto.replaceAll("[\\p{Cntrl}]", " ");
        return limpio.length() > 40 ? limpio.substring(0, 40) + "…" : limpio;
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "sistema";
    }
}
