package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.TokenRecuperacion;
import com.utp.panaderia.repository.TokenRecuperacionRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.CorreoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class RecuperacionTest {

    private static final String EMAIL = "recupera@correo.com";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CorreoService correoService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @BeforeEach
    void preparar() {
        reset(correoService);
        if (!usuarioService.existeEmail(EMAIL)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Rita");
            form.setApellido("Mejia");
            form.setEmail(EMAIL);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        // la cuenta puede haberse bloqueado o cambiado en una prueba anterior de esta clase
        usuarioRepository.findByEmail(EMAIL).ifPresent(u -> {
            u.setPassword(usuarioService.cifrarPassword("Clave#12345"));
            usuarioRepository.save(u);
        });
    }

    // pide el enlace y devuelve el token que viajaria en el correo
    private String pedirToken() throws Exception {
        mvc.perform(post("/recuperar").with(csrf()).param("email", EMAIL));
        ArgumentCaptor<String> enlace = ArgumentCaptor.forClass(String.class);
        verify(correoService).enviarRecuperacion(eq(EMAIL), anyString(), enlace.capture());
        String url = enlace.getValue();
        assertTrue(url.startsWith("http://localhost:8080/recuperar/restablecer?token="));
        return url.substring(url.indexOf("token=") + 6);
    }

    @Test
    void laRespuestaEsLaMismaExistaONoElCorreo() throws Exception {
        String conCuenta = (String) mvc.perform(post("/recuperar").with(csrf()).param("email", EMAIL))
                .andExpect(redirectedUrl("/login"))
                .andReturn().getFlashMap().get("mensaje");
        String sinCuenta = (String) mvc.perform(post("/recuperar").with(csrf()).param("email", "noexiste@correo.com"))
                .andExpect(redirectedUrl("/login"))
                .andReturn().getFlashMap().get("mensaje");
        assertEquals(conCuenta, sinCuenta);
        verify(correoService, never()).enviarRecuperacion(eq("noexiste@correo.com"), anyString(), anyString());
    }

    @Test
    void elTokenEsAleatorioLargoYSoloSeGuardaSuHash() throws Exception {
        String t1 = pedirToken();
        reset(correoService);
        String t2 = pedirToken();

        assertNotEquals(t1, t2);
        assertTrue(t1.length() >= 43, "256 bits en base64: al menos 43 caracteres");
        // en la base de datos no esta el token, solo su hash SHA-256 (64 hex)
        assertTrue(tokenRepository.findByTokenHash(t1).isEmpty());
        assertTrue(tokenRepository.findAll().stream().allMatch(t -> t.getTokenHash().matches("^[0-9a-f]{64}$")));
    }

    @Test
    void abrirElEnlaceQuitaElTokenDeLaDireccionYSePuedeCambiarLaClave() throws Exception {
        String token = pedirToken();
        MockHttpSession sesion = new MockHttpSession();

        // el token deja de verse en la URL (barra, historial y Referer)
        mvc.perform(get("/recuperar/restablecer").param("token", token).session(sesion))
                .andExpect(redirectedUrl("/recuperar/restablecer"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
        mvc.perform(get("/recuperar/restablecer").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elige tu nueva contraseña")))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        mvc.perform(post("/recuperar/restablecer").session(sesion).with(csrf())
                        .param("nueva", "Horno#Caliente_58").param("confirmar", "Horno#Caliente_58"))
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attribute("mensaje", containsString("contraseña fue cambiada")));

        // la contraseña nueva funciona y la vieja no
        mvc.perform(formLogin("/login").user("email", EMAIL).password("Horno#Caliente_58")).andExpect(authenticated());
        mvc.perform(formLogin("/login").user("email", EMAIL).password("Clave#12345"))
                .andExpect(org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated());
    }

    @Test
    void elTokenSoloSirveUnaVez() throws Exception {
        String token = pedirToken();
        MockHttpSession primera = new MockHttpSession();
        mvc.perform(get("/recuperar/restablecer").param("token", token).session(primera));
        mvc.perform(post("/recuperar/restablecer").session(primera).with(csrf())
                        .param("nueva", "Horno#Caliente_59").param("confirmar", "Horno#Caliente_59"))
                .andExpect(redirectedUrl("/login"));

        // el mismo enlace otra vez: ya no vale
        MockHttpSession segunda = new MockHttpSession();
        mvc.perform(get("/recuperar/restablecer").param("token", token).session(segunda))
                .andExpect(redirectedUrl("/recuperar"))
                .andExpect(flash().attribute("error", containsString("no es válido")));
    }

    @Test
    void unTokenVencidoOInventadoNoSirve() throws Exception {
        String token = pedirToken();
        TokenRecuperacion registro = tokenRepository.findAll().stream()
                .filter(t -> !t.isUsado() && t.getUsuario().getEmail().equals(EMAIL)).findFirst().orElseThrow();
        registro.setExpiraEn(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(registro);

        mvc.perform(get("/recuperar/restablecer").param("token", token))
                .andExpect(redirectedUrl("/recuperar"));
        mvc.perform(get("/recuperar/restablecer").param("token", "inventado-0123456789"))
                .andExpect(redirectedUrl("/recuperar"));
        // sin abrir antes un enlace valido tampoco se puede mostrar ni enviar el formulario
        mvc.perform(get("/recuperar/restablecer")).andExpect(redirectedUrl("/recuperar"));
        mvc.perform(post("/recuperar/restablecer").with(csrf())
                        .param("nueva", "Horno#Caliente_60").param("confirmar", "Horno#Caliente_60"))
                .andExpect(redirectedUrl("/recuperar"));
    }

    @Test
    void unEnlaceNuevoInvalidaElAnterior() throws Exception {
        String viejo = pedirToken();
        reset(correoService);
        String nuevo = pedirToken();

        mvc.perform(get("/recuperar/restablecer").param("token", viejo)).andExpect(redirectedUrl("/recuperar"));
        mvc.perform(get("/recuperar/restablecer").param("token", nuevo)).andExpect(redirectedUrl("/recuperar/restablecer"));
    }

    @Test
    void laContrasenaNuevaDebeCumplirLaPolitica() throws Exception {
        String token = pedirToken();
        MockHttpSession sesion = new MockHttpSession();
        mvc.perform(get("/recuperar/restablecer").param("token", token).session(sesion));

        for (String debil : new String[] { "aaaaaaaa", "Clave12345", "Admin#12345" }) {
            mvc.perform(post("/recuperar/restablecer").session(sesion).with(csrf())
                            .param("nueva", debil).param("confirmar", debil))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/recuperar/restablecer").session(sesion).with(csrf())
                        .param("nueva", "Horno#Caliente_61").param("confirmar", "Otra#Distinta_1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("no coinciden")));
        // despues de los intentos fallidos el token sigue vigente (no se gasto)
        mvc.perform(get("/recuperar/restablecer").session(sesion)).andExpect(status().isOk());
    }

    @Test
    void laPaginaDeRecuperarEsPublica() throws Exception {
        mvc.perform(get("/recuperar")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Olvidaste tu contraseña")));
        mvc.perform(get("/login")).andExpect(content().string(containsString("/recuperar")));
    }
}
