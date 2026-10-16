package com.pieroalonso.panaderia.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.pieroalonso.panaderia.dto.ProductoForm;
import com.pieroalonso.panaderia.model.Producto;
import com.pieroalonso.panaderia.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaService categoriaService;

    public ProductoService(ProductoRepository productoRepository, CategoriaService categoriaService) {
        this.productoRepository = productoRepository;
        this.categoriaService = categoriaService;
    }

    public List<Producto> buscarDisponibles(Integer idCategoria, String texto) {
        String filtro = StringUtils.hasText(texto) ? texto.trim() : null;
        return productoRepository.buscarDisponibles(idCategoria, filtro);
    }

    public List<Producto> listarTodos() {
        return productoRepository.findAllByOrderByCategoriaNombreAscNombreAsc();
    }

    public Producto obtener(Integer id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Transactional
    public Producto guardar(ProductoForm form) {
        Producto producto = form.esNuevo() ? new Producto() : obtener(form.getId());

        producto.setCategoria(categoriaService.obtener(form.getIdCategoria()));
        producto.setNombre(form.getNombre().trim());
        producto.setDescripcion(StringUtils.hasText(form.getDescripcion()) ? form.getDescripcion().trim() : null);
        producto.setPrecio(form.getPrecio());
        producto.setImagenUrl(StringUtils.hasText(form.getImagenUrl()) ? form.getImagenUrl().trim() : null);
        producto.setDisponible(form.isDisponible());

        return productoRepository.save(producto);
    }

    /**
     * Los productos no se eliminan porque pueden estar en pedidos antiguos;
     * solo se ocultan del catalogo.
     */
    @Transactional
    public Producto cambiarDisponibilidad(Integer id) {
        Producto producto = obtener(id);
        producto.setDisponible(!producto.isDisponible());
        return producto;
    }
}
