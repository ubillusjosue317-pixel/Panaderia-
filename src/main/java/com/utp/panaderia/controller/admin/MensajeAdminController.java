package com.utp.panaderia.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.service.MensajeService;

// Ventana Mensajeria: consultas de los clientes y sus respuestas
@Controller
@RequestMapping("/admin/mensajes")
public class MensajeAdminController {

    @Autowired
    private MensajeService mensajeService;

    @GetMapping
    public String listar(@RequestParam(defaultValue = "false") boolean pendientes, Model model) {
        model.addAttribute("mensajes", mensajeService.listarParaAdmin(pendientes));
        model.addAttribute("soloPendientes", pendientes);
        return "admin/mensajes/lista";
    }

    @PostMapping("/{id}/responder")
    public String responder(@PathVariable Integer id, @RequestParam(required = false) String respuesta,
                            RedirectAttributes redirect) {
        try {
            mensajeService.responder(id, respuesta);
            redirect.addFlashAttribute("mensaje", "Respuesta enviada. El cliente la verá en su sección Mensajes.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/mensajes";
    }
}
