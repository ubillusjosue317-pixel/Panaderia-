package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.repository.VentaRepository;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.session.Carrito;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class NovedadesTest {

    private static final String CLIENTE = "novedades@correo.com";
    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    private Integer idCliente;

    @BeforeEach
    void preparar() {
        if (!usuarioService.existeEmail(CLIENTE)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Nora");
            form.setApellido("Salas");
            form.setEmail(CLIENTE);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        idCliente = usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId();
    }

    private Integer idProducto(String nombre) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }

    private UsuarioAutenticado cliente() {
        return new UsuarioAutenticado(usuarioRepository.findByEmail(CLIENTE).orElseThrow(), false);
    }

    // ---------- metodo de pago solo en delivery ----------

    @Test
    void elDeliveryExigeMetodoDePagoYElRecojoNo() {
        Map<Integer, Integer> items = Map.of(idProducto("Croissant"), 2);

        try {
            pedidoService.registrarPedido(idCliente, items, TipoEntrega.DELIVERY, "Av. Lima 123", null);
            throw new AssertionError("debio exigir el metodo de pago");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("método de pago"));
        }

        Pedido delivery = pedidoService.registrarPedido(idCliente, items, TipoEntrega.DELIVERY, "Av. Lima 123",
                MetodoPago.YAPE_PLIN);
        assertEquals(MetodoPago.YAPE_PLIN, delivery.getMetodoPago());

        // en recojo en tienda se ignora aunque lo manden
        Pedido recojo = pedidoService.registrarPedido(idCliente, items, TipoEntrega.RECOJO_TIENDA, null,
                MetodoPago.TARJETA);
        assertEquals(null, recojo.getMetodoPago());
    }

    @Test
    void elCarritoMuestraElSelectorDeMetodoDePago() throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/carrito/agregar/" + idProducto("Croissant")).session(sesion).with(csrf()));

        // sin iniciar sesion no sale el formulario de confirmar
        mvc.perform(get("/pedidos").session(sesion))
                .andExpect(content().string(not(containsString("Tarjeta contra entrega"))));
        // con sesion de cliente aparece, con las opciones de pago
        mvc.perform(get("/pedidos").session(sesion).with(org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.user(cliente())))
                .andExpect(content().string(containsString("Tarjeta contra entrega")))
                .andExpect(content().string(containsString("Yape / Plin")));
    }

    // ---------- cancelar pedido ----------

    @Test
    void elClienteCancelaSuPedidoPendienteYNoElDeOtro() throws Exception {
        Pedido pedido = pedidoService.registrarPedido(idCliente, Map.of(idProducto("Croissant"), 1),
                TipoEntrega.RECOJO_TIENDA, null, null);

        // otro usuario no puede cancelarlo
        MockHttpSession ajena = new MockHttpSession();
        mvc.perform(post("/mis-pedidos/" + pedido.getId() + "/cancelar").session(ajena).with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(otroCliente())))
                .andExpect(flash().attribute("error", containsString("No encontramos")));
        assertEquals(EstadoPedido.PENDIENTE, pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado().getNombre());

        mvc.perform(post("/mis-pedidos/" + pedido.getId() + "/cancelar").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(cliente())))
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attribute("mensaje", containsString("cancelado")));
        assertEquals(EstadoPedido.CANCELADO, pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado().getNombre());

        // ya cancelado: no se puede volver a cancelar
        mvc.perform(post("/mis-pedidos/" + pedido.getId() + "/cancelar").with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(cliente())))
                .andExpect(flash().attribute("error", containsString("Pendientes")));
    }

    private UsuarioAutenticado otroCliente() {
        String email = "otro.novedades@correo.com";
        if (!usuarioService.existeEmail(email)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Otro");
            form.setApellido("Cliente");
            form.setEmail(email);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        return new UsuarioAutenticado(usuarioRepository.findByEmail(email).orElseThrow(), false);
    }

    // ---------- volver a pedir ----------

    @Test
    void volverAPedirPoneLosMismosProductosEnLaCesta() throws Exception {
        Integer id = idProducto("Pan camote");
        Pedido pedido = pedidoService.registrarPedido(idCliente, Map.of(id, 3),
                TipoEntrega.RECOJO_TIENDA, null, null);

        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/mis-pedidos/" + pedido.getId() + "/volver-a-pedir").session(sesion).with(csrf())
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(cliente())))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("mensaje", containsString("3 unidades")));
    }

    @Test
    void elCarritoRespetaElMaximoAlAgregarVariasUnidades() {
        Carrito carrito = new Carrito();
        assertEquals(15, carrito.agregar(1, 15));
        assertEquals(5, carrito.agregar(1, 15));
        assertEquals(0, carrito.agregar(1, 3));
        assertEquals(Carrito.MAXIMO_POR_PRODUCTO, carrito.getItems().get(1));
    }

    // ---------- filtro y retroceder ----------

    @Test
    void elBotonDeFiltroAbreUnMenuConLasCategorias() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("data-bs-toggle=\"dropdown\"")))
                .andExpect(content().string(containsString("Filtrar por categoría")))
                .andExpect(content().string(containsString("/explorar?categoria=")));
    }

    @Test
    void siRetrocedeDesdeLaVerificacionPuedeAbrirElRegistro() throws Exception {
        MockHttpSession sesion = (MockHttpSession) mvc.perform(
                        org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders
                                .formLogin("/login").user("email", CLIENTE).password("Clave#12345"))
                .andExpect(redirectedUrl("/verificacion"))
                .andReturn().getRequest().getSession(false);

        // antes se quedaba atrapado: ahora /registro abre normal y cancela la sesion a medias
        mvc.perform(get("/registro").session(sesion)).andExpect(status().isOk());
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    // ---------- carga de ventas desde Excel ----------

    private byte[] excel(String[] titulos, Object[][] filas) throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");
            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < titulos.length; i++) {
                cabecera.createCell(i).setCellValue(titulos[i]);
            }
            for (int r = 0; r < filas.length; r++) {
                Row fila = hoja.createRow(r + 1);
                for (int c = 0; c < filas[r].length; c++) {
                    Object valor = filas[r][c];
                    if (valor instanceof Number n) {
                        fila.createCell(c).setCellValue(n.doubleValue());
                    } else if (valor != null) {
                        fila.createCell(c).setCellValue(valor.toString());
                    }
                }
            }
            libro.write(salida);
            return salida.toByteArray();
        }
    }

    private String[] titulos() {
        return new String[] { "Fecha", "Producto", "Cantidad", "Precio" };
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void elAdministradorCargaVentasDesdeExcel() throws Exception {
        long antes = ventaRepository.count();
        String hoy = LocalDate.now().toString();
        byte[] archivo = excel(titulos(), new Object[][] {
                { hoy, "Pan francés", 10, 2.0 },
                { "01/11/2026", "croissant", 3, null } }); // sin precio: usa el del catalogo, y no distingue mayusculas

        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "ventas.xlsx", XLSX, archivo))
                        .with(csrf()))
                .andExpect(redirectedUrl("/admin/ventas"))
                .andExpect(flash().attribute("mensaje", containsString("2 ventas")));
        assertEquals(antes + 2, ventaRepository.count());

        mvc.perform(get("/admin/ventas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pan francés")));
        mvc.perform(get("/admin/ventas/plantilla.xlsx")).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void siUnaFilaTieneErroresNoSeCargaNada() throws Exception {
        long antes = ventaRepository.count();
        byte[] archivo = excel(titulos(), new Object[][] {
                { LocalDate.now().toString(), "Pan francés", 5, 2.0 },
                { LocalDate.now().toString(), "Producto que no existe", 1, 1.0 },
                { LocalDate.now().plusDays(3).toString(), "Croissant", 1, 1.0 },
                { LocalDate.now().toString(), "Croissant", -4, 1.0 },
                { LocalDate.now().toString(), "Croissant", 2, 0 } });

        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "mal.xlsx", XLSX, archivo))
                        .with(csrf()))
                .andExpect(redirectedUrl("/admin/ventas"))
                .andExpect(flash().attribute("error", containsString("No se cargó nada")))
                .andExpect(flash().attributeExists("errores"));
        assertEquals(antes, ventaRepository.count());
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void soloSeAceptanExcelsXlsxValidos() throws Exception {
        long antes = ventaRepository.count();

        // otra extension
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "ventas.csv", "text/csv",
                        "Fecha,Producto".getBytes())).with(csrf()))
                .andExpect(flash().attribute("error", containsString(".xlsx")));
        // extension .xlsx pero el contenido es otra cosa
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "falso.xlsx", XLSX,
                        "esto no es un excel".getBytes())).with(csrf()))
                .andExpect(flash().attribute("error", containsString("no es un Excel")));
        // un zip que no es un Excel
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "zip.xlsx", XLSX,
                        new byte[] { 'P', 'K', 3, 4, 0, 0, 0, 0 })).with(csrf()))
                .andExpect(flash().attribute("error", containsString("No se pudo leer")));
        // sin los titulos esperados
        byte[] sinTitulos = excel(new String[] { "A", "B", "C" }, new Object[][] { { "x", "y", 1 } });
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "t.xlsx", XLSX, sinTitulos))
                        .with(csrf()))
                .andExpect(flash().attribute("error", containsString("títulos")));
        // sin archivo
        mvc.perform(multipart("/admin/ventas/cargar").with(csrf()))
                .andExpect(flash().attribute("error", containsString("Elige un archivo")));

        assertEquals(antes, ventaRepository.count());
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unClienteNoPuedeEntrarNiCargarVentas() throws Exception {
        mvc.perform(get("/admin/ventas")).andExpect(status().isForbidden());
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "v.xlsx", XLSX,
                        new byte[] { 'P', 'K', 3, 4 })).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/ventas/plantilla.xlsx")).andExpect(status().isForbidden());
    }

    @Test
    void sinIniciarSesionNoSePuedeCargarVentas() throws Exception {
        mvc.perform(get("/admin/ventas")).andExpect(status().is3xxRedirection());
        mvc.perform(multipart("/admin/ventas/cargar").file(new MockMultipartFile("archivo", "v.xlsx", XLSX,
                        new byte[] { 'P', 'K', 3, 4 })).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
