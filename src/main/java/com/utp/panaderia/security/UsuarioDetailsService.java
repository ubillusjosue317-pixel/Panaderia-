package com.utp.panaderia.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.utp.panaderia.repository.UsuarioRepository;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private BloqueoIntentos bloqueo;

    @Override
    public UserDetails loadUserByUsername(String email) {
        boolean bloqueada = bloqueo.estaBloqueado(email);
        return usuarioRepository.findByEmail(email.trim().toLowerCase())
                .map(u -> new UsuarioAutenticado(u, bloqueada))
                .orElseThrow(() -> new UsernameNotFoundException("No existe una cuenta con ese correo"));
    }
}
