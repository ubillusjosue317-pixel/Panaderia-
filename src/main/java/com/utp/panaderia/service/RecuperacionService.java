package com.utp.panaderia.service;

public interface RecuperacionService {

    // Siempre termina igual, exista o no el correo (no revela que cuentas existen).
    // Si existe, crea un token aleatorio de un solo uso y manda el enlace por correo.
    void solicitar(String email);

    boolean tokenValido(String token);

    // Cambia la contraseña con un token valido; el token queda usado. Lanza IllegalArgumentException si no sirve.
    void restablecer(String token, String nuevaPassword);
}
