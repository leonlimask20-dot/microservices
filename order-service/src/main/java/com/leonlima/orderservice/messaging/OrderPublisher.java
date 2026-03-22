package com.leonlima.orderservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.order-created}")
    private String routingKey;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publicando OrderCreatedEvent — pedido id={}", event.getOrderId());
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("Evento publicado com sucesso — pedido id={}", event.getOrderId());
    }
}
