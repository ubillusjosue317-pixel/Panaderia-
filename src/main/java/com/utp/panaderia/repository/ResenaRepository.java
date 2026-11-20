package com.utp.panaderia.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    @Query("select r from Resena r join fetch r.usuario where r.producto.id = :idProducto order by r.fecha desc")
    List<Resena> listarDelProducto(@Param("idProducto") Integer idProducto);

    Optional<Resena> findByProductoIdAndUsuarioId(Integer idProducto, Integer idUsuario);

    @Query("select avg(r.calificacion) from Resena r where r.producto.id = :idProducto")
    Double promedio(@Param("idProducto") Integer idProducto);
}
