package com.utp.panaderia.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.dto.RegistroForm;
import com.utp.panaderia.dto.RegistroPendiente;
import com.utp.panaderia.model.Rol;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.RolRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.UsuarioService;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaService auditoriaService;

    @Override
    public boolean existeEmail(String email) {
        return usuarioRepository.existsByEmail(email.trim().toLowerCase());
    }

    @Override
    @Transactional
    public Usuario registrarCliente(RegistroForm form, String secreto) {
        RegistroPendiente p = new RegistroPendiente();
        p.setNombre(form.getNombre());
        p.setApellido(form.getApellido());
        p.setEmail(form.getEmail());
        p.setTelefono(form.getTelefono());
        // nunca se guarda la contraseña en texto plano
        p.setPasswordCifrada(passwordEncoder.encode(form.getPassword()));
        p.setSecreto(secreto);
        return crearCliente(p);
    }

    @Override
    @Transactional
    public Usuario crearCliente(RegistroPendiente p) {
        String email = p.getEmail().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new IllegalStateException("El correo ya está registrado: " + email);
        }
        if (p.getSecreto() == null || p.getSecreto().isBlank()) {
            throw new IllegalStateException("La cuenta necesita su clave de verificación en 2 pasos");
        }

        Rol rol = rolRepository.findByNombre(Rol.CLIENTE)
                .orElseThrow(() -> new IllegalStateException("No existe el rol CLIENTE"));

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombre(p.getNombre().trim());
        usuario.setApellido(p.getApellido().trim());
        usuario.setEmail(email);
        if (p.getTelefono() != null && !p.getTelefono().isBlank()) {
            usuario.setTelefono(p.getTelefono().trim());
        }
        usuario.setPassword(p.getPasswordCifrada());
        usuario.setTotpSecreto(p.getSecreto());

        return usuarioRepository.save(usuario);
    }

    @Override
    public String cifrarPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    @Transactional
    public void confirmarDosPasos(Integer idUsuario) {
        Usuario usuario = buscarPorId(idUsuario);
        usuario.setTotpConfirmado(true);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void cambiarPassword(Integer idUsuario, String actual, String nueva) {
        Usuario usuario = buscarPorId(idUsuario);
        if (!passwordEncoder.matches(actual, usuario.getPassword())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (passwordEncoder.matches(nueva, usuario.getPassword())) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual.");
        }
        usuario.setPassword(passwordEncoder.encode(nueva));
        usuario.setDebeCambiarPassword(false);
        usuarioRepository.save(usuario);
        auditoriaService.registrar("CAMBIO_CLAVE", "Usuario", usuario.getId(), "Cambio de contraseña de " + usuario.getEmail());
    }

    @Override
    public Usuario buscarPorId(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("No existe el usuario " + id));
    }
}
