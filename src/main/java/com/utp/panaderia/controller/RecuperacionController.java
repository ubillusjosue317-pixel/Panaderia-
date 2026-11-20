package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

import com.utp.panaderia.dto.RestablecerForm;
import com.utp.panaderia.service.RecuperacionService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

// Olvide mi contraseña: pedir el enlace -> abrirlo desde el correo -> elegir la contraseña nueva.
@Controller
@RequestMapping("/recuperar")
public class RecuperacionController {

    private static final String TOKEN_EN_SESION = "recuperacion.token";

    @Autowired
    private RecuperacionService recuperacionService;

    @GetMapping
    public String formulario() {
        return "auth/recuperar";
    }

    @PostMapping
    public String solicitar(@RequestParam(defaultValue = "") String email, RedirectAttributes redirect) {
        recuperacionService.solicitar(email);
        // el mismo mensaje exista o no el correo: no se puede averiguar que cuentas existen
        redirect.addFlashAttribute("mensaje",
                "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña. "
                        + "Vence en 30 minutos y solo se puede usar una vez.");
        return "redirect:/login";
    }

    // El enlace del correo trae el token en la direccion. Se pasa a la sesion y se redirige a una
    // direccion LIMPIA: asi el token no queda en la barra, en el historial, ni se filtra por "Referer".
    @GetMapping(value = "/restablecer", params = "token")
    public String abrirEnlace(@RequestParam String token, HttpSession sesion, HttpServletResponse respuesta,
                              RedirectAttributes redirect) {
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        if (!recuperacionService.tokenValido(token)) {
            redirect.addFlashAttribute("error", "El enlace no es válido o ya venció. Solicita uno nuevo.");
            return "redirect:/recuperar";
        }
        sesion.setAttribute(TOKEN_EN_SESION, token);
        return "redirect:/recuperar/restablecer";
    }

    @GetMapping(value = "/restablecer", params = "!token")
    public String formularioNuevaClave(HttpSession sesion, HttpServletResponse respuesta, Model model,
                                       RedirectAttributes redirect) {
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        String token = (String) sesion.getAttribute(TOKEN_EN_SESION);
        if (token == null || !recuperacionService.tokenValido(token)) {
            sesion.removeAttribute(TOKEN_EN_SESION);
            redirect.addFlashAttribute("error", "El enlace no es válido o ya venció. Solicita uno nuevo.");
            return "redirect:/recuperar";
        }
        model.addAttribute("restablecer", new RestablecerForm());
        return "auth/restablecer";
    }

    @PostMapping("/restablecer")
    public String guardar(@Valid @ModelAttribute("restablecer") RestablecerForm form, BindingResult result,
                          HttpSession sesion, HttpServletResponse respuesta, RedirectAttributes redirect) {
        respuesta.setHeader("Referrer-Policy", "no-referrer");
        String token = (String) sesion.getAttribute(TOKEN_EN_SESION);
        if (form.getNueva() != null && !form.getNueva().equals(form.getConfirmar())) {
            result.rejectValue("confirmar", "noCoincide", "Las contraseñas no coinciden");
        }
        if (result.hasErrors()) {
            form.setNueva(null);
            form.setConfirmar(null);
            return "auth/restablecer";
        }
        try {
            recuperacionService.restablecer(token, form.getNueva());
        } catch (IllegalArgumentException e) {
            sesion.removeAttribute(TOKEN_EN_SESION);
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/recuperar";
        }
        sesion.removeAttribute(TOKEN_EN_SESION);
        redirect.addFlashAttribute("mensaje", "Listo: tu contraseña fue cambiada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }
}
