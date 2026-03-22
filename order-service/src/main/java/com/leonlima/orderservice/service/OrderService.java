package com.leonlima.orderservice.service;

import com.leonlima.orderservice.dto.OrderDTO;
import com.leonlima.orderservice.messaging.OrderCreatedEvent;
import com.leonlima.orderservice.messaging.OrderPublisher;
import com.leonlima.orderservice.model.Order;
import com.leonlima.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderPublisher orderPublisher;

    /**
     * Persiste o pedido e publica o evento em uma única transação.
     * Se a publicação falhar após o save, o @Transactional garante rollback.
     */
    @Transactional
    public OrderDTO.Response createOrder(OrderDTO.CreateRequest request) {
        log.info("Criando pedido — cliente={} produto={}", request.getCustomerEmail(), request.getProductName());

        Order order = Order.builder()
                .customerEmail(request.getCustomerEmail())
                .productName(request.getProductName())
                .quantity(request.getQuantity())
                .totalPrice(request.getTotalPrice())
                .status(Order.OrderStatus.PENDING)
                .build();

        Order saved = orderRepository.save(order);

        orderPublisher.publishOrderCreated(OrderCreatedEvent.builder()
                .orderId(saved.getId())
                .customerEmail(saved.getCustomerEmail())
                .productName(saved.getProductName())
                .quantity(saved.getQuantity())
                .totalPrice(saved.getTotalPrice())
                .createdAt(saved.getCreatedAt())
                .build());

        return OrderDTO.Response.fromEntity(saved);
    }

    public OrderDTO.Response getOrderById(Long id) {
        return orderRepository.findById(id)
                .map(OrderDTO.Response::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException("Pedido não encontrado: " + id));
    }

    public List<OrderDTO.Response> getOrdersByCustomer(String email) {
        return orderRepository.findByCustomerEmail(email).stream()
                .map(OrderDTO.Response::fromEntity).toList();
    }

    public List<OrderDTO.Response> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderDTO.Response::fromEntity).toList();
    }
}
