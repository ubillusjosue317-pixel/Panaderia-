package com.utp.panaderia.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.utp.panaderia.dto.ProductoForm;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.CategoriaService;
import com.utp.panaderia.service.ProductoService;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private AuditoriaService auditoriaService;

    @Override
    public List<Producto> listarDisponibles(Integer idCategoria, String texto) {
        if (texto != null && texto.isBlank()) {
            texto = null;
        }
        return productoRepository.buscarDisponibles(idCategoria, texto == null ? null : texto.trim());
    }

    @Override
    public List<Producto> listarTodos() {
        return productoRepository.findAllByOrderByCategoriaNombreAscNombreAsc();
    }

    @Override
    public List<Producto> listarFavoritos(Collection<Integer> ids) {
        List<Producto> lista = new ArrayList<>();
        for (Producto p : productoRepository.findAllById(ids)) {
            if (p.isDisponible()) {
                lista.add(p);
            }
        }
        return lista;
    }

    @Override
    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Override
    @Transactional
    public Producto guardar(ProductoForm form) {
        Producto producto;
        String cambios;
        if (form.getId() == null) {
            producto = new Producto();
            cambios = "precio=" + form.getPrecio() + ", stock=" + form.getStock() + ", disponible=" + form.isDisponible();
        } else {
            producto = buscarPorId(form.getId());
            cambios = "precio " + producto.getPrecio() + " -> " + form.getPrecio()
                    + ", stock " + producto.getStock() + " -> " + form.getStock()
                    + ", disponible " + producto.isDisponible() + " -> " + form.isDisponible();
        }
        boolean esNuevo = form.getId() == null;

        producto.setCategoria(categoriaService.buscarPorId(form.getIdCategoria()));
        producto.setNombre(form.getNombre().trim());
        producto.setDescripcion(vacioANull(form.getDescripcion()));
        producto.setPrecio(form.getPrecio());
        producto.setImagenUrl(vacioANull(form.getImagenUrl()));
        producto.setStock(form.getStock());
        producto.setDisponible(form.isDisponible());

        Producto guardado = productoRepository.save(producto);
        auditoriaService.registrar(esNuevo ? "CREAR" : "EDITAR", "Producto", guardado.getId(),
                guardado.getNombre() + ": " + cambios);
        return guardado;
    }

    // No borramos productos porque pueden estar en pedidos anteriores,
    // solo se ocultan del catalogo
    @Override
    @Transactional
    public Producto cambiarDisponibilidad(Integer id) {
        Producto producto = buscarPorId(id);
        producto.setDisponible(!producto.isDisponible());
        Producto guardado = productoRepository.save(producto);
        auditoriaService.registrar("DISPONIBILIDAD", "Producto", id,
                guardado.getNombre() + ": disponible " + !guardado.isDisponible() + " -> " + guardado.isDisponible());
        return guardado;
    }

    // Cambio rapido desde la tabla de productos.
    // Los pedidos anteriores no cambian porque guardan el precio con el que se compro.
    @Override
    @Transactional
    public Producto actualizarPrecio(Integer id, BigDecimal precio) {
        if (precio == null || precio.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("El precio mínimo es S/ 0.01.");
        }
        if (precio.scale() > 2 || precio.compareTo(new BigDecimal("999999.99")) > 0) {
            throw new IllegalArgumentException("Formato de precio no válido.");
        }
        Producto producto = buscarPorId(id);
        BigDecimal anterior = producto.getPrecio();
        producto.setPrecio(precio);
        Producto guardado = productoRepository.save(producto);
        auditoriaService.registrar("PRECIO", "Producto", id,
                guardado.getNombre() + ": precio " + anterior + " -> " + precio);
        return guardado;
    }

    private String vacioANull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
