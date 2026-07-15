package com.duoc.transportistas.controller;

import com.duoc.transportistas.model.GuiaDespacho;
import com.duoc.transportistas.service.ProductorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productor")
public class ProductorController {

    @Autowired
    private ProductorService productorService;

    @PostMapping("/enviar")
    public ResponseEntity<String> enviarGuiaALaCola(@RequestBody GuiaDespacho guia) {
        productorService.enviarGuia(guia);
        return ResponseEntity.ok("Guía N° " + guia.getNumeroGuia() + " enviada exitosamente a la cola 1.");
    }
}