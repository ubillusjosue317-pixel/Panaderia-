package com.utp.panaderia.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.service.CategoriaService;
import com.utp.panaderia.service.ProductoService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CatalogoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping("/")
    public String inicio(HttpServletRequest request, Model model) {
        // el administrador no compra, su inicio es el panel
        if (request.isUserInRole(Rol.ADMINISTRADOR)) {
            return "redirect:/admin/productos";
        }
        List<Producto> productos = productoService.listarDisponibles(null, null);
        // en el inicio solo se muestran los primeros 8
        if (productos.size() > 8) {
            productos = productos.subList(0, 8);
        }
        model.addAttribute("productos", productos);
        return "catalogo/inicio";
    }

    @GetMapping("/explorar")
    public String explorar(@RequestParam(required = false) Integer categoria,
                           @RequestParam(required = false) String q,
                           Model model) {
        model.addAttribute("categorias", categoriaService.listar());
        model.addAttribute("productos", productoService.listarDisponibles(categoria, q));
        model.addAttribute("categoriaActual", categoria);
        model.addAttribute("busqueda", q);
        return "catalogo/index";
    }

    // la ruta antigua del catalogo, se deja para no romper enlaces
    @GetMapping("/catalogo")
    public String catalogo(@RequestParam(required = false) Integer categoria,
                           @RequestParam(required = false) String q,
                           RedirectAttributes redirect) {
        if (categoria != null) {
            redirect.addAttribute("categoria", categoria);
        }
        if (q != null) {
            redirect.addAttribute("q", q);
        }
        return "redirect:/explorar";
    }
}
