package com.utp.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Usuario;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByUsuarioOrderByFechaPedidoDesc(Usuario usuario);
}
