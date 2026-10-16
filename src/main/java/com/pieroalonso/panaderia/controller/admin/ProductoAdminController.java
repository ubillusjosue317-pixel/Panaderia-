package com.pieroalonso.panaderia.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pieroalonso.panaderia.dto.ProductoForm;
import com.pieroalonso.panaderia.model.Producto;
import com.pieroalonso.panaderia.service.CategoriaService;
import com.pieroalonso.panaderia.service.ProductoService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/productos")
public class ProductoAdminController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoAdminController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "admin/productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return mostrarFormulario(new ProductoForm(), model);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        return mostrarFormulario(ProductoForm.desde(productoService.obtener(id)), model);
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("producto") ProductoForm form,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirect) {
        if (result.hasErrors()) {
            return mostrarFormulario(form, model);
        }
        Producto guardado = productoService.guardar(form);
        String accion = form.esNuevo() ? "registrado" : "actualizado";
        redirect.addFlashAttribute("mensaje", "Producto \"" + guardado.getNombre() + "\" " + accion + ".");
        return "redirect:/admin/productos";
    }

    @PostMapping("/{id}/disponibilidad")
    public String cambiarDisponibilidad(@PathVariable Integer id, RedirectAttributes redirect) {
        Producto producto = productoService.cambiarDisponibilidad(id);
        String estado = producto.isDisponible() ? "visible en el catálogo" : "oculto del catálogo";
        redirect.addFlashAttribute("mensaje", producto.getNombre() + " ahora está " + estado + ".");
        return "redirect:/admin/productos";
    }

    private String mostrarFormulario(ProductoForm form, Model model) {
        model.addAttribute("producto", form);
        model.addAttribute("categorias", categoriaService.listar());
        return "admin/productos/formulario";
    }
}
