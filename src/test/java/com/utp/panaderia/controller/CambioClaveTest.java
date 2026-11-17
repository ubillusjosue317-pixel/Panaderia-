package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
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

import com.utp.panaderia.model.Rol;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.RolRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class CambioClaveTest {

    private static final String EMAIL = "primer.ingreso@pieroalonso.pe";
    private static final String TEMPORAL = "Temporal#2026x";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void elPrimerIngresoObligaACambiarLaClaveAntesDeUsarElSistema() throws Exception {
        String secreto = Totp.generarSecreto();
        Usuario u = new Usuario();
        u.setRol(rolRepository.findByNombre(Rol.CLIENTE).orElseThrow());
        u.setNombre("Nuevo");
        u.setApellido("Admin");
        u.setEmail(EMAIL);
        u.setPassword(passwordEncoder.encode(TEMPORAL));
        u.setTotpSecreto(secreto);
        u.setDebeCambiarPassword(true);
        usuarioRepository.save(u);

        MockHttpSession sesion = (MockHttpSession) mvc.perform(formLogin("/login").user("email", EMAIL).password(TEMPORAL))
                .andExpect(redirectedUrl("/verificacion"))
                .andReturn().getRequest().getSession(false);
        mvc.perform(post("/verificacion").session(sesion).with(csrf()).param("codigo", Totp.codigoActual(secreto)))
                .andExpect(redirectedUrl("/cambiar-clave"));

        // no puede abrir ninguna otra pagina hasta cambiarla
        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/cambiar-clave"));
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(redirectedUrl("/cambiar-clave"));
        mvc.perform(get("/cambiar-clave").session(sesion)).andExpect(status().isOk());

        // la misma clave, una debil o una comun se rechazan
        for (String nueva : new String[] { TEMPORAL, "aaaaaaaa", "Admin#12345" }) {
            mvc.perform(post("/cambiar-clave").session(sesion).with(csrf())
                            .param("actual", TEMPORAL).param("nueva", nueva).param("confirmar", nueva))
                    .andExpect(status().isOk());
        }
        mvc.perform(post("/cambiar-clave").session(sesion).with(csrf())
                        .param("actual", "Incorrecta#1").param("nueva", "Nueva#Segura9").param("confirmar", "Nueva#Segura9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("contraseña actual no es correcta")));
        mvc.perform(get("/").session(sesion)).andExpect(redirectedUrl("/cambiar-clave"));

        // con una clave valida se libera la sesion
        mvc.perform(post("/cambiar-clave").session(sesion).with(csrf())
                        .param("actual", TEMPORAL).param("nueva", "Nueva#Segura9").param("confirmar", "Nueva#Segura9"))
                .andExpect(redirectedUrl("/"));
        mvc.perform(get("/mis-pedidos").session(sesion)).andExpect(status().isOk());

        assertFalse(usuarioRepository.findByEmail(EMAIL).orElseThrow().isDebeCambiarPassword());
        mvc.perform(formLogin("/login").user("email", EMAIL).password(TEMPORAL)).andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("email", EMAIL).password("Nueva#Segura9")).andExpect(authenticated());
        assertTrue(true);
    }
}
