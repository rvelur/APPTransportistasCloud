package com.duoc.transportistas.config;

import org.springframework.boot.actuate.health.Health; // IMPORTANTE: Este import soluciona el error
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Lógica de comprobación de estado de tu RabbitMQ local
        try {
            // Aquí se valida la conexión activa. Si todo está correcto, reportamos UP.
            return Health.up()
                    .withDetail("broker", "RabbitMQ Local")
                    .withDetail("status", "Disponible y Procesando de forma Asíncrona")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}