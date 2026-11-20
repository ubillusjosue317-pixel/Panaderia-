package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.ResenaRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class FuncionesNuevasTest {

    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 1, 2, 3 };
    private static final byte[] PDF = "%PDF-1.4 contenido".getBytes();

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
    private ResenaRepository resenaRepository;

    private UsuarioAutenticado ana;
    private UsuarioAutenticado beto;

    @BeforeEach
    void preparar() {
        ana = cliente("ana.nuevas@correo.com", "Ana", "Paz");
        beto = cliente("beto.nuevas@correo.com", "Beto", "Luna");
    }

    private UsuarioAutenticado cliente(String email, String nombre, String apellido) {
        if (!usuarioService.existeEmail(email)) {
            RegistroForm form = new RegistroForm();
            form.setNombre(nombre);
            form.setApellido(apellido);
            form.setEmail(email);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        return new UsuarioAutenticado(usuarioRepository.findByEmail(email).orElseThrow(), false);
    }

    private Integer idProducto(String nombre) {
        return productoRepository.findAll().stream().filter(p -> p.getNombre().equals(nombre)).findFirst().orElseThrow().getId();
    }

    // ================= PS-12: perfil e IDOR =================

    @Test
    void cadaPersonaEditaSoloSuPerfil() throws Exception {
        mvc.perform(post("/perfil").with(user(ana)).with(csrf())
                        .param("nombre", "Ana María").param("apellido", "Paz").param("telefono", "987654321"))
                .andExpect(redirectedUrl("/perfil"));
        Usuario guardada = usuarioRepository.findById(ana.getId()).orElseThrow();
        assertEquals("Ana María", guardada.getNombre());
        assertEquals("987654321", guardada.getTelefono());
        // la otra persona no cambia
        assertEquals("Beto", usuarioRepository.findById(beto.getId()).orElseThrow().getNombre());
    }

    @Test
    void cambiarElIdEnLaUrlNoDaAccesoAOtroPerfil() throws Exception {
        mvc.perform(get("/perfil/" + beto.getId()).with(user(ana))).andExpect(status().isForbidden());
        mvc.perform(post("/perfil/" + beto.getId()).with(user(ana)).with(csrf())
                        .param("nombre", "Hackeado").param("apellido", "X"))
                .andExpect(status().isForbidden());
        assertEquals("Beto", usuarioRepository.findById(beto.getId()).orElseThrow().getNombre());

        // el propio id si funciona
        mvc.perform(get("/perfil/" + ana.getId()).with(user(ana))).andExpect(redirectedUrl("/perfil"));
    }

    @Test
    void noSePuedeCambiarElCorreoNiElRolMandandoCamposExtra() throws Exception {
        mvc.perform(post("/perfil").with(user(ana)).with(csrf())
                        .param("nombre", "Ana").param("apellido", "Paz")
                        .param("email", "otro@correo.com").param("rol", "ADMINISTRADOR").param("id", String.valueOf(beto.getId())))
                .andExpect(redirectedUrl("/perfil"));
        Usuario u = usuarioRepository.findById(ana.getId()).orElseThrow();
        assertEquals("ana.nuevas@correo.com", u.getEmail());
        assertEquals("CLIENTE", u.getRol().getNombre());
    }

    @Test
    void elPerfilPideIniciarSesionYValidaLosDatos() throws Exception {
        mvc.perform(get("/perfil")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/perfil").with(user(ana)).with(csrf())
                        .param("nombre", "").param("apellido", "Paz").param("telefono", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresa tu nombre")));
    }

    // ================= PS-14: reseñas y XSS =================

    @Test
    void unaResenaConScriptSeMuestraComoTextoYNoSeEjecuta() throws Exception {
        Integer id = idProducto("Pan camote");
        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                        .param("calificacion", "5").param("comentario", "<script>alert('XSS')</script>"))
                .andExpect(redirectedUrl("/producto/" + id));

        String html = mvc.perform(get("/producto/" + id)).andReturn().getResponse().getContentAsString();
        assertEquals(false, html.contains("<script>alert('XSS')</script>"), "el script no debe llegar sin escapar");
        assertEquals(true, html.contains("&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;"));
    }

    @Test
    void laResenaSeValidaYSoloHayUnaPorCliente() throws Exception {
        Integer id = idProducto("Croissant");
        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                        .param("calificacion", "9").param("comentario", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elige de 1 a 5 estrellas")));
        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                        .param("calificacion", "4").param("comentario", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Escribe tu comentario")));

        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                .param("calificacion", "4").param("comentario", "Muy rico"));
        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                .param("calificacion", "5").param("comentario", "Lo cambié a mejor"));
        assertEquals(1, resenaRepository.listarDelProducto(id).stream().filter(r -> r.getUsuario().getId().equals(ana.getId())).count());
        assertEquals(5, resenaRepository.findByProductoIdAndUsuarioId(id, ana.getId()).orElseThrow().getCalificacion());
    }

    @Test
    void soloElAutorOUnAdminBorraUnaResena() throws Exception {
        Integer id = idProducto("Torta moka");
        mvc.perform(post("/producto/" + id + "/resenas").with(user(ana)).with(csrf())
                .param("calificacion", "3").param("comentario", "Normal"));
        Long idResena = resenaRepository.findByProductoIdAndUsuarioId(id, ana.getId()).orElseThrow().getId();

        // otra persona no puede
        mvc.perform(post("/producto/" + id + "/resenas/" + idResena + "/eliminar").with(user(beto)).with(csrf()))
                .andExpect(status().isForbidden());
        assertNotNull(resenaRepository.findById(idResena).orElse(null));

        // el autor si
        mvc.perform(post("/producto/" + id + "/resenas/" + idResena + "/eliminar").with(user(ana)).with(csrf()))
                .andExpect(redirectedUrl("/producto/" + id));
        assertNull(resenaRepository.findById(idResena).orElse(null));
    }

    @Test
    void sinSesionNoSePuedeOpinarPeroSiLeer() throws Exception {
        Integer id = idProducto("Pan francés");
        mvc.perform(get("/producto/" + id)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Inicia sesión")));
        mvc.perform(post("/producto/" + id + "/resenas").with(csrf())
                        .param("calificacion", "5").param("comentario", "hola"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithUserDetails("admin@pieroalonso.pe")
    void elAdministradorNoEscribeResenasPeroPuedeModerar() throws Exception {
        Integer id = idProducto("Torta de chocolate");
        mvc.perform(post("/producto/" + id + "/resenas").with(csrf())
                        .param("calificacion", "5").param("comentario", "hola"))
                .andExpect(status().isForbidden());
    }

    // ================= PS-15: comprobantes =================

    private Pedido pedidoYape(UsuarioAutenticado dueno) {
        return pedidoService.registrarPedido(dueno.getId(), Map.of(idProducto("Croissant"), 1),
                TipoEntrega.DELIVERY, "Av. Siempre Viva 742", MetodoPago.YAPE_PLIN);
    }

    private MockMultipartFile archivo(String nombre, String tipo, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, tipo, contenido);
    }

    @Test
    void elClienteSubeUnComprobanteValidoYSoloElYElAdminLoVen() throws Exception {
        Pedido pedido = pedidoYape(ana);

        mvc.perform(multipart("/mis-pedidos/" + pedido.getId() + "/comprobante").file(archivo("pago.png", "image/png", PNG))
                        .with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("mensaje", containsString("Recibimos tu comprobante")));
        String guardado = pedidoRepository.findById(pedido.getId()).orElseThrow().getComprobante();
        assertNotNull(guardado);
        assertEquals(true, guardado.matches("^[0-9a-f-]{36}\\.png$"), "se guarda con nombre aleatorio");

        // el dueño lo ve, con el tipo real
        mvc.perform(get("/comprobantes/" + pedido.getId()).with(user(ana)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/png"));
        // otra persona no
        mvc.perform(get("/comprobantes/" + pedido.getId()).with(user(beto))).andExpect(status().isForbidden());
        // sin sesion tampoco
        mvc.perform(get("/comprobantes/" + pedido.getId())).andExpect(status().is3xxRedirection());
        // el administrador si
        Usuario admin = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow();
        mvc.perform(get("/comprobantes/" + pedido.getId()).with(user(new UsuarioAutenticado(admin, false))))
                .andExpect(status().isOk());
        // y el archivo no esta en una ruta publica
        mvc.perform(get("/uploads/comprobantes/" + guardado)).andExpect(status().isNotFound());
        mvc.perform(get("/uploads/" + guardado)).andExpect(status().isNotFound());
    }

    @Test
    void sePuedeSubirUnPdf() throws Exception {
        Pedido pedido = pedidoYape(ana);
        mvc.perform(multipart("/mis-pedidos/" + pedido.getId() + "/comprobante").file(archivo("pago.pdf", "application/pdf", PDF))
                        .with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("mensaje", containsString("Recibimos")));
    }

    @Test
    void losArchivosEjecutablesOFalsosSeRechazan() throws Exception {
        Pedido pedido = pedidoYape(ana);
        String url = "/mis-pedidos/" + pedido.getId() + "/comprobante";
        byte[] php = "<?php system($_GET['c']); ?>".getBytes();

        // extension peligrosa
        for (String nombre : new String[] { "shell.php", "cmd.jsp", "virus.exe", "script.js", "x.html", "sin_extension" }) {
            mvc.perform(multipart(url).file(archivo(nombre, "image/png", PNG)).with(user(ana)).with(csrf()))
                    .andExpect(flash().attribute("error", containsString("Solo se aceptan")));
        }
        // doble extension peligrosa al final
        mvc.perform(multipart(url).file(archivo("foto.png.php", "image/png", PNG)).with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("Solo se aceptan")));
        // un script disfrazado de .png (extension y tipo falsos, contenido de php)
        mvc.perform(multipart(url).file(archivo("foto.png", "image/png", php)).with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("no es realmente")));
        // un PDF renombrado como .png
        mvc.perform(multipart(url).file(archivo("pago.png", "image/png", PDF)).with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("no es realmente")));
        // vacio
        mvc.perform(multipart(url).file(archivo("pago.png", "image/png", new byte[0])).with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("Elige el archivo")));

        assertNull(pedidoRepository.findById(pedido.getId()).orElseThrow().getComprobante());
    }

    @Test
    void nadieSubeComprobanteDelPedidoDeOtroNiDeUnPagoQueNoEsYape() throws Exception {
        Pedido deAna = pedidoYape(ana);
        mvc.perform(multipart("/mis-pedidos/" + deAna.getId() + "/comprobante").file(archivo("p.png", "image/png", PNG))
                        .with(user(beto)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("No encontramos")));

        Pedido efectivo = pedidoService.registrarPedido(ana.getId(), Map.of(idProducto("Croissant"), 1),
                TipoEntrega.DELIVERY, "Calle 1", MetodoPago.EFECTIVO);
        mvc.perform(multipart("/mis-pedidos/" + efectivo.getId() + "/comprobante").file(archivo("p.png", "image/png", PNG))
                        .with(user(ana)).with(csrf()))
                .andExpect(flash().attribute("error", containsString("Yape")));
    }

    @Test
    void laPaginaDeMisPedidosOfreceSubirElComprobanteSoloEnYape() throws Exception {
        pedidoYape(ana);
        mvc.perform(get("/mis-pedidos").with(user(ana)))
                .andExpect(content().string(containsString("Subir comprobante")));
        mvc.perform(get("/mis-pedidos").with(user(beto)))
                .andExpect(content().string(not(containsString("Subir comprobante"))));
    }

    // ================= Perfil como seccion de la barra inferior =================

    @Test
    void cerrarSesionSoloEstaEnLaSeccionPerfilYLaBarraTienePerfil() throws Exception {
        // el boton flotante ya no aparece al final de las otras paginas
        mvc.perform(get("/mis-pedidos").with(user(ana)))
                .andExpect(content().string(not(containsString("salir-movil"))));
        mvc.perform(get("/mensajes").with(user(ana)))
                .andExpect(content().string(not(containsString("salir-movil"))));
        // en el perfil si, junto a los accesos de la cuenta
        mvc.perform(get("/perfil").with(user(ana)))
                .andExpect(content().string(containsString("salir-movil")))
                .andExpect(content().string(containsString("/mis-pedidos")))
                .andExpect(content().string(containsString("/mensajes")))
                .andExpect(content().string(containsString("/cambiar-clave")));
        // la barra de abajo trae la pestaña Perfil
        mvc.perform(get("/mis-pedidos").with(user(ana)))
                .andExpect(content().string(containsString("href=\"/perfil\"")));
    }

    @Test
    void elAdministradorTambienTienePerfilYSinBotonFlotante() throws Exception {
        Usuario admin = usuarioRepository.findByEmail("admin@pieroalonso.pe").orElseThrow();
        UsuarioAutenticado adm = new UsuarioAutenticado(admin, false);
        mvc.perform(get("/admin/productos").with(user(adm)))
                .andExpect(content().string(not(containsString("salir-movil"))))
                .andExpect(content().string(containsString("href=\"/perfil\"")));
        mvc.perform(get("/perfil").with(user(adm)))
                .andExpect(content().string(containsString("salir-movil")))
                .andExpect(content().string(containsString("/cambiar-clave")))
                // el administrador no compra: no ve Mis pedidos ni Mensajes de cliente
                .andExpect(content().string(not(containsString("href=\"/mis-pedidos\""))));
    }
}
