package com.utp.panaderia.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estado_pedido")
@Getter
@Setter
@NoArgsConstructor
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

    public EstadoPedido(String nombre) {
        this.nombre = nombre;
    }
}
