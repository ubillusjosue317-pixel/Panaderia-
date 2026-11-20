package com.utp.panaderia.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.utp.panaderia.dto.ItemCarrito;
import com.utp.panaderia.model.MetodoPago;
import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.model.TipoEntrega;
import com.utp.panaderia.security.UsuarioAutenticado;
import com.utp.panaderia.service.CarritoService;
import com.utp.panaderia.service.PedidoService;
import com.utp.panaderia.service.ProductoService;
import com.utp.panaderia.session.Carrito;
import com.utp.panaderia.util.Redireccion;

@Controller
public class PedidoController {

    @Autowired
    private Carrito carrito;

    @Autowired
    private CarritoService carritoService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private PedidoService pedidoService;

    // ---------- carrito ----------

    @GetMapping("/pedidos")
    public String verCarrito(Model model) {
        List<ItemCarrito> items = carritoService.obtenerItems(carrito);
        model.addAttribute("items", items);
        model.addAttribute("total", carritoService.calcularTotal(items));
        model.addAttribute("metodosPago", MetodoPago.values());
        return "tienda/carrito";
    }

    @PostMapping("/carrito/agregar/{id}")
    public String agregar(@PathVariable Integer id,
                          @RequestHeader(value = "Referer", required = false) String referer,
                          RedirectAttributes redirect) {
        Producto producto = productoService.buscarPorId(id);
        int enCesta = carrito.getItems().getOrDefault(id, 0);
        if (producto.isDisponible() && enCesta >= producto.getStock()) {
            redirect.addFlashAttribute("error", producto.getStock() == 0
                    ? producto.getNombre() + " está agotado."
                    : "Solo quedan " + producto.getStock() + " unidades de " + producto.getNombre() + ".");
        } else if (producto.isDisponible()) {
            if (carrito.agregar(id)) {
                redirect.addFlashAttribute("mensaje", producto.getNombre() + " se agregó a tu pedido.");
            } else {
                redirect.addFlashAttribute("error", "Máximo " + Carrito.MAXIMO_POR_PRODUCTO
                        + " unidades por producto en cada pedido.");
            }
        }
        return Redireccion.volver(referer, "/pedidos");
    }

    @PostMapping("/carrito/quitar/{id}")
    public String quitar(@PathVariable Integer id) {
        carrito.quitar(id);
        return "redirect:/pedidos";
    }

    // ---------- confirmar pedido (requiere login) ----------

    // si el cliente no habia iniciado sesion, despues del login vuelve aqui
    @GetMapping("/pedidos/confirmar")
    public String volverAlCarrito() {
        return "redirect:/pedidos";
    }

    @PostMapping("/pedidos/confirmar")
    public String confirmar(@RequestParam(required = false) String tipoEntrega,
                            @RequestParam(required = false) String direccion,
                            @RequestParam(required = false) String metodoPago,
                            @AuthenticationPrincipal UsuarioAutenticado usuario,
                            RedirectAttributes redirect) {
        try {
            Pedido pedido = pedidoService.registrarPedido(usuario.getId(), carrito.getItems(),
                    convertirTipoEntrega(tipoEntrega), direccion, convertirMetodoPago(metodoPago));
            carrito.vaciar();
            redirect.addFlashAttribute("mensaje", "¡Listo! Tu pedido #" + pedido.getId() + " fue registrado.");
            return "redirect:/mis-pedidos";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/pedidos";
        }
    }

    // si llega un valor que no existe (formulario alterado) se trata como "no eligio"
    private TipoEntrega convertirTipoEntrega(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return TipoEntrega.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private MetodoPago convertirMetodoPago(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return MetodoPago.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ---------- cancelar y volver a pedir ----------

    @PostMapping("/mis-pedidos/{id}/cancelar")
    public String cancelar(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                           RedirectAttributes redirect) {
        try {
            pedidoService.cancelarPropio(usuario.getId(), id);
            redirect.addFlashAttribute("mensaje", "Tu pedido #" + id + " fue cancelado.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/mis-pedidos";
    }

    // Pone en la cesta los mismos productos de un pedido anterior (sin pasar del maximo
    // por producto y omitiendo los que ya no estan disponibles)
    @PostMapping("/mis-pedidos/{id}/volver-a-pedir")
    public String volverAPedir(@PathVariable Integer id, @AuthenticationPrincipal UsuarioAutenticado usuario,
                               RedirectAttributes redirect) {
        try {
            int agregados = 0;
            int omitidos = 0;
            for (Map.Entry<Integer, Integer> item : pedidoService.itemsDePedidoPropio(usuario.getId(), id).entrySet()) {
                Producto producto = productoService.buscarPorId(item.getKey());
                if (!producto.isDisponible()) {
                    omitidos++;
                    continue;
                }
                int cabenPorStock = producto.getStock() - carrito.getItems().getOrDefault(item.getKey(), 0);
                agregados += carrito.agregar(item.getKey(), Math.min(item.getValue(), Math.max(0, cabenPorStock)));
            }
            String mensaje = agregados + " unidades del pedido #" + id + " se agregaron a tu cesta.";
            if (omitidos > 0) {
                mensaje += " " + omitidos + " producto(s) ya no están disponibles.";
            }
            redirect.addFlashAttribute("mensaje", mensaje);
            return "redirect:/pedidos";
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/mis-pedidos";
        }
    }

    @GetMapping("/mis-pedidos")
    public String misPedidos(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("pedidos", pedidoService.listarPorUsuario(usuario.getId()));
        return "tienda/mis-pedidos";
    }
}
