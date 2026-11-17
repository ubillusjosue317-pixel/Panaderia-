package com.utp.panaderia.service;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.dto.RegistroPendiente;
import com.utp.panaderia.model.Usuario;

public interface UsuarioService {

    boolean existeEmail(String email);

    // La cuenta nace con su clave de la app autenticadora (la que se escaneo al registrarse)
    Usuario registrarCliente(RegistroForm form, String secreto);

    Usuario crearCliente(RegistroPendiente pendiente);

    String cifrarPassword(String password);

    // Cambia la contraseña (pide la actual) y quita la obligacion de cambiarla
    void cambiarPassword(Integer idUsuario, String actual, String nueva);

    Usuario buscarPorId(Integer id);
}
