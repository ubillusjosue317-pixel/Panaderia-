package com.utp.panaderia.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Venta cargada por el administrador desde un Excel. Es un registro aparte:
// no se mezcla con los pedidos que hacen los clientes en la web.
@Entity
@Table(name = "venta")
@Getter
@Setter
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 100)
    private String producto;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 100)
    private String archivo;

    @Column(name = "cargado_por", nullable = false, length = 100)
    private String cargadoPor;

    @Column(name = "cargado_en", nullable = false, updatable = false)
    private LocalDateTime cargadoEn;

    @PrePersist
    public void prePersist() {
        if (cargadoEn == null) {
            cargadoEn = LocalDateTime.now();
        }
    }
}
