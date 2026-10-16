package com.utp.panaderia.service;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Usuario;

public interface UsuarioService {

    boolean existeEmail(String email);

    Usuario registrarCliente(RegistroForm form);
}
