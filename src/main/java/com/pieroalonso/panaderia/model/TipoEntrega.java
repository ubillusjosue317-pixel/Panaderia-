package com.pieroalonso.panaderia.model;

public enum TipoEntrega {

    RECOJO_TIENDA("Recojo en tienda"),
    DELIVERY("Delivery");

    private final String descripcion;

    TipoEntrega(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
