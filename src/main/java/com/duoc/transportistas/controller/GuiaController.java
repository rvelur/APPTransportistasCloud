package com.duoc.transportistas.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
import org.springframework.beans.factory.annotation.Autowired;

import com.duoc.transportistas.config.RabbitMQConfig;
import com.duoc.transportistas.model.GuiaDespacho;
import com.duoc.transportistas.repository.GuiaDespachoRepository;
import com.duoc.transportistas.service.StorageService;
import com.duoc.transportistas.service.ConsumidorService;
import com.duoc.transportistas.repository.GuiaProcesadaRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/guias")
public class GuiaController {

    @Autowired
    private GuiaDespachoRepository guiaRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConsumidorService consumidorService;

    @Autowired
    private GuiaProcesadaRepository guiaProcesadaRepository; // <-- Importante tenerla declarada aquí también

    // 1. Crear guías de despacho (Modificado: Ahora envía los datos a la Cola 1)
    @PostMapping
    public ResponseEntity<String> crearGuia(@RequestBody GuiaDespacho guia) {
        System.out.println(">>> [CI/CD OK] Procesando la creación de una nueva guía en local...");
        
        guia.setEstado("GENERADA");
        guia.setFechaEmision(LocalDate.now());
        
        // Simulamos un contenido PDF vacío o un string convertido a bytes para la guía
        byte[] pdfFalso = "CONTENIDO_DEL_PDF_DE_LA_GUIA".getBytes();
        
        // Guardado real en la ruta EFS
        String rutaEfs = storageService.guardarTemporalEfs(guia.getNumeroGuia(), pdfFalso);
        guia.setRutaTemporalEfs(rutaEfs);
        
        try {
            // Enviar la GuiaDespacho directamente como JSON a la Cola 1
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PRINCIPAL, 
                RabbitMQConfig.ROUTING_KEY_1, 
                guia
            );
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body("Guía de despacho N° " + guia.getNumeroGuia() + " enviada a la cola 1 con éxito para procesamiento asíncrono.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar la guía a la cola 1: " + e.getMessage());
        }
    }

    // NEW: Endpoint adicional para consumir mensajes de la Cola 1 bajo demanda
    // Los procesa y los guarda en la base de datos Oracle en la NUEVA TABLA (guias_procesadas)
    @PostMapping("/consumir")
    public ResponseEntity<List<String>> consumirYProcesarGuias() {
        List<String> logsProcesamiento = consumidorService.consumirYProcesarCola1();
        return ResponseEntity.ok(logsProcesamiento);
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