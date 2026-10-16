package com.utp.panaderia.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.utp.panaderia.dto.ItemCarrito;
import com.utp.panaderia.model.Producto;
import com.utp.panaderia.repository.ProductoRepository;
import com.utp.panaderia.service.CarritoService;
import com.utp.panaderia.session.Carrito;

@Service
public class CarritoServiceImpl implements CarritoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public List<ItemCarrito> obtenerItems(Carrito carrito) {
        List<ItemCarrito> lista = new ArrayList<>();
        for (Map.Entry<Integer, Integer> item : carrito.getItems().entrySet()) {
            Producto producto = productoRepository.findById(item.getKey()).orElse(null);
            if (producto == null) {
                continue;
            }
            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.getValue()));
            lista.add(new ItemCarrito(producto, item.getValue(), subtotal));
        }
        return lista;
    }

    @Override
    public BigDecimal calcularTotal(List<ItemCarrito> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemCarrito item : items) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }
}
