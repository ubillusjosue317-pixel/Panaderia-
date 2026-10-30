package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.security.VerificacionDosPasosFilter;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class VerificacionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void elCodigoGeneradoEsValidoYOtroNo() {
        String secreto = Totp.generarSecreto();
        String codigo = Totp.codigoActual(secreto);

        assertEquals(32, secreto.length());
        assertTrue(Totp.esValido(secreto, codigo));
        assertFalse(Totp.esValido(secreto, "12345"));
        assertFalse(Totp.esValido(secreto, "abcdef"));
        assertFalse(Totp.esValido(Totp.generarSecreto(), codigo));
    }

    @Test
    void primerIngresoMuestraQrYAlConfirmarQuedaActivado() throws Exception {
        String email = crearCliente("primer.ingreso@correo.com");
        MockHttpSession sesion = iniciarSesion(email);

        // sin el codigo no puede entrar a ninguna pagina
        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/verificacion"));
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(redirectedUrl("/verificacion"));

        mvc.perform(get("/verificacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data:image/png;base64,")));

        String secreto = (String) sesion.getAttribute(VerificacionDosPasosFilter.SECRETO_NUEVO);
        assertNotNull(secreto);

        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/"));

        assertEquals(secreto, usuarioRepository.findByEmail(email).orElseThrow().getTotpSecreto());
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void siguientesIngresosSoloPidenElCodigo() throws Exception {
        String email = crearCliente("ya.configurado@correo.com");
        String secreto = Totp.generarSecreto();
        usuarioService.activarDosPasos(usuarioRepository.findByEmail(email).orElseThrow().getId(), secreto);

        MockHttpSession sesion = iniciarSesion(email);
        mvc.perform(get("/verificacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data:image/png"))));

        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void elAdministradorVaAlPanelDespuesDelCodigo() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@pieroalonso.pe", "Admin12345");
        mvc.perform(get("/admin/productos").session(sesion)).andExpect(redirectedUrl("/verificacion"));
        mvc.perform(get("/verificacion").session(sesion));

        String secreto = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow().getTotpSecreto();
        if (secreto == null) {
            secreto = (String) sesion.getAttribute(VerificacionDosPasosFilter.SECRETO_NUEVO);
        }
        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/admin/productos"));
    }

    @Test
    void cincoCodigosMalosCierranLaSesion() throws Exception {
        String email = crearCliente("codigos.malos@correo.com");
        MockHttpSession sesion = iniciarSesion(email);
        mvc.perform(get("/verificacion").session(sesion));

        for (int i = 1; i <= 4; i++) {
            mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", "000000"))
                    .andExpect(redirectedUrl("/verificacion"));
        }
        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", "000000"))
                .andExpect(redirectedUrl("/login?bloqueado"));
    }

    private String crearCliente(String email) {
        if (!usuarioService.existeEmail(email)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Ana");
            form.setApellido("Torres");
            form.setEmail(email);
            form.setPassword("clave12345");
            form.setConfirmarPassword("clave12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form);
        }
        return email;
    }

    private MockHttpSession iniciarSesion(String email) throws Exception {
        return iniciarSesion(email, "clave12345");
    }

    private MockHttpSession iniciarSesion(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", email).password(password))
                .andExpect(redirectedUrl("/verificacion"))
                .andReturn().getRequest().getSession(false);
    }
}
