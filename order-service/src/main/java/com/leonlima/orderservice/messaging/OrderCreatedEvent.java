package com.leonlima.orderservice.messaging;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento publicado no RabbitMQ após criação de um pedido.
 * Carrega apenas os dados necessários para o consumidor processar — sem dependência da entidade Order.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String customerEmail;
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
}
