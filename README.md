# Microsserviços com RabbitMQ

Dois microsserviços Spring Boot independentes que se comunicam de forma assíncrona via RabbitMQ, totalmente containerizados com Docker Compose.

---

## Links rápidos

| | |
|---|---|
| Rodar tudo | `docker-compose up --build` |
| Painel RabbitMQ | `http://localhost:15672` (guest/guest) |
| order-service | `http://localhost:8081` |
| notification-service | `http://localhost:8082` |

---

## Principais competências demonstradas

- Arquitetura orientada a eventos com producer, broker e consumer
- Comunicação assíncrona e desacoplamento entre serviços
- Banco de dados por serviço (princípio fundamental de microsserviços)
- Idempotência no consumidor — reentregas do broker não geram duplicações
- Containerização completa com Docker Compose (2 apps + 2 bancos + RabbitMQ)
- Testes unitários com JUnit 5 e Mockito
- Tratamento de falha com relançamento de exceção para reprocessamento automático

---

## Arquitetura

```
┌─────────────────┐   publica evento    ┌─────────────┐   consome evento   ┌──────────────────────┐
│  order-service  │ ──────────────────► │  RabbitMQ   │ ─────────────────► │ notification-service │
│   porta: 8081   │   order.created     │  porta: 5672│                    │      porta: 8082     │
└────────┬────────┘                     └─────────────┘                    └──────────┬───────────┘
         │                                                                             │
         ▼                                                                             ▼
   ┌──────────┐                                                              ┌──────────────────┐
   │ orders_db│                                                              │ notifications_db │
   │ p.: 5433 │                                                              │    porta: 5434   │
   └──────────┘                                                              └──────────────────┘
```

---

## Fluxo completo

```
1. POST /api/orders          →  order-service recebe o pedido
2. orderRepository.save()    →  persiste no orders_db
3. OrderPublisher.publish()  →  publica OrderCreatedEvent no exchange orders.exchange
4. RabbitMQ routing          →  encaminha pela routing key order.created para a fila
5. OrderCreatedListener      →  notification-service consome o evento da fila
6. NotificationService       →  verifica idempotência e persiste no notifications_db
```

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.3 |
| Spring AMQP | 3.x |
| RabbitMQ | 3.12 |
| PostgreSQL | 15 |
| Docker + Docker Compose | — |
| JUnit 5 + Mockito | — |

---

## Pré-requisito

Docker Desktop instalado e em execução.

---

## Execução

```bash
docker-compose up --build
```

Aguarde cerca de 30 segundos para todos os serviços inicializarem.

```bash
# Parar os serviços
docker-compose down

# Parar e remover volumes (apaga dados dos bancos)
docker-compose down -v
```

---

## Contrato do evento

Ao criar um pedido, o `order-service` publica o seguinte evento na fila `order.created`:

```json
{
  "orderId": 1,
  "customerEmail": "leon@email.com",
  "productName": "Notebook Dell",
  "quantity": 1,
  "totalPrice": 3500.00,
  "createdAt": "2025-01-01T10:00:00"
}
```

O `notification-service` consome este payload e persiste a notificação. A classe `OrderCreatedEvent` é definida independentemente em cada serviço — sem biblioteca compartilhada — para manter o desacoplamento.

---

## Garantias de entrega

### Idempotência

O `notification-service` verifica se já existe uma notificação para o `orderId` antes de persistir. Reentregas do RabbitMQ — em caso de retry ou requeue — não geram registros duplicados.

```java
Optional<Notification> existing = notificationRepository.findByOrderId(event.getOrderId());
if (existing.isPresent()) {
    return NotificationDTO.fromEntity(existing.get()); // evento ignorado
}
```

A coluna `order_id` na tabela `notifications` tem constraint `UNIQUE` como segunda camada de proteção.

### Reprocessamento automático

Se o `notification-service` lançar exceção ao processar um evento, a exceção é relançada pelo listener. O Spring AMQP recoloca a mensagem na fila automaticamente para reprocessamento:

```java
} catch (Exception e) {
    log.error("Falha ao processar evento do pedido id={}: {}", event.getOrderId(), e.getMessage(), e);
    throw e; // requeue automático pelo Spring AMQP
}
```

