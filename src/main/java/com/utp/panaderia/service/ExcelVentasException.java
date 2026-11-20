package com.utp.panaderia.service;

import java.util.List;

// El archivo no se pudo cargar. Si hay errores por fila, vienen en la lista.
public class ExcelVentasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<String> errores;

    public ExcelVentasException(String mensaje) {
        this(mensaje, List.of());
    }

    public ExcelVentasException(String mensaje, List<String> errores) {
        super(mensaje);
        this.errores = errores;
    }

    public List<String> getErrores() {
        return errores;
    }
}
