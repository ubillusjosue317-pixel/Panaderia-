package com.utp.panaderia.controller;

import static com.utp.panaderia.security.VerificacionDosPasosFilter.CAMBIO_CLAVE;
import static com.utp.panaderia.security.VerificacionDosPasosFilter.DESTINO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.CambioClaveForm;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.UsuarioService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

// Cambio de contraseña. Es obligatorio en el primer ingreso del administrador
// (el filtro de seguridad no deja abrir otra pagina hasta que se complete).
@Controller
public class CambioClaveController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/cambiar-clave")
    public String formulario(Model model, HttpSession sesion) {
        model.addAttribute("cambio", new CambioClaveForm());
        model.addAttribute("obligatorio", sesion.getAttribute(CAMBIO_CLAVE) != null);
        return "auth/cambiar-clave";
    }

    @PostMapping("/cambiar-clave")
    public String cambiar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                          @Valid @ModelAttribute("cambio") CambioClaveForm form, BindingResult result,
                          Model model, HttpSession sesion, RedirectAttributes redirect) {
        model.addAttribute("obligatorio", sesion.getAttribute(CAMBIO_CLAVE) != null);

        if (form.getNueva() != null && !form.getNueva().equals(form.getConfirmar())) {
            result.rejectValue("confirmar", "noCoincide", "Las contraseñas no coinciden");
        }
        if (!result.hasErrors()) {
            try {
                usuarioService.cambiarPassword(usuario.getId(), form.getActual(), form.getNueva());
            } catch (IllegalArgumentException e) {
                result.rejectValue("actual", "invalida", e.getMessage());
            }
        }
        if (result.hasErrors()) {
            form.setActual(null);
            form.setNueva(null);
            form.setConfirmar(null);
            return "auth/cambiar-clave";
        }

        String destino = (String) sesion.getAttribute(DESTINO);
        sesion.removeAttribute(CAMBIO_CLAVE);
        sesion.removeAttribute(DESTINO);
        redirect.addFlashAttribute("mensaje", "Tu contraseña fue actualizada.");
        return "redirect:" + (destino != null ? destino : "/");
    }
}
