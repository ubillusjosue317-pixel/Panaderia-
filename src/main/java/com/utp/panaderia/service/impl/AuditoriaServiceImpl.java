package com.utp.panaderia.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utp.panaderia.model.Auditoria;
import com.utp.panaderia.repository.AuditoriaRepository;
import com.utp.panaderia.service.AuditoriaService;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional
    public void registrar(String accion, String entidad, Integer idEntidad, String detalle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usuario = (auth != null && auth.isAuthenticated()) ? auth.getName() : "sistema";

        Auditoria registro = new Auditoria();
        registro.setUsuario(usuario);
        registro.setAccion(accion);
        registro.setEntidad(entidad);
        registro.setIdEntidad(idEntidad);
        registro.setDetalle(detalle != null && detalle.length() > 500 ? detalle.substring(0, 500) : detalle);
        auditoriaRepository.save(registro);
    }
}
