package com.utp.panaderia.service;

public interface CorreoService {

    // Envia el enlace para restablecer la contraseña (el enlace nunca incluye la contraseña)
    void enviarRecuperacion(String destino, String nombre, String enlace);
}
