package com.duoc.transportistas.service;

import com.duoc.transportistas.config.RabbitMQConfig;
import com.duoc.transportistas.model.GuiaDespacho;
import com.duoc.transportistas.model.GuiaProcesada;
import com.duoc.transportistas.repository.GuiaProcesadaRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConsumidorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private GuiaProcesadaRepository guiaProcesadaRepository;

    /**
     * Consume manualmente todos los mensajes acumulados en la Cola 1.
     * Si el guardado falla, redirige de forma segura el mensaje a la Cola 2.
     */
    public List<String> consumirYProcesarCola1() {
        List<String> logs = new ArrayList<>();
        boolean hayMensajes = true;

        while (hayMensajes) {
            GuiaDespacho guiaMsg = (GuiaDespacho) rabbitTemplate.receiveAndConvert(RabbitMQConfig.COLA_1);

            if (guiaMsg == null) {
                hayMensajes = false;
                if (logs.isEmpty()) {
                    logs.add("No se encontraron guías pendientes por procesar en la cola 1.");
                }
                break;
            }

            try {
                // Mapeo hacia la nueva entidad de la base de datos
                GuiaProcesada procesada = new GuiaProcesada();
                procesada.setNumeroGuia(guiaMsg.getNumeroGuia());
                procesada.setTransportista(guiaMsg.getTransportista());
                procesada.setFechaEmision(guiaMsg.getFechaEmision());
                procesada.setEstado("PROCESADA_ASINC_OK");
                procesada.setUrlS3(guiaMsg.getUrlS3());
                procesada.setRutaTemporalEfs(guiaMsg.getRutaTemporalEfs());

                // Guardar en Oracle Cloud (Nueva Tabla)
                guiaProcesadaRepository.save(procesada);
                logs.add("Guía N° " + guiaMsg.getNumeroGuia() + " procesada y persistida con éxito en Oracle.");
                
            } catch (Exception e) {
                logs.add("ERROR al persistir Guía N° " + guiaMsg.getNumeroGuia() + ": " + e.getMessage() + ". Redirigiendo a cola 2.");
                
                // Enviar a Cola 2 (Errores) a través del Exchange DLX
                rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_DLX, 
                    RabbitMQConfig.ROUTING_KEY_DLX, 
                    guiaMsg
                );
            }
        }

        return logs;
    }
}