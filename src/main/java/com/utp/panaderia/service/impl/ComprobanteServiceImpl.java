package com.utp.panaderia.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.service.ComprobanteService;
import com.utp.panaderia.util.FirmaArchivo;

@Service
public class ComprobanteServiceImpl implements ComprobanteService {

    private static final long MAXIMO_BYTES = 5 * 1024 * 1024;

    // extension permitida -> tipo real que debe tener el contenido
    private static final Map<String, String> PERMITIDOS = Map.of(
            "jpg", FirmaArchivo.JPEG,
            "jpeg", FirmaArchivo.JPEG,
            "png", FirmaArchivo.PNG,
            "pdf", FirmaArchivo.PDF);

    // extension con la que se guarda cada tipo real
    private static final Map<String, String> EXTENSION_DE_TIPO = Map.of(
            FirmaArchivo.JPEG, "jpg",
            FirmaArchivo.PNG, "png",
            FirmaArchivo.PDF, "pdf");

    @Value("${app.comprobantes.dir}")
    private String carpeta;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Override
    @Transactional
    public void guardar(Integer idUsuario, Integer idPedido, MultipartFile archivo) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .filter(p -> p.getUsuario().getId().equals(idUsuario))
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ese pedido."));
        if (pedido.getMetodoPago() != MetodoPago.YAPE_PLIN) {
            throw new IllegalArgumentException("Solo los pedidos pagados con Yape / Plin llevan comprobante.");
        }
        if (EstadoPedido.CANCELADO.equals(pedido.getEstado().getNombre())) {
            throw new IllegalArgumentException("El pedido está cancelado.");
        }
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("Elige el archivo del comprobante.");
        }
        if (archivo.getSize() > MAXIMO_BYTES) {
            throw new IllegalArgumentException("El comprobante no puede pesar más de 5 MB.");
        }

        // 1) extension: solo .jpg .jpeg .png .pdf (se mira la ULTIMA, asi "foto.php.png" cuenta como png
        //    y "foto.png.php" se rechaza)
        String nombre = archivo.getOriginalFilename() == null ? "" : archivo.getOriginalFilename();
        int punto = nombre.lastIndexOf('.');
        String extension = punto < 0 ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        String tipoEsperado = PERMITIDOS.get(extension);
        if (tipoEsperado == null) {
            throw new IllegalArgumentException("Solo se aceptan archivos .jpg, .png o .pdf.");
        }

        // 2) tipo REAL por el contenido: debe coincidir con la extension
        String tipoReal;
        try (InputStream entrada = archivo.getInputStream()) {
            tipoReal = FirmaArchivo.detectar(entrada.readNBytes(FirmaArchivo.BYTES_NECESARIOS));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el comprobante", e);
        }
        if (tipoReal == null || !tipoReal.equals(tipoEsperado)) {
            throw new IllegalArgumentException("El archivo no es realmente un " + extension.toUpperCase(Locale.ROOT)
                    + " válido.");
        }

        // 3) se guarda con nombre aleatorio y extension decidida por el sistema, fuera de las carpetas publicas
        String guardado = UUID.randomUUID() + "." + EXTENSION_DE_TIPO.get(tipoReal);
        Path base = raiz();
        try (InputStream entrada = archivo.getInputStream()) {
            Files.createDirectories(base);
            Files.copy(entrada, base.resolve(guardado));
            if (pedido.getComprobante() != null) {
                Files.deleteIfExists(resolver(pedido.getComprobante()));
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo guardar el comprobante", e);
        }
        pedido.setComprobante(guardado);
        pedidoRepository.save(pedido);
    }

    @Override
    public void eliminarArchivo(String nombreGuardado) {
        if (nombreGuardado == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolver(nombreGuardado));
        } catch (IOException | IllegalArgumentException e) {
            // si el archivo ya no estaba, no hay nada que borrar
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Archivo obtener(Integer idUsuario, boolean esAdmin, Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ese comprobante."));
        if (!esAdmin && !pedido.getUsuario().getId().equals(idUsuario)) {
            throw new AccessDeniedException("Ese comprobante no es tuyo");
        }
        if (pedido.getComprobante() == null) {
            throw new IllegalArgumentException("Este pedido no tiene comprobante.");
        }
        try {
            Path ruta = resolver(pedido.getComprobante());
            byte[] contenido = Files.readAllBytes(ruta);
            // el tipo que se devuelve sale del contenido, no del nombre guardado
            String tipo = FirmaArchivo.detectar(contenido);
            if (tipo == null || !EXTENSION_DE_TIPO.containsKey(tipo)) {
                throw new IllegalArgumentException("El comprobante no está disponible.");
            }
            return new Archivo(contenido, tipo, EXTENSION_DE_TIPO.get(tipo));
        } catch (IOException e) {
            throw new IllegalArgumentException("El comprobante no está disponible.");
        }
    }

    private Path raiz() {
        return Paths.get(carpeta).toAbsolutePath().normalize();
    }

    // Convierte el nombre guardado en una ruta, comprobando que sea un nombre generado por el sistema
    // y que no se salga de la carpeta (evita "../" aunque alguien alterara la base de datos)
    private Path resolver(String guardado) {
        if (!guardado.matches("^[0-9a-f-]{36}\\.(jpg|png|pdf)$")) {
            throw new IllegalArgumentException("El comprobante no está disponible.");
        }
        Path ruta = raiz().resolve(guardado).normalize();
        if (!ruta.startsWith(raiz())) {
            throw new IllegalArgumentException("El comprobante no está disponible.");
        }
        return ruta;
    }
}
