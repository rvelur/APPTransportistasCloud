package com.duoc.transportistas.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    // Nombres de Exchanges y Colas según la pauta de la sumativa
    public static final String EXCHANGE_PRINCIPAL = "exchange.guia1";
    public static final String COLA_1 = "cola1"; // Cola Principal
    public static final String ROUTING_KEY_1 = "routing.guia1";

    public static final String EXCHANGE_DLX = "exchange.dlx";
    public static final String COLA_2 = "cola2"; // Cola de Errores (DLQ)
    public static final String ROUTING_KEY_DLX = "routing.dlx";

    // 1. Declarar Exchange Principal (Direct)
    @Bean
    public DirectExchange exchangePrincipal() {
        return new DirectExchange(EXCHANGE_PRINCIPAL);
    }

    // 2. Declarar la COLA 1 (Con redirección automática a la COLA 2 en caso de falla)
    @Bean
    public Queue cola1() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", EXCHANGE_DLX);
        args.put("x-dead-letter-routing-key", ROUTING_KEY_DLX);
        return new Queue(COLA_1, true, false, false, args);
    }

    // 3. Vincular COLA 1 al Exchange Principal
    @Bean
    public Binding bindingCola1() {
        return BindingBuilder.bind(cola1()).to(exchangePrincipal()).with(ROUTING_KEY_1);
    }

    // 4. Declarar Exchange DLX para descartes (Cola 2)
    @Bean
    public DirectExchange exchangeDlx() {
        return new DirectExchange(EXCHANGE_DLX);
    }

    // 5. Declarar la COLA 2 (Almacena mensajes con errores)
    @Bean
    public Queue cola2() {
        return new Queue(COLA_2, true);
    }

    // 6. Vincular COLA 2 al Exchange DLX
    @Bean
    public Binding bindingCola2() {
        return BindingBuilder.bind(cola2()).to(exchangeDlx()).with(ROUTING_KEY_DLX);
    }

    // 7. Serializador JSON para objetos completos (Guías)
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}