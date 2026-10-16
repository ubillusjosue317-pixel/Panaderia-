package com.pieroalonso.panaderia.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.pieroalonso.panaderia.model.Usuario;

/**
 * Usuario guardado en la sesion. Ademas del email y el rol guarda el id y el nombre,
 * asi las vistas pueden saludar al cliente sin volver a consultar la BD.
 */
public class UsuarioAutenticado extends User {

    private final Integer id;
    private final String nombre;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())));
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
    }

    public Integer getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
