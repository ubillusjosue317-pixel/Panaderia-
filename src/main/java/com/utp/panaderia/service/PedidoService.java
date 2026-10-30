package com.utp.panaderia.service;

import java.util.List;
import java.util.Map;

import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.TipoEntrega;

public interface PedidoService {

    Pedido registrarPedido(Integer idUsuario, Map<Integer, Integer> items, TipoEntrega tipoEntrega, String direccion);

    List<Pedido> listarPorUsuario(Integer idUsuario);

    // ---------- administrador ----------

    List<Pedido> listarParaAdmin(Integer idEstado);

    List<EstadoPedido> listarEstados();

    // cantidad de pedidos por estado, para los filtros de la ventana Pedidos
    Map<Integer, Long> contarPorEstado();

    long contarPendientes();

    Pedido cambiarEstado(Integer idPedido, Integer idEstado);
}
