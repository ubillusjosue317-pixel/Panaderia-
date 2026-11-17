package com.utp.panaderia.dto;

import com.utp.panaderia.validation.ClaveSegura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CambioClaveForm {

    @NotBlank(message = "Ingresa tu contraseña actual")
    private String actual;

    @NotBlank(message = "Ingresa la nueva contraseña")
    @Size(min = 8, max = 60, message = "Debe tener entre 8 y 60 caracteres")
    @ClaveSegura
    private String nueva;

    @NotBlank(message = "Confirma la nueva contraseña")
    private String confirmar;
}
