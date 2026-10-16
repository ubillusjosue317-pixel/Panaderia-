package com.utp.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.Pedido;
import com.utp.panaderia.model.Usuario;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    List<Pedido> findByUsuarioOrderByFechaPedidoDesc(Usuario usuario);

    // trae los detalles en la misma consulta porque open-in-view esta desactivado
    @Query("""
            select distinct p from Pedido p
            left join fetch p.detalles d
            left join fetch d.producto
            where p.usuario.id = :idUsuario
            order by p.fechaPedido desc
            """)
    List<Pedido> listarConDetalles(@Param("idUsuario") Integer idUsuario);
}
