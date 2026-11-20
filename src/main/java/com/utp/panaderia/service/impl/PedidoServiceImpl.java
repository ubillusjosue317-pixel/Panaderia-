package com.utp.panaderia.service.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.utp.panaderia.model.EstadoPedido;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.EstadoPedidoRepository;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.AuditoriaService;
import com.utp.panaderia.service.ComprobanteService;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.session.Carrito;

@Service
public class PedidoServiceImpl implements PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoPedidoRepository estadoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private ComprobanteService comprobanteService;

    @Override
    @Transactional
    public Pedido registrarPedido(Integer idUsuario, Map<Integer, Integer> items,
                                  TipoEntrega tipoEntrega, String direccion, MetodoPago metodoPago) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Tu cesta está vacía.");
        }
        if (tipoEntrega == null) {
            throw new IllegalArgumentException("Elige una modalidad de entrega.");
        }
        if (tipoEntrega == TipoEntrega.DELIVERY && (direccion == null || direccion.isBlank())) {
            throw new IllegalArgumentException("Ingresa la dirección de entrega para el delivery.");
        }
        // la columna direccion_entrega es VARCHAR(200)
        if (tipoEntrega == TipoEntrega.DELIVERY && direccion.trim().length() > 200) {
            throw new IllegalArgumentException("La dirección no puede tener más de 200 caracteres.");
        }
        if (tipoEntrega == TipoEntrega.DELIVERY && metodoPago == null) {
            throw new IllegalArgumentException("Elige un método de pago para el delivery.");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        EstadoPedido pendiente = estadoRepository.findByNombre(EstadoPedido.PENDIENTE)
                .orElseThrow(() -> new IllegalStateException("No existe el estado Pendiente"));

        Pedido pedido = new Pedido();
        pedido.setUsuario(usuario);
        pedido.setEstado(pendiente);
        pedido.setTipoEntrega(tipoEntrega);
        if (tipoEntrega == TipoEntrega.DELIVERY) {
            pedido.setDireccionEntrega(direccion.trim());
            pedido.setMetodoPago(metodoPago);
        }

        // el precio se toma de la BD, no de lo que manda el navegador
        for (Map.Entry<Integer, Integer> item : items.entrySet()) {
            Producto producto = productoRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no existe."));
            if (!producto.isDisponible()) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " ya no está disponible.");
            }
            if (item.getValue() == null || item.getValue() < 1 || item.getValue() > Carrito.MAXIMO_POR_PRODUCTO) {
                throw new IllegalArgumentException("Máximo " + Carrito.MAXIMO_POR_PRODUCTO
                        + " unidades por producto (" + producto.getNombre() + ").");
            }
            // descuento atomico: si no alcanza el stock, se cancela todo el pedido (rollback)
            if (productoRepository.descontarStock(producto.getId(), item.getValue()) == 0) {
                throw new IllegalArgumentException("No hay stock suficiente de " + producto.getNombre()
                        + " (quedan " + Math.max(0, productoRepository.findById(producto.getId())
                                .map(Producto::getStock).orElse(0)) + ").");
            }
            pedido.agregarDetalle(producto, item.getValue());
        }

        return pedidoRepository.save(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPorUsuario(Integer idUsuario) {
        return pedidoRepository.listarConDetalles(idUsuario);
    }

    @Override
    @Transactional
    public Pedido cancelarPropio(Integer idUsuario, Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .filter(p -> p.getUsuario().getId().equals(idUsuario))
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ese pedido."));
        if (!EstadoPedido.PENDIENTE.equals(pedido.getEstado().getNombre())) {
            throw new IllegalArgumentException("Solo se pueden cancelar los pedidos que siguen Pendientes.");
        }
        pedido.setEstado(estadoRepository.findByNombre(EstadoPedido.CANCELADO)
                .orElseThrow(() -> new IllegalStateException("No existe el estado Cancelado")));
        reponerStock(pedido);
        Pedido guardado = pedidoRepository.save(pedido);
        auditoriaService.registrar("CANCELAR", "Pedido", idPedido, "El cliente canceló su pedido");
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Integer> itemsDePedidoPropio(Integer idUsuario, Integer idPedido) {
        Pedido pedido = pedidoRepository.listarConDetalles(idUsuario).stream()
                .filter(p -> p.getId().equals(idPedido))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ese pedido."));
        Map<Integer, Integer> items = new LinkedHashMap<>();
        pedido.getDetalles().forEach(d -> items.merge(d.getProducto().getId(), d.getCantidad(), Integer::sum));
        return items;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarParaAdmin(Integer idEstado) {
        return pedidoRepository.listarParaAdmin(idEstado);
    }

    @Override
    public List<EstadoPedido> listarEstados() {
        return estadoRepository.findAll(Sort.by("id"));
    }

    @Override
    public Map<Integer, Long> contarPorEstado() {
        Map<Integer, Long> cantidades = new LinkedHashMap<>();
        for (EstadoPedido estado : listarEstados()) {
            cantidades.put(estado.getId(), pedidoRepository.countByEstadoId(estado.getId()));
        }
        return cantidades;
    }

    @Override
    public long contarPendientes() {
        return pedidoRepository.countByEstadoNombre(EstadoPedido.PENDIENTE);
    }

    @Override
    @Transactional
    public void eliminar(Integer idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new IllegalArgumentException("No encontramos ese pedido."));
        if (!EstadoPedido.CANCELADO.equals(pedido.getEstado().getNombre())) {
            throw new IllegalArgumentException("Solo se pueden eliminar los pedidos cancelados. "
                    + "Cambia primero el estado a Cancelado si de verdad quieres quitarlo.");
        }
        // se anota antes de borrar: despues ya no habria datos que mostrar
        auditoriaService.registrar("ELIMINAR", "Pedido", idPedido,
                "Pedido eliminado: cliente " + pedido.getUsuario().getEmail() + ", S/ " + pedido.getTotal()
                        + ", fecha " + pedido.getFechaPedido());
        String comprobante = pedido.getComprobante();
        pedidoRepository.delete(pedido);
        pedidoRepository.flush();
        comprobanteService.eliminarArchivo(comprobante);
    }

    // Un pedido cancelado devuelve sus unidades al stock
    private void reponerStock(Pedido pedido) {
        pedido.getDetalles().forEach(d -> productoRepository.reponerStock(d.getProducto().getId(), d.getCantidad()));
    }

    @Override
    @Transactional
    public Pedido cambiarEstado(Integer idPedido, Integer idEstado) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        EstadoPedido estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalArgumentException("El estado elegido no existe."));
        String anterior = pedido.getEstado() != null ? pedido.getEstado().getNombre() : "-";
        boolean cancelaAhora = !EstadoPedido.CANCELADO.equals(anterior) && EstadoPedido.CANCELADO.equals(estado.getNombre());
        boolean reactiva = EstadoPedido.CANCELADO.equals(anterior) && !EstadoPedido.CANCELADO.equals(estado.getNombre());
        if (cancelaAhora) {
            reponerStock(pedido);
        } else if (reactiva) {
            for (var d : pedido.getDetalles()) {
                if (productoRepository.descontarStock(d.getProducto().getId(), d.getCantidad()) == 0) {
                    throw new IllegalArgumentException("No hay stock suficiente de " + d.getProducto().getNombre()
                            + " para reactivar el pedido.");
                }
            }
        }
        pedido.setEstado(estado);
        Pedido guardado = pedidoRepository.save(pedido);
        auditoriaService.registrar("ESTADO", "Pedido", idPedido, "estado " + anterior + " -> " + estado.getNombre());
        return guardado;
    }
}
