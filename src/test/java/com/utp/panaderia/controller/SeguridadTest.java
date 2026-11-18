package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroPendiente;
import com.utp.panaderia.repository.AuditoriaRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.session.Carrito;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class SeguridadTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void tras5FallosLaCuentaSeBloqueaAunConLaClaveCorrecta() throws Exception {
        String email = "bloqueo@correo.com";
        registrar(email);

        for (int i = 0; i < 5; i++) {
            mvc.perform(formLogin("/login").user("email", email).password("Incorrecta1"))
                    .andExpect(unauthenticated());
        }
        mvc.perform(formLogin("/login").user("email", email).password("Clave#12345"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?cuentaBloqueada"));
    }

    @Test
    void contrasenaDebilSeRechaza() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Ana").param("apellido", "Paz")
                        .param("email", "debil@correo.com")
                        .param("password", "aaaaaaaa").param("confirmarPassword", "aaaaaaaa")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("al menos una mayúscula")));
    }

    @Test
    void correoYaRegistradoDaLaMismaRespuestaQueUnoNuevo() throws Exception {
        String nuevo = registroCompleto("nuevo.enum@correo.com");
        String existente = registroCompleto("admin@pieroalonso.pe");
        assertEquals(nuevo, existente);
    }

    @Test
    void elQrSoloLoVeQuienSeRegistra() throws Exception {
        // otra sesion (otra persona) no puede ver el QR de un registro ajeno
        MockHttpSession ajena = new MockHttpSession();
        mvc.perform(get("/registro/verificacion").session(ajena))
                .andExpect(redirectedUrl("/registro"));
    }

    @Test
    void elCarritoLimitaLaCantidadPorProducto() {
        Carrito carrito = new Carrito();
        for (int i = 0; i < Carrito.MAXIMO_POR_PRODUCTO; i++) {
            assertTrue(carrito.agregar(1));
        }
        assertFalse(carrito.agregar(1));
        assertEquals(Carrito.MAXIMO_POR_PRODUCTO, carrito.getItems().get(1));
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void precioCeroYImagenExternaSeRechazan() throws Exception {
        mvc.perform(post("/admin/productos/guardar").with(csrf())
                        .param("idCategoria", "1").param("nombre", "Gratis").param("precio", "0.00"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El precio mínimo es S/ 0.01")));

        mvc.perform(post("/admin/productos/guardar").with(csrf())
                        .param("idCategoria", "1").param("nombre", "Externa").param("precio", "1.50")
                        .param("imagenUrl", "https://sitio-malo.example/foto.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no se aceptan enlaces externos")));
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void cambiarElPrecioDejaRastroDeQuienYCuando() throws Exception {
        Integer id = productoRepository.findAll().get(0).getId();
        long antes = auditoriaRepository.count();

        mvc.perform(post("/admin/productos/" + id + "/precio").with(csrf()).param("precio", "5.55"))
                .andExpect(status().is3xxRedirection());

        assertEquals(antes + 1, auditoriaRepository.count());
        var ultimo = auditoriaRepository.findAll().stream()
                .reduce((a, b) -> b).orElseThrow();
        assertEquals("admin@pieroalonso.pe", ultimo.getUsuario());
        assertEquals("PRECIO", ultimo.getAccion());
        assertTrue(ultimo.getDetalle().contains("5.55"));
        assertTrue(ultimo.getFecha() != null);
    }

    @Test
    void laRespuestaIncluyeCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-Content-Type-Options"));
    }

    @Test
    void bootstrapSeSirveDesdeElPropioSitioSinCdn() throws Exception {
        String html = mvc.perform(get("/login")).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("cdn.jsdelivr.net"));
        assertTrue(html.contains("/vendor/bootstrap/css/bootstrap.min.css"));
        assertTrue(html.contains("/vendor/bootstrap/js/bootstrap.bundle.min.js"));

        mvc.perform(get("/vendor/bootstrap/css/bootstrap.min.css")).andExpect(status().isOk());
        mvc.perform(get("/vendor/bootstrap/js/bootstrap.bundle.min.js")).andExpect(status().isOk());
        mvc.perform(get("/vendor/bootstrap-icons/bootstrap-icons.min.css")).andExpect(status().isOk());
        mvc.perform(get("/vendor/bootstrap-icons/fonts/bootstrap-icons.woff2")).andExpect(status().isOk());
    }

    @Test
    void laCspYaNoPermiteElCdn() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(header().string("Content-Security-Policy", not(containsString("jsdelivr"))));
    }

    @Test
    void claveSinSimboloOComunSeRechaza() throws Exception {
        for (String clave : new String[] { "Clave12345", "Admin#12345", "Password#1" }) {
            mvc.perform(post("/registro").with(csrf())
                            .param("nombre", "Ana").param("apellido", "Paz")
                            .param("email", "clave.mala@correo.com")
                            .param("password", clave).param("confirmarPassword", clave)
                            .param("aceptaTerminos", "true"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("Recibimos tu registro"))));
        }
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Ana").param("apellido", "Paz")
                        .param("email", "clave.mala@correo.com")
                        .param("password", "Admin#12345").param("confirmarPassword", "Admin#12345")
                        .param("aceptaTerminos", "true"))
                .andExpect(content().string(containsString("muy común")));
    }

    @Test
    void unArchivoQueElNavegadorPideSoloNoEsElDestinoDespuesDelLogin() throws Exception {
        String email = "destino.archivo@correo.com";
        registrar(email);
        String secreto = usuarioRepository.findByEmail(email).orElseThrow().getTotpSecreto();

        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(get("/sw.js").session(sesion)).andExpect(status().is3xxRedirection());
        mvc.perform(post("/login").session(sesion).with(csrf())
                        .param("email", email).param("password", "Clave#12345"))
                .andExpect(redirectedUrl("/verificacion"));
        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void laPaginaDeErrorNoMuestraDetallesTecnicos() throws Exception {
        String html = mvc.perform(get("/no-existe-esta-pagina")).andReturn().getResponse().getContentAsString();
        assertFalse(html.contains("org.springframework"));
        assertFalse(html.contains("Whitelabel"));
    }

    private void registrar(String email) throws Exception {
        registroCompleto(email);
    }

    // hace los dos pasos del registro (datos + codigo del Authenticator) y devuelve el redirect final
    private String registroCompleto(String email) throws Exception {
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(post("/registro").session(sesion).with(csrf())
                .param("nombre", "Test").param("apellido", "Uno").param("email", email)
                .param("password", "Clave#12345").param("confirmarPassword", "Clave#12345")
                .param("aceptaTerminos", "true"))
                .andExpect(redirectedUrl("/registro/verificacion"));
        RegistroPendiente pendiente = (RegistroPendiente) sesion.getAttribute(AuthController.REGISTRO_PENDIENTE);
        return mvc.perform(post("/registro/verificacion").session(sesion).with(csrf())
                        .param("codigo", Totp.codigoActual(pendiente.getSecreto())))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
    }
}
