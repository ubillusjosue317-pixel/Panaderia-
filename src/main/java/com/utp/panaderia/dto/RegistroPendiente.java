package com.utp.panaderia.dto;

import java.io.Serializable;

import lombok.Data;

// Datos de un registro que todavia no termina: la cuenta se crea recien cuando la persona
// escanea el QR y escribe un codigo valido. La contraseña se guarda ya cifrada.
@Data
public class RegistroPendiente implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String passwordCifrada;
    private String secreto;
    private long creadoEn = System.currentTimeMillis();
    private int intentos;
}
