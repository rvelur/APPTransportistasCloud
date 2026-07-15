package com.duoc.transportistas.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "guias_despacho")
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

    private String rutaTemporalEfs;
    private String urlS3;
    private String estado; 

    // Constructores
    public GuiaDespacho() {}

    public GuiaDespacho(Long id, String numeroGuia, String transportista, LocalDate fechaEmision, String rutaTemporalEfs, String urlS3, String estado) {
        this.id = id;
        this.numeroGuia = numeroGuia;
        this.transportista = transportista;
        this.fechaEmision = fechaEmision;
        this.rutaTemporalEfs = rutaTemporalEfs;
        this.urlS3 = urlS3;
        this.estado = estado;
    }

    // GETTERS Y SETTERS MANUALES (Para evitar errores de VS Code)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroGuia() { return numeroGuia; }
    public void setNumeroGuia(String numeroGuia) { this.numeroGuia = numeroGuia; }

    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }

    public LocalDate getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDate fechaEmision) { this.fechaEmision = fechaEmision; }

    public String getRutaTemporalEfs() { return rutaTemporalEfs; }
    public void setRutaTemporalEfs(String rutaTemporalEfs) { this.rutaTemporalEfs = rutaTemporalEfs; }

    public String getUrlS3() { return urlS3; }
    public void setUrlS3(String urlS3) { this.urlS3 = urlS3; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}