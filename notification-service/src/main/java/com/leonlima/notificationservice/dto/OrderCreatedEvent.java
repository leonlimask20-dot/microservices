package com.leonlima.notificationservice.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Contrato do evento recebido do RabbitMQ.
 * Cópia intencional do evento do order-service — cada serviço define seu próprio modelo
 * para evitar acoplamento por dependência compartilhada.
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
