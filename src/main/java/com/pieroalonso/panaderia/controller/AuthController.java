package com.pieroalonso.panaderia.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.pieroalonso.panaderia.dto.RegistroForm;
import com.pieroalonso.panaderia.service.UsuarioService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("registro", new RegistroForm());
        return "auth/registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroForm form,
                            BindingResult result,
                            RedirectAttributes redirect) {
        if (!form.passwordsCoinciden()) {
            result.rejectValue("confirmarPassword", "noCoincide", "Las contraseñas no coinciden");
        }
        if (!result.hasFieldErrors("email") && usuarioService.emailRegistrado(form.getEmail())) {
            result.rejectValue("email", "duplicado", "Ya existe una cuenta con este correo");
        }
        if (result.hasErrors()) {
            return "auth/registro";
        }

        usuarioService.registrarCliente(form);
        redirect.addFlashAttribute("mensaje", "Tu cuenta fue creada. Ya puedes iniciar sesión.");
        return "redirect:/login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "error/acceso-denegado";
    }
}
