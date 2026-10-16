package com.pieroalonso.panaderia.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    // Celular peruano: 9 dígitos empezando en 9. Es opcional.
    @Pattern(regexp = "^$|^9\\d{8}$", message = "Debe tener 9 dígitos y empezar con 9")
    private String telefono;

    @NotBlank(message = "Ingresa una contraseña")
    @Size(min = 8, max = 60, message = "Debe tener entre 8 y 60 caracteres")
    private String password;

    @NotBlank(message = "Confirma tu contraseña")
    private String confirmarPassword;

    @AssertTrue(message = "Debes aceptar los términos para continuar")
    private boolean aceptaTerminos;

    public boolean passwordsCoinciden() {
        return password != null && password.equals(confirmarPassword);
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmarPassword() {
        return confirmarPassword;
    }

    public void setConfirmarPassword(String confirmarPassword) {
        this.confirmarPassword = confirmarPassword;
    }

    public boolean isAceptaTerminos() {
        return aceptaTerminos;
    }

    public void setAceptaTerminos(boolean aceptaTerminos) {
        this.aceptaTerminos = aceptaTerminos;
    }
}
