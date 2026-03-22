package com.leonlima.notificationservice.listener;

import com.leonlima.notificationservice.dto.OrderCreatedEvent;
import com.leonlima.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consome mensagens da fila order.created.
 * A exceção é relançada intencionalmente para que o Spring AMQP
 * recoloque a mensagem na fila em caso de falha no processamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final NotificationService notificationService;

    @RabbitListener(queues = "${rabbitmq.queue.order-created}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Evento recebido — pedido id={}", event.getOrderId());
        try {
            notificationService.processOrderCreated(event);
        } catch (Exception e) {
            log.error("Falha ao processar evento do pedido id={}: {}", event.getOrderId(), e.getMessage(), e);
            throw e;
        }
    }
}
