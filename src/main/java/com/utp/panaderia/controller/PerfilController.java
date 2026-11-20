package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.PerfilForm;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.UsuarioService;

import jakarta.validation.Valid;

// Perfil del usuario. El usuario se toma SIEMPRE de la sesion, nunca de un id que mande el navegador:
// asi nadie puede ver ni editar el perfil de otra persona cambiando un numero en la URL (IDOR).
@Controller
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/perfil")
    public String ver(@AuthenticationPrincipal UsuarioAutenticado actual, Model model) {
        Usuario usuario = usuarioService.buscarPorId(actual.getId());
        PerfilForm form = new PerfilForm();
        form.setNombre(usuario.getNombre());
        form.setApellido(usuario.getApellido());
        form.setTelefono(usuario.getTelefono());
        model.addAttribute("perfil", form);
        model.addAttribute("correo", usuario.getEmail());
        return "tienda/perfil";
    }

    @PostMapping("/perfil")
    public String guardar(@AuthenticationPrincipal UsuarioAutenticado actual,
                          @Valid @ModelAttribute("perfil") PerfilForm form, BindingResult result,
                          Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("correo", usuarioService.buscarPorId(actual.getId()).getEmail());
            return "tienda/perfil";
        }
        usuarioService.actualizarPerfil(actual.getId(), form);
        redirect.addFlashAttribute("mensaje", "Tus datos fueron actualizados.");
        return "redirect:/perfil";
    }

    // Rutas con id (por ejemplo /perfil/2): solo valen si el id es el de la propia sesion
    @GetMapping("/perfil/{id}")
    public String verPorId(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        exigirPropio(id, actual);
        return "redirect:/perfil";
    }

    @PostMapping("/perfil/{id}")
    public String guardarPorId(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado actual,
                               @Valid @ModelAttribute("perfil") PerfilForm form, BindingResult result,
                               Model model, RedirectAttributes redirect) {
        exigirPropio(id, actual);
        return guardar(actual, form, result, model, redirect);
    }

    private void exigirPropio(Integer id, UsuarioAutenticado actual) {
        if (!actual.getId().equals(id)) {
            throw new AccessDeniedException("No puedes ver ni editar el perfil de otra persona");
        }
    }
}
