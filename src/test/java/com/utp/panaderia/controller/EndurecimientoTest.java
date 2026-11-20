package com.utp.panaderia.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.ImagenService;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.UsuarioService;
import com.utp.panaderia.util.Totp;

@SpringBootTest
@AutoConfigureMockMvc
class EndurecimientoTest {

    private static final String CLIENTE = "endurecimiento@correo.com";

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
    private ImagenService imagenService;

    private Integer idCliente;

    @BeforeEach
    void preparar() {
        if (!usuarioService.existeEmail(CLIENTE)) {
            RegistroForm form = new RegistroForm();
            form.setNombre("Eva");
            form.setApellido("Rojas");
            form.setEmail(CLIENTE);
            form.setPassword("Clave#12345");
            form.setConfirmarPassword("Clave#12345");
            form.setAceptaTerminos(true);
            usuarioService.registrarCliente(form, Totp.generarSecreto());
        }
        idCliente = usuarioRepository.findByEmail(CLIENTE).orElseThrow().getId();
    }

    // la base de las pruebas se comparte entre clases: se deja el stock como estaba
    @AfterEach
    void restaurarStock() {
        for (String nombre : new String[] { "Empanada de carne", "Roll de canela" }) {
            fijarStock(producto(nombre).getId(), 50);
        }
    }

    private Producto producto(String nombre) {
        return productoRepository.findAll().stream().filter(p -> p.getNombre().equals(nombre)).findFirst().orElseThrow();
    }

    private void fijarStock(Integer id, int stock) {
        Producto p = productoRepository.findById(id).orElseThrow();
        p.setStock(stock);
        productoRepository.save(p);
    }

    // ---------- PS-19: condicion de carrera ----------

    @Test
    void conStockUnoSoloUnaDeLasComprasSimultaneasTieneExito() throws Exception {
        Integer id = producto("Empanada de carne").getId();
        fijarStock(id, 1);

        int hilos = 12;
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        AtomicInteger rechazos = new AtomicInteger();
        List<Future<?>> tareas = new ArrayList<>();
        for (int i = 0; i < hilos; i++) {
            tareas.add(pool.submit(() -> {
                try {
                    salida.await(); // todos arrancan al mismo tiempo
                    pedidoService.registrarPedido(idCliente, Map.of(id, 1), TipoEntrega.RECOJO_TIENDA, null, null);
                    exitos.incrementAndGet();
                } catch (IllegalArgumentException e) {
                    rechazos.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        salida.countDown();
        for (Future<?> t : tareas) {
            t.get();
        }
        pool.shutdown();

        assertEquals(1, exitos.get(), "solo una compra podia llevarse la ultima unidad");
        assertEquals(hilos - 1, rechazos.get());
        assertEquals(0, productoRepository.findById(id).orElseThrow().getStock());
    }

    @Test
    void sinStockSuficienteElPedidoNoSeRegistraYCancelarDevuelveElStock() {
        Integer id = producto("Roll de canela").getId();
        fijarStock(id, 3);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () ->
                pedidoService.registrarPedido(idCliente, Map.of(id, 4), TipoEntrega.RECOJO_TIENDA, null, null));
        assertTrue(e.getMessage().contains("No hay stock suficiente"));
        assertEquals(3, productoRepository.findById(id).orElseThrow().getStock());

        Pedido pedido = pedidoService.registrarPedido(idCliente, Map.of(id, 2), TipoEntrega.RECOJO_TIENDA, null, null);
        assertEquals(1, productoRepository.findById(id).orElseThrow().getStock());

        pedidoService.cancelarPropio(idCliente, pedido.getId());
        assertEquals(3, productoRepository.findById(id).orElseThrow().getStock());
    }

    // ---------- PS-15: tipo real del archivo ----------

    @Test
    void unaImagenFalsaQueDiceSerPngSeRechaza() {
        MockMultipartFile falsa = new MockMultipartFile("archivoImagen", "foto.png", "image/png",
                "<?php echo 'hola'; ?>".getBytes());
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> imagenService.guardarImagenProducto(falsa));
        assertTrue(e.getMessage().contains("no es una imagen"));
    }

    @Test
    void unPngVerdaderoSiSeAcepta() {
        byte[] png = { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };
        String ruta = imagenService.guardarImagenProducto(new MockMultipartFile("a", "foto.png", "image/png", png));
        assertTrue(ruta.startsWith("/uploads/productos/") && ruta.endsWith(".png"));
    }

    @Test
    void unJpgNoPuedeHacersepasarPorPng() {
        byte[] jpg = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0 };
        assertThrows(IllegalArgumentException.class, () ->
                imagenService.guardarImagenProducto(new MockMultipartFile("a", "foto.png", "image/png", jpg)));
    }

    // ---------- PS-17: cerrar sesion borra la cookie ----------

    @Test
    void cerrarSesionBorraLaCookieDeSesion() throws Exception {
        mvc.perform(post("/logout").with(csrf()))
                .andExpect(header().string("Set-Cookie", containsString("JSESSIONID=;")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }
}
