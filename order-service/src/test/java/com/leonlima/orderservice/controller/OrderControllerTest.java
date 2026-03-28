package com.leonlima.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonlima.orderservice.dto.OrderDTO;
import com.leonlima.orderservice.service.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController - testes MockMvc")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderDTO.Response pedido() {
        return OrderDTO.Response.builder()
            .id(1L)
            .customerEmail("leon@email.com")
            .productName("Notebook Dell")
            .quantity(1)
            .totalPrice(new BigDecimal("3500.00"))
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();
    }

    @Test
    @DisplayName("POST /api/orders - deve criar pedido e retornar 201")
    void createOrder_dadosValidos_retorna201() throws Exception {
        OrderDTO.CreateRequest req = new OrderDTO.CreateRequest();
        req.setCustomerEmail("leon@email.com");
        req.setProductName("Notebook Dell");
        req.setQuantity(1);
        req.setTotalPrice(new BigDecimal("3500.00"));

        when(orderService.createOrder(any())).thenReturn(pedido());

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/orders - deve retornar 400 quando dados invalidos")
    void createOrder_dadosInvalidos_retorna400() throws Exception {
        OrderDTO.CreateRequest req = new OrderDTO.CreateRequest();
        req.setCustomerEmail("email-invalido");
        req.setProductName("");
        req.setQuantity(0);
        req.setTotalPrice(new BigDecimal("0.00"));

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/orders/{id} - deve retornar pedido existente")
    void getOrder_existente_retornaPedido() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(pedido());

        mockMvc.perform(get("/api/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.productName").value("Notebook Dell"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} - deve retornar 404 quando nao encontrado")
    void getOrder_naoEncontrado_retorna404() throws Exception {
        when(orderService.getOrderById(99L))
            .thenThrow(new EntityNotFoundException("Pedido nao encontrado"));

        mockMvc.perform(get("/api/orders/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/orders - deve listar todos os pedidos")
    void getAllOrders_retornaLista() throws Exception {
        when(orderService.getAllOrders()).thenReturn(List.of(pedido()));

        mockMvc.perform(get("/api/orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].customerEmail").value("leon@email.com"));
    }

    @Test
    @DisplayName("GET /api/orders/customer/{email} - deve filtrar por email do cliente")
    void getByCustomer_retornaFiltrado() throws Exception {
        when(orderService.getOrdersByCustomer("leon@email.com"))
            .thenReturn(List.of(pedido()));

        mockMvc.perform(get("/api/orders/customer/leon@email.com"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].customerEmail").value("leon@email.com"));
    }
}
