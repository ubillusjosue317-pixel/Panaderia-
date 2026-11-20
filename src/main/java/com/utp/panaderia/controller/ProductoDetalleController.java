package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.ResenaForm;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.ProductoService;
import com.utp.panaderia.service.ResenaService;

import jakarta.validation.Valid;

// Pagina de un producto con sus reseñas. Cualquiera puede leerlas; solo los clientes con sesion escriben.
@Controller
public class ProductoDetalleController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ResenaService resenaService;

    @GetMapping("/producto/{id}")
    public String ver(@PathVariable Integer id, Model model) {
        Producto producto = productoService.buscarPorId(id);
        if (!producto.isDisponible()) {
            // los productos ocultos del catalogo tampoco se muestran por su direccion directa
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        cargar(model, producto);
        if (!model.containsAttribute("resena")) {
            model.addAttribute("resena", new ResenaForm());
        }
        return "tienda/producto";
    }

    @PostMapping("/producto/{id}/resenas")
    public String publicar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                           @Valid @ModelAttribute("resena") ResenaForm form, BindingResult result,
                           Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            cargar(model, productoService.buscarPorId(id));
            return "tienda/producto";
        }
        resenaService.guardar(usuario.getId(), id, form);
        redirect.addFlashAttribute("mensaje", "¡Gracias! Tu reseña fue publicada.");
        return "redirect:/producto/" + id;
    }

    @PostMapping("/producto/{id}/resenas/{idResena}/eliminar")
    public String eliminar(@PathVariable Integer id, @PathVariable Long idResena,
                           @AuthenticationPrincipal UsuarioAutenticado usuario, RedirectAttributes redirect) {
        boolean esAdmin = usuario.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR));
        try {
            resenaService.eliminar(idResena, usuario.getId(), esAdmin);
            redirect.addFlashAttribute("mensaje", "La reseña fue eliminada.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/producto/" + id;
    }

    private void cargar(Model model, Producto producto) {
        model.addAttribute("producto", producto);
        model.addAttribute("resenas", resenaService.listar(producto.getId()));
        model.addAttribute("promedio", resenaService.promedio(producto.getId()));
    }
}
