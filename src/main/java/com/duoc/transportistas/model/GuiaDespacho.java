package com.duoc.transportistas.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "guias_despacho")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroGuia;

    @Column(nullable = false)
    private String transportista;

    @Column(nullable = false)
    private LocalDate fechaEmision;

    // Ruta donde se guarda temporalmente en el volumen EFS
    private String rutaTemporalEfs;

    // URL o ruta final de almacenamiento en AWS S3 (/ano/transportista/...)
    private String urlS3;

    private String estado; // Ejemplo: "GENERADA", "SUBIDA_S3", "ERROR"
}