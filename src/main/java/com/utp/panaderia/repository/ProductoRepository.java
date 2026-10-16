package com.utp.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    @Query("""
            select p from Producto p
            where p.disponible = true
              and (:idCategoria is null or p.categoria.id = :idCategoria)
              and (:texto is null or lower(p.nombre) like lower(concat('%', :texto, '%')))
            order by p.nombre
            """)
    List<Producto> buscarDisponibles(@Param("idCategoria") Integer idCategoria,
                                     @Param("texto") String texto);

    List<Producto> findAllByOrderByCategoriaNombreAscNombreAsc();
}
