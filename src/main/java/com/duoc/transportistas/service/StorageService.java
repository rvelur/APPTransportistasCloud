package com.duoc.transportistas.service;

import java.time.LocalDate;

public interface StorageService {
    // Guarda el archivo temporalmente en el volumen EFS montado
    String guardarTemporalEfs(String numeroGuia, byte[] contenidoPdf);
    
    // Sube el archivo desde EFS a S3 en la ruta: /ano/transportista/guia.pdf
    String subirAS3(String rutaEfs, String transportista, LocalDate fecha, String numeroGuia);
    
    // Elimina el archivo de EFS y S3 si es necesario
    void eliminarArchivo(String rutaEfs, String urlS3);
}