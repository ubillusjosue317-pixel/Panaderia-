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

import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.service.PedidoService;

// Ventana Pedidos del administrador: ve los pedidos de todos los clientes y les cambia el estado
@Controller
@RequestMapping("/admin/pedidos")
public class PedidoAdminController {

    @Autowired
    private PedidoService pedidoService;

    @GetMapping
    public String listar(@RequestParam(required = false) Integer estado, Model model) {
        model.addAttribute("pedidos", pedidoService.listarParaAdmin(estado));
        model.addAttribute("estados", pedidoService.listarEstados());
        model.addAttribute("cantidades", pedidoService.contarPorEstado());
        model.addAttribute("estadoActual", estado);
        return "admin/pedidos/lista";
    }

    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Integer id, @RequestParam Integer idEstado,
                                @RequestParam(required = false) Integer filtro,
                                RedirectAttributes redirect) {
        try {
            Pedido pedido = pedidoService.cambiarEstado(id, idEstado);
            redirect.addFlashAttribute("mensaje",
                    "Pedido #" + pedido.getId() + " ahora está \"" + pedido.getEstado().getNombre() + "\".");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        // vuelve con el mismo filtro que tenia la lista
        if (filtro != null) {
            redirect.addAttribute("estado", filtro);
        }
        return "redirect:/admin/pedidos";
    }
}
