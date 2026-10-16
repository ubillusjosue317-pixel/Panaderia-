package com.utp.panaderia.service;

import java.util.List;

import com.utp.panaderia.model.Categoria;

public interface CategoriaService {

    List<Categoria> listar();

    Categoria buscarPorId(Integer id);
}
