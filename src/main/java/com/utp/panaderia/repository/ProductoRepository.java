package com.utp.panaderia.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {

    // Descuenta stock en UNA sola instruccion SQL atomica: la base de datos bloquea la fila y
    // solo actualiza si todavia alcanza. Asi dos compras al mismo tiempo no pueden vender la
    // misma ultima unidad (condicion de carrera). Devuelve 0 si no hay stock suficiente.
    @Modifying(flushAutomatically = true)
    @Query("update Producto p set p.stock = p.stock - :cantidad where p.id = :id and p.stock >= :cantidad")
    int descontarStock(@Param("id") Integer id, @Param("cantidad") int cantidad);

    @Modifying(flushAutomatically = true)
    @Query("update Producto p set p.stock = p.stock + :cantidad where p.id = :id")
    int reponerStock(@Param("id") Integer id, @Param("cantidad") int cantidad);

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

    boolean existsByNombreIgnoreCase(String nombre);
}
