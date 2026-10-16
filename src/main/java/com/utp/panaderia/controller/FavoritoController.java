package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.utp.panaderia.service.ProductoService;
import com.utp.panaderia.session.Carrito;
import com.utp.panaderia.util.Redireccion;

@Controller
public class FavoritoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private Carrito carrito;

    @GetMapping("/favoritos")
    public String favoritos(Model model) {
        model.addAttribute("productos", productoService.listarFavoritos(carrito.getFavoritos()));
        return "tienda/favoritos";
    }

    @PostMapping("/favoritos/{id}")
    public String marcarFavorito(@PathVariable Integer id,
                                 @RequestHeader(value = "Referer", required = false) String referer) {
        // valida que el producto exista antes de guardarlo
        productoService.buscarPorId(id);
        carrito.marcarFavorito(id);
        return Redireccion.volver(referer, "/favoritos");
    }
}
