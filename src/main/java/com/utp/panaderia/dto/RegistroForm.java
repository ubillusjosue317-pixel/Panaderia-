package com.utp.panaderia.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistroForm {

    @NotBlank(message = "Ingresa tu nombre")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String nombre;

    @NotBlank(message = "Ingresa tu apellido")
    @Size(max = 50, message = "Máximo 50 caracteres")
    private String apellido;

    @NotBlank(message = "Ingresa tu correo")
    @Email(message = "El correo no es válido")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String email;

    // es opcional, pero si lo ponen tiene que ser un celular (9 digitos y empieza con 9)
    @Pattern(regexp = "^$|^9\\d{8}$", message = "Debe tener 9 dígitos y empezar con 9")
    private String telefono;

    @NotBlank(message = "Ingresa una contraseña")
    @Size(min = 8, max = 60, message = "Debe tener entre 8 y 60 caracteres")
    private String password;

    @NotBlank(message = "Confirma tu contraseña")
    private String confirmarPassword;

    @AssertTrue(message = "Debes aceptar los términos para continuar")
    private boolean aceptaTerminos;
}
