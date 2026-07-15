package com.duoc.transportistas.repository;

import com.duoc.transportistas.model.GuiaProcesada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuiaProcesadaRepository extends JpaRepository<GuiaProcesada, Long> {
}