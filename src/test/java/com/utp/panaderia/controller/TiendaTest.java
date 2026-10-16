package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.TestExecutionEvent;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.session.Carrito;

@SpringBootTest
@AutoConfigureMockMvc
class TiendaTest {

    private static final String CLIENTE = "cliente.prueba@correo.com";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioService usuarioService;

    private MockHttpSession sesion;

    @BeforeEach
    void preparar() {
        sesion = new MockHttpSession();
        if (!usuarioService.existeEmail(CLIENTE)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Rosa");
            form.setApellido("Quispe");
            form.setEmail(CLIENTE);
            form.setPassword("clave12345");
            form.setConfirmarPassword("clave12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form);
        }
    }

    @Test
    void laRutaAntiguaDelCatalogoRedirigeAExplorar() throws Exception {
        mvc.perform(get("/catalogo").param("q", "pan"))
                .andExpect(redirectedUrl("/explorar?q=pan"));
    }

    @Test
    void agregarAlCarritoVuelveALaPaginaYSeVeEnPedidos() throws Exception {
        Integer id = idDe("Croissant");

        mvc.perform(post("/carrito/agregar/" + id).session(sesion).with(csrf())
                        .header("Referer", "http://localhost/explorar?q=croi"))
                .andExpect(redirectedUrl("/explorar?q=croi"));

        mvc.perform(get("/pedidos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Croissant")))
                .andExpect(content().string(containsString("S/ 2.50")))
                .andExpect(content().string(containsString("Inicia sesión para confirmar")));
    }

    @Test
    void nuncaRedirigeAOtroDominio() throws Exception {
        // del Referer solo se usa la ruta, el dominio externo se descarta
        mvc.perform(post("/carrito/agregar/" + idDe("Croissant")).session(sesion).with(csrf())
                        .header("Referer", "https://sitio-malo.com/explorar"))
                .andExpect(redirectedUrl("/explorar"));

        mvc.perform(post("/carrito/agregar/" + idDe("Croissant")).session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/pedidos"));
    }

    @Test
    void sumarYQuitarCantidades() throws Exception {
        Integer id = idDe("Pan camote");
        mvc.perform(post("/carrito/agregar/" + id).session(sesion).with(csrf()));
        mvc.perform(post("/carrito/agregar/" + id).session(sesion).with(csrf()));
        assertEquals(2, carrito().getCantidadTotal());

        mvc.perform(post("/carrito/quitar/" + id).session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/pedidos"));
        assertEquals(1, carrito().getCantidadTotal());

        mvc.perform(post("/carrito/quitar/" + id).session(sesion).with(csrf()));
        assertTrue(carrito().estaVacio());
    }

    @Test
    void marcarYDesmarcarFavorito() throws Exception {
        Integer id = idDe("Torta moka");

        mvc.perform(post("/favoritos/" + id).session(sesion).with(csrf()))
                .andExpect(redirectedUrl("/favoritos"));
        mvc.perform(get("/favoritos").session(sesion))
                .andExpect(content().string(containsString("Torta moka")))
                .andExpect(content().string(not(containsString("Sin favoritos todavía"))));

        mvc.perform(post("/favoritos/" + id).session(sesion).with(csrf()));
        mvc.perform(get("/favoritos").session(sesion))
                .andExpect(content().string(containsString("Sin favoritos todavía")));
    }

    @Test
    void confirmarSinIniciarSesionPideLogin() throws Exception {
        mvc.perform(get("/pedidos/confirmar"))
                .andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(post("/pedidos/confirmar").with(csrf()).param("tipoEntrega", "RECOJO_TIENDA"))
                .andExpect(redirectedUrl("http://localhost/login"));
        mvc.perform(get("/mis-pedidos"))
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void confirmarPedidoConRecojoEnTienda() throws Exception {
        Integer id = idDe("Empanada de carne");
        mvc.perform(post("/carrito/agregar/" + id).session(sesion).with(csrf()));
        mvc.perform(post("/carrito/agregar/" + id).session(sesion).with(csrf()));

        mvc.perform(post("/pedidos/confirmar").session(sesion).with(csrf())
                        .param("tipoEntrega", "RECOJO_TIENDA"))
                .andExpect(redirectedUrl("/mis-pedidos"))
                .andExpect(flash().attribute("mensaje", containsString("fue registrado")));

        assertTrue(carrito().estaVacio());

        mvc.perform(get("/mis-pedidos").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pendiente")))
                .andExpect(content().string(containsString("2 × Empanada de carne")))
                .andExpect(content().string(containsString("S/ 7.00")));
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void deliverySinDireccionNoSeRegistra() throws Exception {
        mvc.perform(post("/carrito/agregar/" + idDe("Croissant")).session(sesion).with(csrf()));

        mvc.perform(post("/pedidos/confirmar").session(sesion).with(csrf())
                        .param("tipoEntrega", "DELIVERY")
                        .param("direccion", "  "))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("error", containsString("dirección")));

        // el carrito se mantiene para que el cliente corrija
        assertEquals(1, carrito().getCantidadTotal());
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void direccionMuyLargaSeRechazaSinError500() throws Exception {
        mvc.perform(post("/carrito/agregar/" + idDe("Croissant")).session(sesion).with(csrf()));

        mvc.perform(post("/pedidos/confirmar").session(sesion).with(csrf())
                        .param("tipoEntrega", "DELIVERY")
                        .param("direccion", "x".repeat(250)))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("error", containsString("200 caracteres")));
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void modalidadInventadaSeRechazaSinError400() throws Exception {
        mvc.perform(post("/carrito/agregar/" + idDe("Croissant")).session(sesion).with(csrf()));

        mvc.perform(post("/pedidos/confirmar").session(sesion).with(csrf())
                        .param("tipoEntrega", "TELETRANSPORTE"))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("error", containsString("modalidad")));
    }

    @Test
    @WithUserDetails(value = CLIENTE, setupBefore = TestExecutionEvent.TEST_EXECUTION)
    void noSeConfirmaUnCarritoVacio() throws Exception {
        mvc.perform(post("/pedidos/confirmar").session(sesion).with(csrf())
                        .param("tipoEntrega", "RECOJO_TIENDA"))
                .andExpect(redirectedUrl("/pedidos"))
                .andExpect(flash().attribute("error", containsString("vacía")));
    }

    private Integer idDe(String nombre) {
        return productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals(nombre))
                .map(Producto::getId)
                .findFirst()
                .orElseThrow();
    }

    private Carrito carrito() {
        return (Carrito) sesion.getAttribute("scopedTarget.carrito");
    }
}
