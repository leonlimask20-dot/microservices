package com.leonlima.orderservice.service;

import com.leonlima.orderservice.dto.OrderDTO;
import com.leonlima.orderservice.messaging.OrderCreatedEvent;
import com.leonlima.orderservice.messaging.OrderPublisher;
import com.leonlima.orderservice.model.Order;
import com.leonlima.orderservice.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService — testes unitários")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderPublisher orderPublisher;

    @InjectMocks
    private OrderService orderService;

    private Order sampleOrder;
    private OrderDTO.CreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleOrder = Order.builder()
                .id(1L).customerEmail("leon@email.com")
                .productName("Notebook Dell").quantity(1)
                .totalPrice(new BigDecimal("3500.00"))
                .status(Order.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now()).build();

        createRequest = new OrderDTO.CreateRequest();
        createRequest.setCustomerEmail("leon@email.com");
        createRequest.setProductName("Notebook Dell");
        createRequest.setQuantity(1);
        createRequest.setTotalPrice(new BigDecimal("3500.00"));
    }

    @Test
    @DisplayName("Deve persistir o pedido e publicar o evento com os dados corretos")
    void createOrder_success_persistsAndPublishesEvent() {
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);

        OrderDTO.Response response = orderService.createOrder(createRequest);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("PENDING");

        // Verifica que o evento publicado reflete os dados do pedido salvo
        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderPublisher).publishOrderCreated(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(1L);
        assertThat(captor.getValue().getCustomerEmail()).isEqualTo("leon@email.com");
    }

    @Test
    @DisplayName("Deve retornar pedido existente pelo ID")
    void getOrderById_exists_returnsOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(sampleOrder));
        assertThat(orderService.getOrderById(1L).getProductName()).isEqualTo("Notebook Dell");
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException para ID inexistente")
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.getOrderById(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Deve retornar pedidos filtrados pelo email do cliente")
    void getOrdersByCustomer_returnsFilteredOrders() {
        when(orderRepository.findByCustomerEmail("leon@email.com")).thenReturn(List.of(sampleOrder));
        assertThat(orderService.getOrdersByCustomer("leon@email.com")).hasSize(1);
    }

    @Test
    @DisplayName("Deve retornar todos os pedidos")
    void getAllOrders_returnsAllOrders() {
        when(orderRepository.findAll()).thenReturn(List.of(sampleOrder));
        assertThat(orderService.getAllOrders()).hasSize(1);
        verify(orderRepository).findAll();
    }
}
