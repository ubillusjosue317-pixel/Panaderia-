package com.utp.panaderia.service;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Usuario;

public interface UsuarioService {

    boolean existeEmail(String email);

    Usuario registrarCliente(RegistroForm form);

    Usuario buscarPorId(Integer id);

    // Guarda la clave de la app autenticadora cuando el usuario confirma su primer codigo
    void activarDosPasos(Integer idUsuario, String secreto);
}
