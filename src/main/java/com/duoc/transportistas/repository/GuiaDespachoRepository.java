package com.duoc.transportistas.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.duoc.transportistas.model.GuiaDespacho;

@Repository
public interface GuiaDespachoRepository extends JpaRepository<GuiaDespacho, Long> {

    // Consulta personalizada para filtrar guías por transportista y fecha exacta
    List<GuiaDespacho> findByTransportistaAndFechaEmision(String transportista, LocalDate fechaEmision);
}