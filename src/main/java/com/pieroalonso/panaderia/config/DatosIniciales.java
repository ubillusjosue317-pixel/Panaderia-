package com.pieroalonso.panaderia.config;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.pieroalonso.panaderia.model.Categoria;
import com.pieroalonso.panaderia.model.EstadoPedido;
import com.pieroalonso.panaderia.model.Producto;
import com.pieroalonso.panaderia.model.Rol;
import com.pieroalonso.panaderia.model.Usuario;
import com.pieroalonso.panaderia.repository.CategoriaRepository;
import com.pieroalonso.panaderia.repository.EstadoPedidoRepository;
import com.pieroalonso.panaderia.repository.ProductoRepository;
import com.pieroalonso.panaderia.repository.RolRepository;
import com.pieroalonso.panaderia.repository.UsuarioRepository;

/**
 * Carga los datos minimos para que el sistema funcione la primera vez:
 * roles, estados de pedido, categorias, el usuario administrador y algunos productos.
 * Si los datos ya existen no hace nada.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final RolRepository rolRepository;
    private final EstadoPedidoRepository estadoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public DatosIniciales(RolRepository rolRepository,
                          EstadoPedidoRepository estadoRepository,
                          CategoriaRepository categoriaRepository,
                          ProductoRepository productoRepository,
                          UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.estadoRepository = estadoRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearRoles();
        crearEstados();
        crearCategorias();
        crearAdministrador();
        crearProductosDeEjemplo();
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

    private Producto producto(String categoria, String nombre, String descripcion, String precio) {
        Producto p = new Producto();
        p.setCategoria(categoriaRepository.findByNombre(categoria).orElseThrow());
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }
}
