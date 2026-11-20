package com.utp.panaderia.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utp.panaderia.model.TokenRecuperacion;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

    // Marca el token como usado en UNA sola instruccion: si dos personas lo usan a la vez,
    // solo una consigue que devuelva 1 (un solo uso garantizado)
    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usado = true where t.id = :id and t.usado = false")
    int marcarUsado(@Param("id") Long id);

    @Modifying(flushAutomatically = true)
    @Query("update TokenRecuperacion t set t.usado = true where t.usuario.id = :idUsuario and t.usado = false")
    int invalidarPendientes(@Param("idUsuario") Integer idUsuario);
}
