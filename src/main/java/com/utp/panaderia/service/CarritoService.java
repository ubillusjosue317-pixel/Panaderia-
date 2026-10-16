package com.utp.panaderia.service;

import java.math.BigDecimal;
import java.util.List;

import com.utp.panaderia.dto.ItemCarrito;
import com.utp.panaderia.session.Carrito;

public interface CarritoService {

    List<ItemCarrito> obtenerItems(Carrito carrito);

    BigDecimal calcularTotal(List<ItemCarrito> items);
}
