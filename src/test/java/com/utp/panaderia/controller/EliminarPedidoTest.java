package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.repository.AuditoriaRepository;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class EliminarPedidoTest {

    private static final String CLIENTE = "elimina.pedido@correo.com";

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
    private AuditoriaRepository auditoriaRepository;

    private UsuarioAutenticado cliente;

    @BeforeEach
    void preparar() {
        if (!usuarioService.existeEmail(CLIENTE)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Elena");
            form.setApellido("Ruiz");
            form.setEmail(CLIENTE);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        cliente = new UsuarioAutenticado(usuarioRepository.findByEmail(CLIENTE).orElseThrow(), false);
    }

    private Pedido nuevoPedido() {
        Integer id = productoRepository.findAll().stream().filter(p -> p.getNombre().equals("Croissant"))
                .findFirst().orElseThrow().getId();
        return pedidoService.registrarPedido(cliente.getId(), Map.of(id, 1), TipoEntrega.RECOJO_TIENDA, null, null);
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void elAdminEliminaUnPedidoCanceladoYQuedaRegistrado() throws Exception {
        Pedido pedido = nuevoPedido();
        pedidoService.cancelarPropio(cliente.getId(), pedido.getId());
        long auditoriasAntes = auditoriaRepository.count();

        // el boton Eliminar aparece en el pedido cancelado
        mvc.perform(get("/admin/pedidos"))
                .andExpect(content().string(containsString("/admin/pedidos/" + pedido.getId() + "/eliminar")));

        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/eliminar").with(csrf()))
                .andExpect(redirectedUrl("/admin/pedidos"))
                .andExpect(flash().attribute("mensaje", containsString("fue eliminado")));

        assertFalse(pedidoRepository.findById(pedido.getId()).isPresent());
        assertEquals(auditoriasAntes + 1, auditoriaRepository.count());
        var ultima = auditoriaRepository.findAll().stream().reduce((a, b) -> b).orElseThrow();
        assertEquals("ELIMINAR", ultima.getAccion());
        assertEquals("admin@pieroalonso.pe", ultima.getUsuario());
        assertTrue(ultima.getDetalle().contains(CLIENTE));
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void unPedidoQueNoEstaCanceladoNoSeEliminaNiTieneBoton() throws Exception {
        Pedido pedido = nuevoPedido();

        mvc.perform(get("/admin/pedidos"))
                .andExpect(content().string(not(containsString("/admin/pedidos/" + pedido.getId() + "/eliminar"))));

        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/eliminar").with(csrf()))
                .andExpect(flash().attribute("error", containsString("Solo se pueden eliminar los pedidos cancelados")));
        assertTrue(pedidoRepository.findById(pedido.getId()).isPresent());
    }

    @Test
    void unClienteNoPuedeEliminarPedidos() throws Exception {
        Pedido pedido = nuevoPedido();
        pedidoService.cancelarPropio(cliente.getId(), pedido.getId());

        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/eliminar").with(user(cliente)).with(csrf()))
                .andExpect(status().isForbidden());
        // sin sesion tampoco
        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/eliminar").with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertTrue(pedidoRepository.findById(pedido.getId()).isPresent());
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void alEliminarUnPedidoSeBorraTambienSuComprobante() throws Exception {
        Integer id = productoRepository.findAll().stream().filter(p -> p.getNombre().equals("Croissant"))
                .findFirst().orElseThrow().getId();
        Pedido pedido = pedidoService.registrarPedido(cliente.getId(), Map.of(id, 1), TipoEntrega.DELIVERY,
                "Calle Falsa 123", MetodoPago.YAPE_PLIN);
        byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };
        mvc.perform(multipart("/mis-pedidos/" + pedido.getId() + "/comprobante")
                .file(new MockMultipartFile("archivo", "pago.png", "image/png", png)).with(user(cliente)).with(csrf()));
        String guardado = pedidoRepository.findById(pedido.getId()).orElseThrow().getComprobante();
        java.nio.file.Path archivo = java.nio.file.Paths.get("target/test-comprobantes").toAbsolutePath().resolve(guardado);
        assertTrue(java.nio.file.Files.exists(archivo));

        pedidoService.cancelarPropio(cliente.getId(), pedido.getId());
        mvc.perform(post("/admin/pedidos/" + pedido.getId() + "/eliminar").with(csrf()));

        assertFalse(pedidoRepository.findById(pedido.getId()).isPresent());
        assertFalse(java.nio.file.Files.exists(archivo), "el comprobante tambien se borra del disco");
    }
}
