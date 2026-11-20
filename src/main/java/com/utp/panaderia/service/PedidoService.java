package com.utp.panaderia.service;

import java.util.List;
import java.util.Map;

import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.TipoEntrega;

public interface PedidoService {

    // El metodo de pago es obligatorio solo en delivery; en recojo en tienda se ignora
    Pedido registrarPedido(Integer idUsuario, Map<Integer, Integer> items, TipoEntrega tipoEntrega,
                           String direccion, MetodoPago metodoPago);

    List<Pedido> listarPorUsuario(Integer idUsuario);

    // El cliente solo puede cancelar su propio pedido mientras siga Pendiente
    Pedido cancelarPropio(Integer idUsuario, Integer idPedido);

    // Productos y cantidades de un pedido del propio cliente (para volver a pedir)
    Map<Integer, Integer> itemsDePedidoPropio(Integer idUsuario, Integer idPedido);

    // ---------- administrador ----------

    List<Pedido> listarParaAdmin(Integer idEstado);

    List<EstadoPedido> listarEstados();

    // cantidad de pedidos por estado, para los filtros de la ventana Pedidos
    Map<Integer, Long> contarPorEstado();

    long contarPendientes();

    Pedido cambiarEstado(Integer idPedido, Integer idEstado);

    // Elimina un pedido. Solo se permite con pedidos CANCELADOS (su stock ya fue devuelto); los demas
    // son ventas reales o pedidos en curso y no se borran. Queda registrado en la auditoria.
    void eliminar(Integer idPedido);
}
