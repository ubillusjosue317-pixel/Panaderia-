package com.utp.panaderia.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utp.panaderia.model.Auditoria;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
}
