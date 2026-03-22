package com.leonlima.notificationservice.listener;

import com.leonlima.notificationservice.dto.NotificationDTO;
import com.leonlima.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<NotificationDTO> getByOrderId(@PathVariable Long orderId) {
        return ResponseEntity.ok(notificationService.getByOrderId(orderId));
    }

    @GetMapping("/customer/{email}")
    public ResponseEntity<List<NotificationDTO>> getByCustomer(@PathVariable String email) {
        return ResponseEntity.ok(notificationService.getByCustomerEmail(email));
    }
}
