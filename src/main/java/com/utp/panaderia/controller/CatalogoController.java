package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.utp.panaderia.service.CategoriaService;
import com.utp.panaderia.service.ProductoService;

@Controller
public class CatalogoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping({"/", "/catalogo"})
    public String catalogo(@RequestParam(required = false) Integer categoria,
                           @RequestParam(required = false) String q,
                           Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("productos", productoService.listarDisponibles(categoria, q));
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("busqueda", q);
        return "catalogo/index";
    }

    // TODO: agregar al carrito (RF05) - siguiente avance
}
