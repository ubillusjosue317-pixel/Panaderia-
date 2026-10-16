package com.utp.panaderia.service;

import java.util.List;
import java.util.Map;

import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.TipoEntrega;

public interface PedidoService {

    Pedido registrarPedido(Integer idUsuario, Map<Integer, Integer> items, TipoEntrega tipoEntrega, String direccion);

    List<Pedido> listarPorUsuario(Integer idUsuario);
}
