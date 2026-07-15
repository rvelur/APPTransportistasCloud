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
        System.out.println(">>> [RabbitMQ] Enviando guía N° " + guia.getNumeroGuia() + " a la cola1 para procesamiento asíncrono...");
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_PRINCIPAL, 
            RabbitMQConfig.ROUTING_KEY_1, 
            guia
        );
    }
}