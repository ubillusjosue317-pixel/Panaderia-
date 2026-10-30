package com.utp.panaderia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MensajeForm {

    @NotBlank(message = "Escribe el asunto")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String asunto;

    @NotBlank(message = "Escribe tu mensaje")
    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String contenido;
}
