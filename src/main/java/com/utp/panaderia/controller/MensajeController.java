package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.MensajeForm;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.MensajeService;

import jakarta.validation.Valid;

// El cliente escribe consultas a la panaderia y ve las respuestas del administrador
@Controller
public class MensajeController {

    @Autowired
    private MensajeService mensajeService;

    @GetMapping("/mensajes")
    public String mensajes(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("nuevo", new MensajeForm());
        model.addAttribute("mensajes", mensajeService.listarPorUsuario(usuario.getId()));
        return "tienda/mensajes";
    }

    @PostMapping("/mensajes")
    public String enviar(@Valid @ModelAttribute("nuevo") MensajeForm form, BindingResult result,
                         @AuthenticationPrincipal UsuarioAutenticado usuario,
                         Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("mensajes", mensajeService.listarPorUsuario(usuario.getId()));
            return "tienda/mensajes";
        }
        mensajeService.enviar(usuario.getId(), form);
        redirect.addFlashAttribute("mensaje", "Tu mensaje fue enviado. Te responderemos pronto por aquí.");
        return "redirect:/mensajes";
    }
}
