package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

import com.utp.panaderia.model.Rol;
import com.utp.panaderia.service.MensajeService;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.session.Carrito;

import jakarta.servlet.http.HttpServletRequest;

// Datos que necesitan todas las vistas (contador del menu y corazones de favoritos)
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private Carrito carrito;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private MensajeService mensajeService;

    @ModelAttribute("carrito")
    public Carrito carrito() {
        return carrito;
    }

    // numeros rojos del menu del administrador (pedidos nuevos y mensajes sin responder)
    @ModelAttribute
    public void contadoresAdmin(Authentication autenticacion, Model model) {
        long pedidos = 0;
        long mensajes = 0;
        if (esAdmin(autenticacion)) {
            pedidos = pedidoService.contarPendientes();
            mensajes = mensajeService.contarPendientes();
        }
        model.addAttribute("pedidosPendientes", pedidos);
        model.addAttribute("mensajesPendientes", mensajes);
    }

    // la foto supera el limite de spring.servlet.multipart.max-file-size
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String imagenMuyGrande(HttpServletRequest request) {
        RequestContextUtils.getOutputFlashMap(request).put("error", "La imagen no puede pesar más de 5 MB.");
        return "redirect:/admin/productos";
    }

    private boolean esAdmin(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Rol.ADMINISTRADOR));
    }
}
