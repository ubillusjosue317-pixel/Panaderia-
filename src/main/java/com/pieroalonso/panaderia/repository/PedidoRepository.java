package com.pieroalonso.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pieroalonso.panaderia.model.Pedido;
import com.pieroalonso.panaderia.model.Usuario;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByUsuarioOrderByFechaPedidoDesc(Usuario usuario);
}
