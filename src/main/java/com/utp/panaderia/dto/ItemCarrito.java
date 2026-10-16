package com.utp.panaderia.dto;

import java.math.BigDecimal;

import com.utp.panaderia.model.Producto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Una fila del carrito para mostrar en la vista
@Getter
@AllArgsConstructor
public class ItemCarrito {

    private Producto producto;
    private int cantidad;
    private BigDecimal subtotal;
}
