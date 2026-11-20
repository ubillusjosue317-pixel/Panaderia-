package com.utp.panaderia.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Reseña de un cliente sobre un producto (una por cliente y producto)
@Entity
@Table(name = "resena", uniqueConstraints = @UniqueConstraint(columnNames = { "id_usuario", "id_producto" }))
@Getter
@Setter
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resena")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private int calificacion;

    @Column(nullable = false, length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    @PreUpdate
    public void antesDeGuardar() {
        fecha = LocalDateTime.now();
    }
}
