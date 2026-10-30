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
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.model.Usuario;
import com.utp.panaderia.repository.EstadoPedidoRepository;
import com.utp.panaderia.repository.PedidoRepository;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.repository.UsuarioRepository;
import com.utp.panaderia.service.PedidoService;

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

    @Override
    @Transactional
    public Pedido registrarPedido(Integer idUsuario, Map<Integer, Integer> items,
                                  TipoEntrega tipoEntrega, String direccion) {
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
        }

        // el precio se toma de la BD, no de lo que manda el navegador
        for (Map.Entry<Integer, Integer> item : items.entrySet()) {
            Producto producto = productoRepository.findById(item.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no existe."));
            if (!producto.isDisponible()) {
                throw new IllegalArgumentException("El producto " + producto.getNombre() + " ya no está disponible.");
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
    public Pedido cambiarEstado(Integer idPedido, Integer idEstado) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        EstadoPedido estado = estadoRepository.findById(idEstado)
                .orElseThrow(() -> new IllegalArgumentException("El estado elegido no existe."));
        pedido.setEstado(estado);
        return pedidoRepository.save(pedido);
    }
}
