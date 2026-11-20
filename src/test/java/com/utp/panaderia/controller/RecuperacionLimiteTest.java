package com.utp.panaderia.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.service.CorreoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

// El limite real de produccion: 3 enlaces cada 15 minutos por correo
@SpringBootTest(properties = "app.recuperacion.max-solicitudes=3")
@AutoConfigureMockMvc
class RecuperacionLimiteTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CorreoService correoService;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void noSePuedenPedirMuchosEnlacesParaElMismoCorreo() throws Exception {
        String email = "limite.recupera@correo.com";
        RegistroForm form = new RegistroForm();
        form.setNombre("Lu");
        form.setApellido("Vega");
        form.setEmail(email);
        form.setPassword("Clave#12345");
        form.setConfirmarPassword("Clave#12345");
        form.setAceptaTerminos(true);
        usuarioService.registrarCliente(form, Totp.generarSecreto());

        for (int i = 0; i < 8; i++) {
            mvc.perform(post("/recuperar").with(csrf()).param("email", email));
        }
        // solo salieron 3 correos; las demas peticiones se ignoraron sin avisar
        verify(correoService, times(3)).enviarRecuperacion(eq(email), anyString(), anyString());
    }
}
