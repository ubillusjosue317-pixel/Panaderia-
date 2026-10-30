package com.utp.panaderia.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.utp.panaderia.dto.MensajeForm;
import com.utp.panaderia.model.Mensaje;
import com.utp.panaderia.repository.MensajeRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.MensajeService;

@Service
public class MensajeServiceImpl implements MensajeService {

    @Autowired
    private MensajeRepository mensajeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public Mensaje enviar(Integer idUsuario, MensajeForm form) {
        Mensaje mensaje = new Mensaje();
        mensaje.setUsuario(usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado")));
        mensaje.setAsunto(form.getAsunto().trim());
        mensaje.setContenido(form.getContenido().trim());
        return mensajeRepository.save(mensaje);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mensaje> listarPorUsuario(Integer idUsuario) {
        return mensajeRepository.findByUsuarioIdOrderByFechaEnvioDesc(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Mensaje> listarParaAdmin(boolean soloPendientes) {
        return mensajeRepository.listarParaAdmin(soloPendientes);
    }

    @Override
    @Transactional
    public Mensaje responder(Integer idMensaje, String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new IllegalArgumentException("Escribe la respuesta antes de enviarla.");
        }
        if (respuesta.trim().length() > 1000) {
            throw new IllegalArgumentException("La respuesta no puede tener más de 1000 caracteres.");
        }
        Mensaje mensaje = mensajeRepository.findById(idMensaje)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mensaje no encontrado"));
        mensaje.setRespuesta(respuesta.trim());
        mensaje.setFechaRespuesta(LocalDateTime.now());
        return mensajeRepository.save(mensaje);
    }

    @Override
    public long contarPendientes() {
        return mensajeRepository.countByRespuestaIsNull();
    }
}
