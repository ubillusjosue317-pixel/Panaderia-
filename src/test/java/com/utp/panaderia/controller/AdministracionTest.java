package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.MensajeForm;
import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.Mensaje;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.repository.EstadoPedidoRepository;
import com.utp.panaderia.repository.MensajeRepository;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.MensajeService;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.UsuarioService;

@SpringBootTest
@AutoConfigureMockMvc
class AdministracionTest {

    private static final String ADMIN = "admin@pieroalonso.pe";
    private static final String CLIENTE = "cliente.admin.test@correo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EstadoPedidoRepository estadoRepository;

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private MensajeService mensajeService;

    @BeforeEach
    void preparar() {
        if (!usuarioService.existeEmail(CLIENTE)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Carla");
            form.setApellido("Vega");
            form.setEmail(CLIENTE);
            form.setPassword("clave12345");
            form.setConfirmarPassword("clave12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form);
        }
    }

    @Test
    @WithUserDetails(ADMIN)
    void elMenuDelAdministradorNoTieneOpcionesDeCompra() throws Exception {
        mvc.perform(get("/admin/productos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Mensajería")))
                .andExpect(content().string(containsString("/admin/pedidos")))
                .andExpect(content().string(not(containsString("href=\"/favoritos\""))))
                .andExpect(content().string(not(containsString("href=\"/explorar\""))));

        mvc.perform(get("/")).andExpect(redirectedUrl("/admin/productos"));
    }

    @Test
    @WithUserDetails(ADMIN)
    void cambiarPrecioDesdeLaTabla() throws Exception {
        Producto p = producto("Roll de canela");

        mvc.perform(post("/admin/productos/" + p.getId() + "/precio").with(csrf()).param("precio", "3.75"))
                .andExpect(redirectedUrl("/admin/productos"))
                .andExpect(flash().attribute("mensaje", containsString("S/ 3.75")));
        assertEquals(0, new BigDecimal("3.75").compareTo(producto("Roll de canela").getPrecio()));

        mvc.perform(post("/admin/productos/" + p.getId() + "/precio").with(csrf()).param("precio", "-1"))
                .andExpect(flash().attribute("error", containsString("negativo")));
    }

    @Test
    @WithUserDetails(ADMIN)
    void subirImagenDelProducto() throws Exception {
        Producto p = producto("Baguette clásica");
        MockMultipartFile foto = new MockMultipartFile("archivoImagen", "baguette.png", "image/png", new byte[] { 1, 2, 3 });

        mvc.perform(multipart("/admin/productos/guardar").file(foto).with(csrf())
                        .param("id", p.getId().toString())
                        .param("idCategoria", p.getCategoria().getId().toString())
                        .param("nombre", p.getNombre())
                        .param("precio", p.getPrecio().toPlainString())
                        .param("imagenUrl", "/img/productos/pan-frances.jpg")
                        .param("disponible", "true"))
                .andExpect(redirectedUrl("/admin/productos"));

        String nueva = producto("Baguette clásica").getImagenUrl();
        assertTrue(nueva.startsWith("/uploads/productos/") && nueva.endsWith(".png"));
        mvc.perform(get(nueva)).andExpect(status().isOk());
    }

    @Test
    @WithUserDetails(ADMIN)
    void unArchivoQueNoEsImagenSeRechaza() throws Exception {
        Producto p = producto("Croissant");
        MockMultipartFile archivo = new MockMultipartFile("archivoImagen", "virus.exe", "application/octet-stream", new byte[] { 1 });

        mvc.perform(multipart("/admin/productos/guardar").file(archivo).with(csrf())
                        .param("id", p.getId().toString())
                        .param("idCategoria", p.getCategoria().getId().toString())
                        .param("nombre", p.getNombre())
                        .param("precio", p.getPrecio().toPlainString())
                        .param("disponible", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("JPG, PNG o WEBP")));
    }

    @Test
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void elAdministradorVeLosPedidosYCambiaElEstado() throws Exception {
        Integer idCliente = usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId();
        Pedido pedido = pedidoService.registrarPedido(idCliente, Map.of(producto("Pan camote").getId(), 3),
                TipoEntrega.DELIVERY, "Av. Los Olivos 123");
        EstadoPedido listo = estadoRepository.findByNombre(EstadoPedido.LISTO).orElseThrow();

        mvc.perform(get("/admin/pedidos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pedido #" + pedido.getId())))
                .andExpect(content().string(containsString("Carla Vega")))
                .andExpect(content().string(containsString("Av. Los Olivos 123")));

        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/estado").with(csrf())
                        .param("idEstado", listo.getId().toString()))
                .andExpect(redirectedUrl("/admin/pedidos"));

        assertEquals(EstadoPedido.LISTO, pedidoRepository.findById(pedido.getId()).orElseThrow().getEstado().getNombre());

        mvc.perform(get("/admin/pedidos").param("estado", listo.getId().toString()))
                .andExpect(content().string(containsString("Pedido #" + pedido.getId())));
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void elClienteEnviaUnMensaje() throws Exception {
        mvc.perform(post("/mensajes").with(csrf())
                        .param("asunto", "Torta de cumpleaños")
                        .param("contenido", "¿Hacen tortas para 20 personas?"))
                .andExpect(redirectedUrl("/mensajes"));

        mvc.perform(get("/mensajes"))
                .andExpect(content().string(containsString("Torta de cumpleaños")))
                .andExpect(content().string(containsString("En espera")));

        mvc.perform(post("/mensajes").with(csrf()).param("asunto", "").param("contenido", ""))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe el asunto")));
    }

    @Test
    @WithUserDetails(value = ADMIN, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void elAdministradorRespondeUnMensaje() throws Exception {
        MensajeForm form = new MensajeForm();
        form.setAsunto("Horario");
        form.setContenido("¿A qué hora abren los domingos?");
        Mensaje mensaje = mensajeService.enviar(usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId(), form);

        mvc.perform(get("/admin/mensajes").param("pendientes", "true"))
                .andExpect(content().string(containsString("¿A qué hora abren los domingos?")));

        mvc.perform(post("/admin/mensajes/" + mensaje.getId() + "/responder").with(csrf())
                        .param("respuesta", "Abrimos de 7am a 1pm."))
                .andExpect(redirectedUrl("/admin/mensajes"));

        Mensaje respondido = mensajeRepository.findById(mensaje.getId()).orElseThrow();
        assertEquals("Abrimos de 7am a 1pm.", respondido.getRespuesta());
        assertNotNull(respondido.getFechaRespuesta());
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void unClienteNoEntraALasVentanasDelAdministrador() throws Exception {
        mvc.perform(get("/admin/pedidos")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/mensajes")).andExpect(status().isForbidden());
    }

    private Producto producto(String nombre) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals(nombre))
                .findFirst()
                .orElseThrow();
    }
}
