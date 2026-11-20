package com.utp.panaderia.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @DecimalMin(value = "0.01", message = "El precio mínimo es S/ 0.01")
    @Digits(integer = 6, fraction = 2, message = "Formato de precio no válido")
    private BigDecimal precio;

    // solo rutas propias del sitio (imagenes incluidas o subidas); no se aceptan URLs externas
    @Size(max = 255, message = "Máximo 255 caracteres")
    @Pattern(regexp = "^$|^/(img|uploads)/[A-Za-z0-9_-]+(/[A-Za-z0-9_-]+)*\\.(jpg|jpeg|png|webp|svg)$",
            message = "La imagen debe subirse desde tu equipo; no se aceptan enlaces externos")
    private String imagenUrl;

    @NotNull(message = "Ingresa el stock")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Max(value = 99999, message = "El stock máximo es 99999")
    private Integer stock = 50;

    private boolean disponible = true;
}
