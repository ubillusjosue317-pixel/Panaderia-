package com.utp.panaderia.security;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import com.utp.panaderia.model.Usuario;

import lombok.Getter;

// Extendemos User para guardar tambien el id y el nombre en la sesion
// (asi en el navbar sale "Hola, <nombre>" sin consultar otra vez la BD)
@Getter
public class UsuarioAutenticado extends User {

    private final Integer id;
    private final String nombre;

    public UsuarioAutenticado(Usuario usuario) {
        super(usuario.getEmail(), usuario.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre())));
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
    }
}
