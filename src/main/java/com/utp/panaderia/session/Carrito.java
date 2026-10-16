package com.utp.panaderia.session;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import lombok.Getter;

// Se crea uno por cada sesion (cada cliente tiene su propio carrito).
// Solo guardamos ids y cantidades, los precios se leen de la BD al confirmar.
@Component
@SessionScope
@Getter
public class Carrito implements Serializable {

    private static final long serialVersionUID = 1L;

    // id del producto -> cantidad
    private final Map<Integer, Integer> items = new LinkedHashMap<>();

    private final Set<Integer> favoritos = new LinkedHashSet<>();

    public void agregar(Integer idProducto) {
        items.put(idProducto, items.getOrDefault(idProducto, 0) + 1);
    }

    public void quitar(Integer idProducto) {
        Integer cantidad = items.get(idProducto);
        if (cantidad == null) {
            return;
        }
        if (cantidad <= 1) {
            items.remove(idProducto);
        } else {
            items.put(idProducto, cantidad - 1);
        }
    }

    public int getCantidadTotal() {
        int total = 0;
        for (Integer cantidad : items.values()) {
            total += cantidad;
        }
        return total;
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    public void vaciar() {
        items.clear();
    }

    public void marcarFavorito(Integer idProducto) {
        if (favoritos.contains(idProducto)) {
            favoritos.remove(idProducto);
        } else {
            favoritos.add(idProducto);
        }
    }

    public boolean esFavorito(Integer idProducto) {
        return favoritos.contains(idProducto);
    }
}
