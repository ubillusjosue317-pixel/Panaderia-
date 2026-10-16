package com.pieroalonso.panaderia.dto;

import java.math.BigDecimal;

import com.pieroalonso.panaderia.model.Producto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    public static ProductoForm desde(Producto producto) {
        ProductoForm form = new ProductoForm();
        form.id = producto.getId();
        form.idCategoria = producto.getCategoria().getId();
        form.nombre = producto.getNombre();
        form.descripcion = producto.getDescripcion();
        form.precio = producto.getPrecio();
        form.imagenUrl = producto.getImagenUrl();
        form.disponible = producto.isDisponible();
        return form;
    }

    public boolean esNuevo() {
        return id == null;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}
