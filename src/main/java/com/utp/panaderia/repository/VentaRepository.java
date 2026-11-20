package com.utp.panaderia.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.utp.panaderia.model.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    List<Venta> findTop200ByOrderByFechaDescIdDesc();

    @Query("select coalesce(sum(v.total), 0) from Venta v")
    BigDecimal sumaTotal();
}
