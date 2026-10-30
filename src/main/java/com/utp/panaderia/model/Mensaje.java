package com.utp.panaderia.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// Consulta que un cliente le escribe a la panaderia. El administrador la responde desde Mensajeria.
@Entity
@Table(name = "mensaje")
@Getter
@Setter
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String asunto;

    @Column(nullable = false, length = 1000)
    private String contenido;

    @Column(name = "fecha_envio", nullable = false, updatable = false)
    private LocalDateTime fechaEnvio;

    // null mientras el administrador no responda
    @Column(length = 1000)
    private String respuesta;

    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;

    @PrePersist
    public void prePersist() {
        if (fechaEnvio == null) {
            fechaEnvio = LocalDateTime.now();
        }
    }

    public boolean isRespondido() {
        return respuesta != null;
    }
}
