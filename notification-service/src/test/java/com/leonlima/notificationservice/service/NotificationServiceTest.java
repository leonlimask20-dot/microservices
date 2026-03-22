package com.leonlima.notificationservice.service;

import com.leonlima.notificationservice.dto.NotificationDTO;
import com.leonlima.notificationservice.dto.OrderCreatedEvent;
import com.leonlima.notificationservice.model.Notification;
import com.leonlima.notificationservice.repository.NotificationRepository;
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
@DisplayName("NotificationService — testes unitários")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private OrderCreatedEvent sampleEvent;
    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleEvent = OrderCreatedEvent.builder()
                .orderId(1L).customerEmail("leon@email.com")
                .productName("Notebook Dell").quantity(1)
                .totalPrice(new BigDecimal("3500.00"))
                .createdAt(LocalDateTime.now()).build();

        sampleNotification = Notification.builder()
                .id(1L).orderId(1L).customerEmail("leon@email.com")
                .productName("Notebook Dell").quantity(1)
                .totalPrice(new BigDecimal("3500.00"))
                .status(Notification.NotificationStatus.SENT)
                .processedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("Deve retornar notificação existente sem duplicar ao receber evento repetido")
    void processOrderCreated_duplicateEvent_returnsExistingWithoutDuplicate() {
        // Simula uma segunda entrega do mesmo evento pelo RabbitMQ
        when(notificationRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleNotification));

        NotificationDTO result = notificationService.processOrderCreated(sampleEvent);

        assertThat(result.getOrderId()).isEqualTo(1L);
        // Garante que o save NÃO foi chamado — evento duplicado não gera novo registro
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve salvar notificação com status SENT ao processar evento")
    void processOrderCreated_success_savesNotification() {
        // Primeira entrega: não existe notificação para este pedido ainda
        when(notificationRepository.findByOrderId(1L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(sampleNotification);

        NotificationDTO result = notificationService.processOrderCreated(sampleEvent);

        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("SENT");

        // Confirma que os dados do evento foram mapeados corretamente para a entidade
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerEmail()).isEqualTo("leon@email.com");
        assertThat(captor.getValue().getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
    }

    @Test
    @DisplayName("Deve retornar todas as notificações")
    void getAllNotifications_returnsAll() {
        when(notificationRepository.findAll()).thenReturn(List.of(sampleNotification));
        assertThat(notificationService.getAllNotifications()).hasSize(1);
        verify(notificationRepository).findAll();
    }

    @Test
    @DisplayName("Deve retornar notificação pelo ID do pedido")
    void getByOrderId_exists_returnsNotification() {
        when(notificationRepository.findByOrderId(1L)).thenReturn(Optional.of(sampleNotification));
        assertThat(notificationService.getByOrderId(1L).getOrderId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Deve lançar EntityNotFoundException para pedido sem notificação")
    void getByOrderId_notFound_throwsException() {
        when(notificationRepository.findByOrderId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.getByOrderId(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("Deve filtrar notificações pelo email do cliente")
    void getByCustomerEmail_returnsFiltered() {
        when(notificationRepository.findByCustomerEmail("leon@email.com"))
                .thenReturn(List.of(sampleNotification));
        assertThat(notificationService.getByCustomerEmail("leon@email.com")).hasSize(1);
    }
}
