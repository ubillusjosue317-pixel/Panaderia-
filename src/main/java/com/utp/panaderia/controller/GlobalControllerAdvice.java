package com.utp.panaderia.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.utp.panaderia.session.Carrito;

// Datos que necesitan todas las vistas (contador del menu y corazones de favoritos)
@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private Carrito carrito;

    @ModelAttribute("carrito")
    public Carrito carrito() {
        return carrito;
    }
}
