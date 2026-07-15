package com.duoc.transportistas.service;

import com.duoc.transportistas.config.RabbitMQConfig;
import com.duoc.transportistas.model.GuiaDespacho;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Envía la guía de despacho completa como JSON a la Cola 1 (Principal)
     */
    public void enviarGuia(GuiaDespacho guia) {
        try {
            System.out.println(">>> [RabbitMQ] Enviando guía N° " + guia.getNumeroGuia() + " a la cola1 para procesamiento asíncrono...");
            
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PRINCIPAL, 
                RabbitMQConfig.ROUTING_KEY_1, 
                guia
            );
            
            System.out.println(">>> [RabbitMQ] Guía N° " + guia.getNumeroGuia() + " enviada con éxito.");
            
        } catch (Exception e) {
            System.err.println("❌ [RabbitMQ] ¡FALLO EL ENVÍO A COLA 1! Redirigiendo guía N° " + guia.getNumeroGuia() + " a la Cola 2 de errores...");
            System.err.println("Detalle del error: " + e.getMessage());
            
            try {
                // Si falla la cola 1, intentamos publicarlo directamente en la cola de errores (Cola 2 / DLX)
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_DLX, 
                    RabbitMQConfig.ROUTING_KEY_DLX, 
                    guia
                );
                System.out.println("⚠️ [RabbitMQ] Guía N° " + guia.getNumeroGuia() + " respaldada con éxito en la Cola 2.");
            } catch (Exception fatalError) {
                System.err.println("🚨 [RabbitMQ-CRÍTICO] No se pudo enviar a la Cola 2: " + fatalError.getMessage());
            }
        }
    }
}