package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.RolRepository;
import com.utp.panaderia.repository.UsuarioRepository;
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

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
    void sinElCodigoNoSePuedeEntrarANingunaPagina() throws Exception {
        String email = crearCliente("sin.codigo@correo.com");
        MockHttpSession sesion = iniciarSesion(email);

        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/verificacion"));
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(redirectedUrl("/verificacion"));
        mvc.perform(get("/verificacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data:image/png"))));
    }

    @Test
    void unaCuentaSinClaveDeAuthenticatorNoPuedeVincularUnCelular() throws Exception {
        // cuenta antigua sin clave: quien sepa la contraseña NO puede escanear un QR nuevo
        Usuario u = new Usuario();
        u.setRol(rolRepository.findByNombre(Rol.CLIENTE).orElseThrow());
        u.setNombre("Sin");
        u.setApellido("Clave");
        u.setEmail("sin.clave@correo.com");
        u.setPassword(passwordEncoder.encode("Clave#12345"));
        usuarioRepository.save(u);

        MockHttpSession sesion = iniciarSesion("sin.clave@correo.com");
        mvc.perform(get("/verificacion").session(sesion))
                .andExpect(redirectedUrl("/login?sinClave"));
        assertNull(usuarioRepository.findByEmail("sin.clave@correo.com").orElseThrow().getTotpSecreto());
    }

    @Test
    void siguientesIngresosSoloPidenElCodigo() throws Exception {
        String email = crearCliente("ya.configurado@correo.com");
        String secreto = usuarioRepository.findByEmail(email).orElseThrow().getTotpSecreto();

        MockHttpSession sesion = iniciarSesion(email);
        mvc.perform(get("/verificacion").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("data:image/png"))));

        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void elAdministradorDebeCambiarSuClaveDespuesDelCodigo() throws Exception {
        MockHttpSession sesion = iniciarSesion("admin@pieroalonso.pe", "Admin12345");
        mvc.perform(get("/admin/productos").session(sesion)).andExpect(redirectedUrl("/verificacion"));
        mvc.perform(get("/verificacion").session(sesion));

        // el administrador ya nace con su propia clave de Authenticator
        String secreto = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow().getTotpSecreto();
        assertNotNull(secreto);
        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/cambiar-clave"));
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
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        return email;
    }

    private MockHttpSession iniciarSesion(String email) throws Exception {
        return iniciarSesion(email, "Clave#12345");
    }

    private MockHttpSession iniciarSesion(String email, String password) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", email).password(password))
                .andExpect(redirectedUrl("/verificacion"))
                .andReturn().getRequest().getSession(false);
    }
}
