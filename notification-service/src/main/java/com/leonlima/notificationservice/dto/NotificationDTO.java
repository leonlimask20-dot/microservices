package com.leonlima.notificationservice.dto;

import com.leonlima.notificationservice.model.Notification;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder
public class NotificationDTO {

    private Long id;
    private Long orderId;
    private String customerEmail;
    private String productName;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime processedAt;

    public static NotificationDTO fromEntity(Notification n) {
        return NotificationDTO.builder()
                .id(n.getId())
                .orderId(n.getOrderId())
                .customerEmail(n.getCustomerEmail())
                .productName(n.getProductName())
                .quantity(n.getQuantity())
                .totalPrice(n.getTotalPrice())
                .status(n.getStatus().name())
                .processedAt(n.getProcessedAt())
                .build();
    }
}
