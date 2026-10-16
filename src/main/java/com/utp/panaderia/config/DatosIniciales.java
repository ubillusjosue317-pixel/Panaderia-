package com.utp.panaderia.config;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.model.Categoria;
import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.CategoriaRepository;
import com.utp.panaderia.repository.EstadoPedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.RolRepository;
import com.utp.panaderia.repository.UsuarioRepository;

// Se ejecuta al iniciar la app. Llena los roles, estados, categorias, el admin
// y unos productos de prueba (solo si la tabla esta vacia)
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private EstadoPedidoRepository estadoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        crearRoles();
        crearEstados();
        crearCategorias();
        crearAdministrador();
        crearProductosDeEjemplo();
        agregarProductosNuevos();
        asignarImagenesFaltantes();
    }

    private void crearRoles() {
        for (String nombre : List.of(Rol.ADMINISTRADOR, Rol.CLIENTE)) {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre));
            }
        }
    }

    private void crearEstados() {
        List<String> estados = List.of(EstadoPedido.PENDIENTE, EstadoPedido.EN_PREPARACION,
                EstadoPedido.LISTO, EstadoPedido.ENTREGADO, EstadoPedido.CANCELADO);
        for (String nombre : estados) {
            if (estadoRepository.findByNombre(nombre).isEmpty()) {
                estadoRepository.save(new EstadoPedido(nombre));
            }
        }
    }

    private void crearCategorias() {
        if (categoriaRepository.count() > 0) {
            return;
        }
        categoriaRepository.saveAll(List.of(
                new Categoria("Panadería", "Panes del día: francés, camote, yema, integral"),
                new Categoria("Pastelería", "Tortas enteras y por porción"),
                new Categoria("Bollería", "Croissants, cachitos y masas hojaldradas"),
                new Categoria("Salados", "Empanadas, enrollados y bocaditos")));
    }

    private void crearAdministrador() {
        if (usuarioRepository.existsByEmail(adminEmail)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setRol(rolRepository.findByNombre(Rol.ADMINISTRADOR).orElseThrow());
        admin.setNombre("Administrador");
        admin.setApellido("Piero Alonso");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        usuarioRepository.save(admin);
        log.info("Usuario administrador creado: {}", adminEmail);
    }

    private void crearProductosDeEjemplo() {
        if (productoRepository.count() > 0) {
            return;
        }
        productoRepository.saveAll(List.of(
                producto("Panadería", "Pan camote", "Paquete de 5 unidades, recién horneado", "1.00"),
                producto("Panadería", "Pan francés", "Paquete de 10 unidades", "2.00"),
                producto("Pastelería", "Torta moka", "Porción de torta de café con crema", "4.00"),
                producto("Pastelería", "Torta de chocolate", "Porción con fudge", "4.50"),
                producto("Bollería", "Croissant", "Croissant de mantequilla", "2.50"),
                producto("Salados", "Empanada de carne", "Empanada horneada con relleno de carne", "3.50")));
    }

    // Productos que se agregaron despues. Solo se crean si no existen,
    // asi no se toca lo que ya esta registrado en la BD.
    private void agregarProductosNuevos() {
        if (!productoRepository.existsByNombreIgnoreCase("Baguette clásica")
                && categoriaRepository.findByNombre("Panadería").isPresent()) {
            productoRepository.save(producto("Panadería", "Baguette clásica", "Corteza crocante, ideal para el desayuno", "2.80"));
        }
        if (!productoRepository.existsByNombreIgnoreCase("Roll de canela")
                && categoriaRepository.findByNombre("Bollería").isPresent()) {
            productoRepository.save(producto("Bollería", "Roll de canela", "Masa suave con canela y glaseado", "3.20"));
        }
    }

    // Pone una foto de referencia solo a los productos que no tienen imagen
    private void asignarImagenesFaltantes() {
        Map<String, String> fotos = new HashMap<>();
        fotos.put("pan camote", "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=900&q=85");
        fotos.put("pan francés", "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?w=900&q=85");
        fotos.put("baguette clásica", "https://images.unsplash.com/photo-1549931319-a545dcf3bc73?w=900&q=85");
        fotos.put("torta moka", "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=900&q=85");
        fotos.put("torta de chocolate", "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=900&q=85");
        fotos.put("croissant", "https://images.unsplash.com/photo-1555507036-ab1f4038808a?w=900&q=85");
        fotos.put("empanada de carne", "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=900&q=85");
        fotos.put("roll de canela", "https://images.unsplash.com/photo-1509365465985-25d11c17e812?w=900&q=85");

        for (Producto p : productoRepository.findAll()) {
            String foto = fotos.get(p.getNombre().toLowerCase());
            if (foto != null && (p.getImagenUrl() == null || p.getImagenUrl().isBlank())) {
                p.setImagenUrl(foto);
                productoRepository.save(p);
            }
        }
    }

    private Producto producto(String categoria, String nombre, String descripcion, String precio) {
        Producto p = new Producto();
        p.setCategoria(categoriaRepository.findByNombre(categoria).orElseThrow());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }
}
