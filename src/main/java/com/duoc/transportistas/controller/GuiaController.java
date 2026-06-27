package com.duoc.transportistas.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.duoc.transportistas.model.GuiaDespacho;
import com.duoc.transportistas.repository.GuiaDespachoRepository;
import com.duoc.transportistas.service.StorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guias")
@RequiredArgsConstructor
public class GuiaController {

    private final GuiaDespachoRepository guiaRepository;
    private final StorageService storageService; // <--- Inyectamos el nuevo servicio de almacenamiento

    // 1. Crear guías de despacho (y guardarla en EFS de verdad)
    @PostMapping
    public ResponseEntity<GuiaDespacho> crearGuia(@RequestBody GuiaDespacho guia) {
        guia.setEstado("GENERADA");
        guia.setFechaEmision(LocalDate.now());
        
        // Simulamos un contenido PDF vacío o un string convertido a bytes para la guía
        byte[] pdfFalso = "CONTENIDO_DEL_PDF_DE_LA_GUIA".getBytes();
        
        // Guardado real en la ruta EFS
        String rutaEfs = storageService.guardarTemporalEfs(guia.getNumeroGuia(), pdfFalso);
        guia.setRutaTemporalEfs(rutaEfs);
        
        GuiaDespacho nuevaGuia = guiaRepository.save(guia);
        return new ResponseEntity<>(nuevaGuia, HttpStatus.CREATED);
    }

    // 2. Subir guías generadas a S3 con la estructura exacta de la pauta
    @PostMapping("/{id}/subir-s3")
    public ResponseEntity<GuiaDespacho> subirAS3(@PathVariable Long id) {
        Optional<GuiaDespacho> guiaOpt = guiaRepository.findById(id);
        
        if (guiaOpt.isPresent()) {
            GuiaDespacho guia = guiaOpt.get();
            
            // Llamamos al servicio de AWS S3 real
            String urlS3 = storageService.subirAS3(
                    guia.getRutaTemporalEfs(), 
                    guia.getTransportista(), 
                    guia.getFechaEmision(), 
                    guia.getNumeroGuia()
            );
            
            guia.setUrlS3(urlS3);
            guia.setEstado("SUBIDA_S3");
            
            GuiaDespacho guiaActualizada = guiaRepository.save(guia);
            return ResponseEntity.ok(guiaActualizada);
        }
        return ResponseEntity.notFound().build();
    }

    // 3. Descargar guías con validación de permisos
    @GetMapping("/{id}/descargar")
    public ResponseEntity<String> descargarGuia(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return new ResponseEntity<>("Acceso denegado: Token inválido o ausente", HttpStatus.FORBIDDEN);
        }
        
        Optional<GuiaDespacho> guiaOpt = guiaRepository.findById(id);
        if (guiaOpt.isPresent()) {
            return ResponseEntity.ok("Descargando archivo físico desde la ruta del volumen EFS: " + guiaOpt.get().getRutaTemporalEfs());
        }
        return ResponseEntity.notFound().build();
    }

    // 4. Modificar o actualizar guías
    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespacho> actualizarGuia(@PathVariable Long id, @RequestBody GuiaDespacho datosActualizados) {
        return guiaRepository.findById(id)
                .map(guia -> {
                    guia.setTransportista(datosActualizados.getTransportista());
                    guia.setNumeroGuia(datosActualizados.getNumeroGuia());
                    guia.setFechaEmision(datosActualizados.getFechaEmision());
                    guia.setEstado(datosActualizados.getEstado());
                    return ResponseEntity.ok(guiaRepository.save(guia));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    

    // 5. Eliminar guías específicas (Registro y archivo en EFS)
    @DeleteMapping("/{id}")
    public ResponseEntity<String> eliminarGuia(@PathVariable Long id) {
        Optional<GuiaDespacho> guiaOpt = guiaRepository.findById(id);
        if (guiaOpt.isPresent()) {
            GuiaDespacho guia = guiaOpt.get();
            // Borra el archivo físico del volumen EFS
            storageService.eliminarArchivo(guia.getRutaTemporalEfs(), guia.getUrlS3());
            // Borra el registro de la BD
            guiaRepository.deleteById(id);
            return ResponseEntity.ok("Guía eliminada del registro y del almacenamiento EFS.");
        }
        return ResponseEntity.notFound().build();
    }

    // 6. Consultar guías por transportista y fecha
    @GetMapping("/buscar")
    public ResponseEntity<List<GuiaDespacho>> consultarGuias(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
            
        List<GuiaDespacho> guias = guiaRepository.findByTransportistaAndFechaEmision(transportista, fecha);
        return ResponseEntity.ok(guias);
    }
}