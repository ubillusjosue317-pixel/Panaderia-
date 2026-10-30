package com.utp.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.Mensaje;

public interface MensajeRepository extends JpaRepository<Mensaje, Integer> {

    List<Mensaje> findByUsuarioIdOrderByFechaEnvioDesc(Integer idUsuario);

    // primero los que faltan responder; trae al usuario porque open-in-view esta desactivado
    @Query("""
            select m from Mensaje m
            join fetch m.usuario
            where (:soloPendientes = false or m.respuesta is null)
            order by case when m.respuesta is null then 0 else 1 end, m.fechaEnvio desc
            """)
    List<Mensaje> listarParaAdmin(@Param("soloPendientes") boolean soloPendientes);

    long countByRespuestaIsNull();
}
