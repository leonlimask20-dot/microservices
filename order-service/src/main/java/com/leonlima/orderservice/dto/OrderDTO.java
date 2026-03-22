package com.leonlima.orderservice.dto;

import com.leonlima.orderservice.model.Order;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDTO {

    @Data
    public static class CreateRequest {

        @NotBlank(message = "Email do cliente é obrigatório")
        @Email(message = "Formato de email inválido")
        private String customerEmail;

        @NotBlank(message = "Nome do produto é obrigatório")
        private String productName;

        @NotNull(message = "Quantidade é obrigatória")
        @Min(value = 1, message = "Quantidade mínima é 1")
        private Integer quantity;

        @NotNull(message = "Preço total é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        private BigDecimal totalPrice;
    }

    @Data
    @Builder
    public static class Response {
        private Long id;
        private String customerEmail;
        private String productName;
        private Integer quantity;
        private BigDecimal totalPrice;
        private String status;
        private LocalDateTime createdAt;

        public static Response fromEntity(Order order) {
            return Response.builder()
                    .id(order.getId())
                    .customerEmail(order.getCustomerEmail())
                    .productName(order.getProductName())
                    .quantity(order.getQuantity())
                    .totalPrice(order.getTotalPrice())
                    .status(order.getStatus().name())
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }
}
