package com.utp.panaderia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProductoForm {

    private Integer id;

    @NotNull(message = "Selecciona una categoría")
    private Integer idCategoria;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "Máximo 100 caracteres")
    private String nombre;

    @Size(max = 1000, message = "Máximo 1000 caracteres")
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.00", message = "El precio no puede ser negativo")
    @Digits(integer = 6, fraction = 2, message = "Formato de precio no válido")
    private BigDecimal precio;

    @Size(max = 255, message = "Máximo 255 caracteres")
    private String imagenUrl;

    private boolean disponible = true;
}
