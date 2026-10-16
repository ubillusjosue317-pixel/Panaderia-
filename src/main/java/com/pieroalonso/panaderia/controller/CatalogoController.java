package com.pieroalonso.panaderia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.pieroalonso.panaderia.service.CategoriaService;
import com.pieroalonso.panaderia.service.ProductoService;

@Controller
public class CatalogoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public CatalogoController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping({"/", "/catalogo"})
    public String catalogo(@RequestParam(required = false) Integer categoria,
                           @RequestParam(required = false) String q,
                           Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("productos", productoService.buscarDisponibles(categoria, q));
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("busqueda", q);
        return "catalogo/index";
    }
}
