package com.pieroalonso.panaderia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pieroalonso.panaderia.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombre(String nombre);
}
