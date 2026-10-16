package com.utp.panaderia.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.utp.panaderia.dto.ProductoForm;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.service.CategoriaService;
import com.utp.panaderia.service.ProductoService;

@Service
public class ProductoServiceImpl implements ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private CategoriaService categoriaService;

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
    public Producto buscarPorId(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Override
    @Transactional
    public Producto guardar(ProductoForm form) {
        Producto producto;
        if (form.getId() == null) {
            producto = new Producto();
        } else {
            producto = buscarPorId(form.getId());
        }

        producto.setCategoria(categoriaService.buscarPorId(form.getIdCategoria()));
        producto.setNombre(form.getNombre().trim());
        producto.setDescripcion(vacioANull(form.getDescripcion()));
        producto.setPrecio(form.getPrecio());
        producto.setImagenUrl(vacioANull(form.getImagenUrl()));
        producto.setDisponible(form.isDisponible());

        return productoRepository.save(producto);
    }

    // No borramos productos porque pueden estar en pedidos anteriores,
    // solo se ocultan del catalogo
    @Override
    @Transactional
    public Producto cambiarDisponibilidad(Integer id) {
        Producto producto = buscarPorId(id);
        producto.setDisponible(!producto.isDisponible());
        return productoRepository.save(producto);
    }

    private String vacioANull(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
