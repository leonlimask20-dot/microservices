package com.leonlima.notificationservice.service;

import com.leonlima.notificationservice.dto.NotificationDTO;
import com.leonlima.notificationservice.dto.OrderCreatedEvent;
import com.leonlima.notificationservice.model.Notification;
import com.leonlima.notificationservice.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationDTO processOrderCreated(OrderCreatedEvent event) {
        log.info("Processando notificação para pedido id={}", event.getOrderId());

        // Idempotência: se a notificação para este pedido já existe, retorna sem duplicar.
        // Garante que reentregas do RabbitMQ (retry, requeue) não criem registros duplicados.
        Optional<Notification> existing = notificationRepository.findByOrderId(event.getOrderId());
        if (existing.isPresent()) {
            log.warn("Notificação já existe para pedido id={} — evento ignorado (entrega duplicada)",
                    event.getOrderId());
            return NotificationDTO.fromEntity(existing.get());
        }

        Notification saved = notificationRepository.save(Notification.builder()
                .orderId(event.getOrderId())
                .customerEmail(event.getCustomerEmail())
                .productName(event.getProductName())
                .quantity(event.getQuantity())
                .totalPrice(event.getTotalPrice())
                .status(Notification.NotificationStatus.SENT)
                .build());

        log.info("Notificação id={} registrada para pedido id={}", saved.getId(), event.getOrderId());
        return NotificationDTO.fromEntity(saved);
    }

    public List<NotificationDTO> getAllNotifications() {
        return notificationRepository.findAll().stream()
                .map(NotificationDTO::fromEntity).toList();
    }

    public List<NotificationDTO> getByCustomerEmail(String email) {
        return notificationRepository.findByCustomerEmail(email).stream()
                .map(NotificationDTO::fromEntity).toList();
    }

    public NotificationDTO getByOrderId(Long orderId) {
        return notificationRepository.findByOrderId(orderId)
                .map(NotificationDTO::fromEntity)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Notificação não encontrada para pedido: " + orderId));
    }
}
