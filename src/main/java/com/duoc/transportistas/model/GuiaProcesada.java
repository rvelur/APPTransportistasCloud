package com.duoc.transportistas.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import java.time.LocalDate;

@Entity
@Table(name = "GUIAS_PROCESADAS")
@Getter
@Setter
@ToString
public class GuiaProcesada {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    private String numeroGuia;
    private String transportista;
    private LocalDate fechaEmision;
    private String estado;
    private String urlS3;
    private String rutaTemporalEfs;
}