### Melhorias previstas para produção

- **Dead Letter Queue (DLQ)**: mensagens que falharem repetidamente são movidas para uma fila separada, evitando loop infinito de retry
- **Ack manual**: confirmar o processamento explicitamente ao broker somente após persistir com sucesso
- **Retry com backoff**: tentativas com intervalo crescente para não sobrecarregar o banco em falhas temporárias

---

## Versionamento de eventos

Hoje o contrato `OrderCreatedEvent` é implícito. Em produção, eventos evoluem — campos são adicionados, renomeados ou removidos. Estratégias comuns:

- Incluir campo `eventVersion` no payload (`"eventVersion": "v1"`)
- Manter compatibilidade retroativa (novos campos opcionais, nunca remover)
- Usar schema registry (ex: Confluent Schema Registry com Avro) para contrato explícito e validado

---

## Observabilidade

Implementado: logs estruturados com Slf4j em todos os pontos críticos do fluxo (publicação, consumo, idempotência).

Melhorias previstas:
- **Correlation ID**: propagar um identificador único da requisição original por todos os serviços para rastrear o fluxo completo nos logs
- **Distributed tracing**: integração com OpenTelemetry + Jaeger ou Zipkin para visualização do fluxo entre serviços
- **Métricas**: exposição via Micrometer + Prometheus para monitoramento de taxa de mensagens, latência e falhas

---

## Endpoints

### order-service — http://localhost:8081

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/orders` | Criar pedido e publicar evento |
| GET | `/api/orders` | Listar todos os pedidos |
| GET | `/api/orders/{id}` | Buscar pedido por ID |
| GET | `/api/orders/customer/{email}` | Pedidos por cliente |

### notification-service — http://localhost:8082

| Método | Rota | Descrição |
|--------|------|-----------|
| GET | `/api/notifications` | Listar todas as notificações |
| GET | `/api/notifications/order/{orderId}` | Notificação por pedido |
| GET | `/api/notifications/customer/{email}` | Notificações por cliente |

---

## Testando o fluxo

### 1. Criar pedido

```bash
curl -X POST http://localhost:8081/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerEmail": "leon@email.com",
    "productName": "Notebook Dell",
    "quantity": 1,
    "totalPrice": 3500.00
  }'
```

```json
{
  "id": 1,
  "customerEmail": "leon@email.com",
  "productName": "Notebook Dell",
  "quantity": 1,
  "totalPrice": 3500.00,
  "status": "PENDING",
  "createdAt": "2025-01-01T10:00:00"
}
```

### 2. Verificar notificação gerada automaticamente

```bash
curl http://localhost:8082/api/notifications/order/1
```

```json
{
  "id": 1,
  "orderId": 1,
  "customerEmail": "leon@email.com",
  "productName": "Notebook Dell",
  "quantity": 1,
  "totalPrice": 3500.00,
  "status": "SENT",
  "processedAt": "2025-01-01T10:00:01"
}
```

A notificação foi criada pelo `notification-service` ao consumir o evento da fila — sem nenhuma chamada HTTP direta entre os serviços.

### 3. Verificar no painel do RabbitMQ

Acesse `http://localhost:15672` (guest/guest) para visualizar a fila `order.created`, taxa de mensagens e histórico de entregas.

---

## Testes

```bash
cd order-service && mvn test
cd notification-service && mvn test
```

---

## Estrutura

```
microservices/
├── docker-compose.yml
├── order-service/
│   ├── Dockerfile
│   └── src/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── model/
│       ├── dto/
│       ├── messaging/     ← RabbitMQConfig, OrderPublisher, OrderCreatedEvent
│       └── exception/
└── notification-service/
    ├── Dockerfile
    └── src/
        ├── listener/      ← OrderCreatedListener, RabbitMQConfig, NotificationController
        ├── service/       ← idempotência implementada aqui
        ├── repository/
        ├── model/         ← constraint UNIQUE em order_id
        ├── dto/
        └── exception/
```

---

## Autor

**LNL**
GitHub: [@leonlimask20-dot](https://github.com/leonlimask20-dot)
Email: leonlimask@gmail.com
