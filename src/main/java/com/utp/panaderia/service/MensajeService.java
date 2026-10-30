package com.utp.panaderia.service;

import java.util.List;

import com.utp.panaderia.dto.MensajeForm;
import com.utp.panaderia.model.Mensaje;

public interface MensajeService {

    Mensaje enviar(Integer idUsuario, MensajeForm form);

    List<Mensaje> listarPorUsuario(Integer idUsuario);

    List<Mensaje> listarParaAdmin(boolean soloPendientes);

    Mensaje responder(Integer idMensaje, String respuesta);

    long contarPendientes();
}
