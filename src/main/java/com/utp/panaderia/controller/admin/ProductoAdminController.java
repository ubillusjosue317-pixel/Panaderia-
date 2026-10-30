package com.utp.panaderia.controller.admin;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.ProductoForm;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.service.CategoriaService;
import com.utp.panaderia.service.ImagenService;
import com.utp.panaderia.service.ProductoService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/productos")
public class ProductoAdminController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ImagenService imagenService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("productos", productoService.listarTodos());
        return "admin/productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("producto", new ProductoForm());
        model.addAttribute("categorias", categoriaService.listar());
        return "admin/productos/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Integer id, Model model) {
        Producto p = productoService.buscarPorId(id);

        ProductoForm form = new ProductoForm();
        form.setId(p.getId());
        form.setIdCategoria(p.getCategoria().getId());
        form.setNombre(p.getNombre());
        form.setDescripcion(p.getDescripcion());
        form.setPrecio(p.getPrecio());
        form.setImagenUrl(p.getImagenUrl());
        form.setDisponible(p.isDisponible());

        model.addAttribute("producto", form);
        model.addAttribute("categorias", categoriaService.listar());
        return "admin/productos/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("producto") ProductoForm form, BindingResult result,
                          @RequestParam(required = false) MultipartFile archivoImagen,
                          Model model, RedirectAttributes redirect) {
        // si eligio una foto desde su PC, reemplaza a la imagen anterior
        if (!result.hasErrors() && archivoImagen != null && !archivoImagen.isEmpty()) {
            try {
                form.setImagenUrl(imagenService.guardarImagenProducto(archivoImagen));
            } catch (IllegalArgumentException e) {
                result.rejectValue("imagenUrl", "imagen", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("categorias", categoriaService.listar());
            return "admin/productos/formulario";
        }

        boolean esNuevo = form.getId() == null;
        Producto guardado = productoService.guardar(form);

        if (esNuevo) {
            redirect.addFlashAttribute("mensaje", "Producto \"" + guardado.getNombre() + "\" registrado.");
        } else {
            redirect.addFlashAttribute("mensaje", "Producto \"" + guardado.getNombre() + "\" actualizado.");
        }
        return "redirect:/admin/productos";
    }

    @PostMapping("/{id}/disponibilidad")
    public String cambiarDisponibilidad(@PathVariable Integer id, RedirectAttributes redirect) {
        Producto producto = productoService.cambiarDisponibilidad(id);
        if (producto.isDisponible()) {
            redirect.addFlashAttribute("mensaje", producto.getNombre() + " ahora está visible en el catálogo.");
        } else {
            redirect.addFlashAttribute("mensaje", producto.getNombre() + " ahora está oculto del catálogo.");
        }
        return "redirect:/admin/productos";
    }

    @PostMapping("/{id}/precio")
    public String cambiarPrecio(@PathVariable Integer id, @RequestParam(required = false) BigDecimal precio,
                                RedirectAttributes redirect) {
        try {
            Producto producto = productoService.actualizarPrecio(id, precio);
            redirect.addFlashAttribute("mensaje", "Nuevo precio de " + producto.getNombre() + ": S/ "
                    + producto.getPrecio().setScale(2));
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/productos";
    }
}
