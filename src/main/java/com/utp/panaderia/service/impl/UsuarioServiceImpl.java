package com.utp.panaderia.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.RolRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional
    public Usuario registrarCliente(RegistroForm form) {
        String email = form.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalStateException("El correo ya está registrado: " + email);
        }

        Rol rol = rolRepository.findByNombre(Rol.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("No existe el rol CLIENTE"));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(form.getNombre().trim());
        usuario.setApellido(form.getApellido().trim());
        usuario.setEmail(email);
        if (form.getTelefono() != null && !form.getTelefono().isBlank()) {
            usuario.setTelefono(form.getTelefono().trim());
        }
        // nunca se guarda la contraseña en texto plano
        usuario.setPassword(passwordEncoder.encode(form.getPassword()));

        return usuarioRepository.save(usuario);
    }

    @Override
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("No existe el usuario " + id));
    }

    @Override
    @Transactional
    public void activarDosPasos(Integer idUsuario, String secreto) {
        Usuario usuario = buscarPorId(idUsuario);
        usuario.setTotpSecreto(secreto);
        usuarioRepository.save(usuario);
    }
}
