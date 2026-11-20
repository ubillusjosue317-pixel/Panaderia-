package com.utp.panaderia.service;

import java.util.List;

import com.utp.panaderia.dto.ResenaForm;
import com.utp.panaderia.model.Resena;

public interface ResenaService {

    List<Resena> listar(Integer idProducto);

    // promedio de estrellas, o null si todavia no hay reseñas
    Double promedio(Integer idProducto);

    // Crea la reseña del cliente para el producto, o reemplaza la que ya tenia
    Resena guardar(Integer idUsuario, Integer idProducto, ResenaForm form);

    // Solo la puede borrar su autor o un administrador
    void eliminar(Long idResena, Integer idUsuario, boolean esAdmin);
}
