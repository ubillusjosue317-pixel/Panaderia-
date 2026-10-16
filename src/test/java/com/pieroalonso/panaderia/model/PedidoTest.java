package com.pieroalonso.panaderia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class PedidoTest {

    @Test
    void elTotalEsLaSumaDeLosSubtotales() {
        Pedido pedido = new Pedido();
        pedido.agregarDetalle(producto("Pan camote", "1.00"), 5);
        pedido.agregarDetalle(producto("Torta moka", "4.00"), 2);

        assertEquals(new BigDecimal("13.00"), pedido.getTotal());
        assertEquals(2, pedido.getDetalles().size());
    }

    @Test
    void elDetalleGuardaElPrecioDelMomento() {
        Producto croissant = producto("Croissant", "2.50");
        Pedido pedido = new Pedido();
        pedido.agregarDetalle(croissant, 2);

        croissant.setPrecio(new BigDecimal("3.00"));

        DetallePedido detalle = pedido.getDetalles().get(0);
        assertEquals(new BigDecimal("2.50"), detalle.getPrecioUnitario());
        assertEquals(new BigDecimal("5.00"), detalle.getSubtotal());
    }

    @Test
    void noSePermiteCantidadCero() {
        Pedido pedido = new Pedido();
        assertThrows(IllegalArgumentException.class,
                () -> pedido.agregarDetalle(producto("Empanada", "3.50"), 0));
    }

    private Producto producto(String nombre, String precio) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(new BigDecimal(precio));
        return p;
    }
}
