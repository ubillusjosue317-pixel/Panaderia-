package com.utp.panaderia.model;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellido;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(length = 15)
    private String telefono;

    @Column(nullable = false)
    private String password;

    // Clave de la verificacion en 2 pasos (Google/Microsoft Authenticator).
    // Es null hasta que el usuario escanea el QR en su primer inicio de sesion.
    @Column(name = "totp_secreto", length = 32)
    private String totpSecreto;

    // false solo para el administrador recien creado: mientras no escriba su primer codigo,
    // la pantalla de verificacion le muestra el QR para vincular su Authenticator
    @Column(name = "totp_confirmado", nullable = false, columnDefinition = "boolean default true")
    private boolean totpConfirmado = true;

    // true cuando la persona debe reemplazar su contraseña antes de usar el sistema (admin recien creado)
    @Column(name = "debe_cambiar_password", nullable = false, columnDefinition = "boolean default false")
    private boolean debeCambiarPassword;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }
}
