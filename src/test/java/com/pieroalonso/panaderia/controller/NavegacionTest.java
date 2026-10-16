package com.pieroalonso.panaderia.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NavegacionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void elCatalogoEsPublicoYMuestraLosProductos() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pan camote")));
    }

    @Test
    void elCatalogoFiltraPorTexto() throws Exception {
        mvc.perform(get("/").param("q", "croiss"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Croissant")))
                .andExpect(content().string(not(containsString("Pan camote"))));
    }

    @Test
    void loginYRegistroSonPublicos() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/registro")).andExpect(status().isOk());
    }

    @Test
    void elPanelPideIniciarSesion() throws Exception {
        mvc.perform(get("/admin/productos"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "CLIENTE")
    void unClienteNoEntraAlPanel() throws Exception {
        mvc.perform(get("/admin/productos")).andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void elAdministradorVeLaListaDeProductos() throws Exception {
        mvc.perform(get("/admin/productos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo producto")));
        mvc.perform(get("/admin/productos/nuevo")).andExpect(status().isOk());
    }

    @Test
    void elAdministradorInicialPuedeIniciarSesion() throws Exception {
        mvc.perform(formLogin("/login").user("email", "admin@pieroalonso.pe").password("Admin12345"))
                .andExpect(authenticated())
                .andExpect(redirectedUrl("/admin/productos"));
    }

    @Test
    void registroConContrasenasDistintasNoSeGuarda() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Mateo")
                        .param("apellido", "Gómez")
                        .param("email", "mateo@correo.com")
                        .param("password", "clave12345")
                        .param("confirmarPassword", "otraClave1")
                        .param("aceptaTerminos", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las contraseñas no coinciden")));
    }

    @Test
    void registroCorrectoRedirigeAlLogin() throws Exception {
        mvc.perform(post("/registro").with(csrf())
                        .param("nombre", "Lucía")
                        .param("apellido", "Ramos")
                        .param("email", "lucia@correo.com")
                        .param("telefono", "987654321")
                        .param("password", "clave12345")
                        .param("confirmarPassword", "clave12345")
                        .param("aceptaTerminos", "true"))
                .andExpect(redirectedUrl("/login"));

        mvc.perform(formLogin("/login").user("email", "lucia@correo.com").password("clave12345"))
                .andExpect(authenticated().withRoles("CLIENTE"));
    }
}
