package com.pieroalonso.panaderia.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.pieroalonso.panaderia.dto.RegistroForm;
import com.pieroalonso.panaderia.model.Rol;
import com.pieroalonso.panaderia.model.Usuario;
import com.pieroalonso.panaderia.repository.RolRepository;
import com.pieroalonso.panaderia.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean emailRegistrado(String email) {
        return usuarioRepository.existsByEmail(normalizarEmail(email));
    }

    @Transactional
    public Usuario registrarCliente(RegistroForm form) {
        String email = normalizarEmail(form.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalStateException("El correo ya está registrado: " + email);
        }

        Rol rolCliente = rolRepository.findByNombre(Rol.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("No existe el rol " + Rol.CLIENTE));

        Usuario usuario = new Usuario();
        usuario.setRol(rolCliente);
        usuario.setNombre(form.getNombre().trim());
        usuario.setApellido(form.getApellido().trim());
        usuario.setEmail(email);
        usuario.setTelefono(StringUtils.hasText(form.getTelefono()) ? form.getTelefono().trim() : null);
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));

        return usuarioRepository.save(usuario);
    }

    private String normalizarEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
