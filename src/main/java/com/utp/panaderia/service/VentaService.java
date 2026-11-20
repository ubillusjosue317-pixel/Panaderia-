package com.utp.panaderia.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.utp.panaderia.model.Venta;

public interface VentaService {

    // Lee el Excel (.xlsx) y guarda todas sus filas, o ninguna si alguna fila tiene errores.
    // Devuelve cuantas ventas se cargaron.
    int cargarExcel(MultipartFile archivo);

    List<Venta> ultimas();

    long contar();

    BigDecimal totalGeneral();

    // Excel de ejemplo con las columnas que espera la carga
    byte[] plantilla();
}
