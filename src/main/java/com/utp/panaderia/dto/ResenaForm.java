package com.utp.panaderia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResenaForm {

    @NotNull(message = "Elige de 1 a 5 estrellas")
    @Min(value = 1, message = "Elige de 1 a 5 estrellas")
    @Max(value = 5, message = "Elige de 1 a 5 estrellas")
    private Integer calificacion;

    @NotBlank(message = "Escribe tu comentario")
    @Size(max = 500, message = "Máximo 500 caracteres")
    private String comentario;
}
