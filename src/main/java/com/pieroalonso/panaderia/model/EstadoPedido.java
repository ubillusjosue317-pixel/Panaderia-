package com.pieroalonso.panaderia.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "estado_pedido")
public class EstadoPedido {

    public static final String PENDIENTE = "Pendiente";
    public static final String EN_PREPARACION = "En preparación";
    public static final String LISTO = "Listo";
    public static final String ENTREGADO = "Entregado";
    public static final String CANCELADO = "Cancelado";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Integer id;

    @Column(name = "nombre_estado", nullable = false, length = 30)
    private String nombre;

    protected EstadoPedido() {
    }

    public EstadoPedido(String nombre) {
        this.nombre = nombre;
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
