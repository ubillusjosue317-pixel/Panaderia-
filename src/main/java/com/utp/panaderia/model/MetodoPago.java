package com.utp.panaderia.model;

// Solo se elige en pedidos con delivery (en recojo en tienda se paga en el local)
public enum MetodoPago {
    EFECTIVO("Efectivo"),
    YAPE_PLIN("Yape / Plin"),
    TARJETA("Tarjeta contra entrega");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
