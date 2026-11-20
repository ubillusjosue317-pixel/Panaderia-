package com.utp.panaderia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// Solo los datos que la persona puede cambiar de su propio perfil. A proposito NO tiene id,
// correo ni rol: aunque alguien agregue esos campos a la peticion, no se asignan (mass assignment).
@Data
public class PerfilForm {

    @NotBlank(message = "Ingresa tu nombre")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;

    @NotBlank(message = "Ingresa tu apellido")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String apellido;

    @Pattern(regexp = "^$|^9\\d{8}$", message = "Debe tener 9 dígitos y empezar con 9")
    private String telefono;
}
