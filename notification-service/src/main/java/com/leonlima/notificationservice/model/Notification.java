package com.leonlima.notificationservice.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications",
       uniqueConstraints = @UniqueConstraint(columnNames = "order_id", name = "uk_notifications_order_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência lógica ao pedido — sem FK para manter o desacoplamento entre serviços
    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    public enum NotificationStatus {
        SENT, FAILED
    }
}
