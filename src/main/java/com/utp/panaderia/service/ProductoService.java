package com.utp.panaderia.service;

import java.util.List;

import com.utp.panaderia.dto.ProductoForm;
import com.utp.panaderia.model.Producto;

public interface ProductoService {

    List<Producto> listarDisponibles(Integer idCategoria, String texto);

    List<Producto> listarTodos();

    Producto buscarPorId(Integer id);

    Producto guardar(ProductoForm form);

    Producto cambiarDisponibilidad(Integer id);
}
