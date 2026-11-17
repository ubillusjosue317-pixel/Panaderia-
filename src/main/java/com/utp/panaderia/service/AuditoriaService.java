package com.utp.panaderia.service;

public interface AuditoriaService {

    // Guarda quien (usuario autenticado), cuando y que cambio
    void registrar(String accion, String entidad, Integer idEntidad, String detalle);
}
