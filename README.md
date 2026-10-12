# Tickets Backend

Sistema backend **reactivo y orientado a eventos** para venta de entradas con picos de alta
concurrencia, sin sobreventa, procesando compras de forma **asíncrona** vía colas SQS.

Construido con **Java 25**, **Spring Boot 4**, **Spring WebFlux** (reactivo) y **AWS DynamoDB**,
siguiendo **Arquitectura Hexagonal** (Ports & Adapters) estricta.

---

## Tabla de contenidos

1. [Descripción de la solución](#descripción-de-la-solución)
2. [Stack tecnológico](#stack-tecnológico)
3. [Arquitectura](#arquitectura)
4. [Instalación y configuración](#instalación-y-configuración)
5. [Endpoints y ejemplos de uso](#endpoints-y-ejemplos-de-uso)
6. [Testing](#testing)
7. [Decisiones arquitectónicas](#decisiones-arquitectónicas)
8. [Concurrencia y consistencia](#concurrencia-y-consistencia)
9. [Asincronía y procesamiento basado en eventos](#asincronía-y-procesamiento-basado-en-eventos)
10. [Escalabilidad, resiliencia y tolerancia a fallos](#escalabilidad-resiliencia-y-tolerancia-a-fallos)
11. [Seguridad](#seguridad)
12. [Diagramas](#diagramas)
13. [Limitaciones y mejoras futuras](#limitaciones-y-mejoras-futuras)

---

## Descripción de la solución

El sistema vende entradas para eventos sin riesgo de sobreventa incluso bajo picos de
concurrencia. El flujo es:

1. **Crear evento** → genera N filas `Ticket` (inventario) + contadores O(1) en `Event`.
2. **Consultar disponibilidad** → lee contadores del evento en O(1).
3. **Crear orden** → responde `202 Accepted` inmediatamente y encola el mensaje en SQS.
4. **Worker consume SQS** → reserva atómica de N entradas (AVAILABLE→RESERVED, TTL 10 min)
   o rechaza la orden si no hay cupo.
5. **Confirmar orden** → RESERVED→SOLD, ajusta contadores.
6. **Scheduler** → libera reservas expiradas (RESERVED→AVAILABLE), marca orden EXPIRED.
7. **Cortesía** → AVAILABLE→COMPLIMENTARY, crea orden COMPLIMENTARY auditable.

### Inventario híbrido

El sistema usa un modelo de **inventario híbrido**:

- **Filas `Ticket`**: una fila por entrada con estado e identidad. Dan auditoría y permiten
  reservas atómicas por entrada.
- **Contadores en `Event`** (`available`, `reserved`, `sold`, `complimentary`): lectura O(1)
  para disponibilidad y gate anti-sobreventa.

### Topología: API + Worker

Un solo codebase hexagonal, **dos puntos de entrada**:

- **API** (`TicketsApplication`): WebFlux/Netty, expone HTTP. **Produce** mensajes a SQS.
- **Worker** (`WorkerApplication`): sin servidor web. **Consume** SQS y corre el scheduler.

Beneficio: API y Worker escalan independientemente; el worker puede caerse sin tumbar el API.

---

## Stack tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 (Virtual Threads, Records, Sealed Classes, Pattern Matching) |
| Framework | Spring Boot 4.0 |
| Web | Spring WebFlux (Netty, reactivo — `Mono`/`Flux`) |
| Persistencia | AWS DynamoDB (SDK v2 Enhanced Client) |
| Cola | AWS SQS (SDK v2) |
| Build | Maven |
| Cobertura | JaCoCo 0.8.13 (gate ≥90%) |
| Validación | Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, AssertJ, Reactor Test (`StepVerifier`) |

---

## Arquitectura

### Arquitectura hexagonal (Ports & Adapters)

Dependencias unidireccionales:

```
infraestructura → aplicación → dominio
dominio → (nada, 100% puro)
```

| Capa | Paquete base | Responsabilidad | Reglas |
|---|---|---|---|
| **Domain** | `com.tickets.domain` | Agregados, Value Objects, excepciones | Sin Spring, sin JPA, sin Reactor. Records y sealed classes. |
| **Application** | `com.tickets.application` | Puertos in/out, casos de uso | Sin anotaciones de Spring. Puede usar `Mono`/`Flux`. |
| **Infrastructure** | `com.tickets.infrastructure` | Adaptadores REST, DynamoDB, SQS, config | Aquí viven las anotaciones de Spring. |

### Estructura del proyecto

```
tickets-back/
├── pom.xml
├── Dockerfile                              # Multi-stage build (Maven+JDK25 → JRE25)
├── docker-compose.yml                      # DynamoDB Local + LocalStack SQS + API + Worker
├── src/
│   ├── main/java/com/tickets/
│   │   ├── TicketsApplication.java         # Entry point API (WebFlux)
│   │   ├── WorkerApplication.java          # Entry point Worker (SQS consumer + scheduler)
│   │   │
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── event/                  # Event, EventId
│   │   │   │   ├── order/                  # Order, OrderId, OrderStatus
│   │   │   │   └── ticket/                 # Ticket, TicketId, TicketStatus, TicketTransition
│   │   │   └── exception/                  # DomainException (sealed), Invalid*, NotFound
│   │   │
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── in/event/               # CreateEvent, GetEvent, ListEvents, GetAvailability
│   │   │   │   ├── in/order/               # CreateOrder, GetOrder, ProcessOrder, ConfirmOrder,
│   │   │   │   │                           #   GrantComplimentary, ReleaseExpiredReservations
│   │   │   │   ├── out/repositories/       # Event, Order, Ticket, TicketTransition repos
│   │   │   │   └── out/publishers/         # MessagePublisher
│   │   │   └── usecase/
│   │   │       ├── event/                  # Service implementations + EventAvailability
│   │   │       └── order/                  # Service implementations + commands
│   │   │
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/web/                 # REST controllers, DTOs, GlobalExceptionHandler
│   │       │   ├── in/worker/              # SQS consumer, handlers, scheduler
│   │       │   └── out/
│   │       │       ├── persistence/        # DynamoDB repos, entities, mappers
│   │       │       └── sqs/                # SQS producer
│   │       └── config/                     # DynamoDbConfig, SqsConfig, UseCaseConfiguration
│   └── resources/
│       └── application.yml
└── src/test/                               # 247 tests, JaCoCo ≥90%
```

---

## Instalación y configuración

### Prerrequisitos

- **Docker** + **Docker Compose** (recomendado — no requiere Java ni Maven instalados)
- O, para ejecución local: **Java 25** (OpenJDK 25+) y **Maven 3.9+**

### Ejecución con Docker Compose (recomendado)

Levanta DynamoDB Local, LocalStack (SQS), la API y el Worker con un solo comando.
El build del JAR se realiza dentro del contenedor (multi-stage build).

```bash
# Construir y arrancar todo (primera vez o tras cambios en el código)
docker compose up --build

# Arrancar en background
docker compose up --build -d

# Ver logs de la API
docker compose logs -f app

# Ver logs del Worker
docker compose logs -f worker

# Detener
docker compose down
```

Servicios expuestos:

| Servicio | URL | Descripción |
|---|---|---|
| API REST | `http://localhost:8080` | Spring Boot WebFlux |
| DynamoDB Local | `http://localhost:8000` | Persistencia (in-memory) |
| LocalStack SQS | `http://localhost:4566` | Cola de mensajes |

Las tablas (`events`, `tickets`, `orders`, `ticket_transitions`) se crean automáticamente al arrancar.

### Ejecución local sin Docker

```bash
# 1. Iniciar DynamoDB Local
docker run -p 8000:8000 amazon/dynamodb-local

# 2. Iniciar LocalStack (SQS)
docker run -p 4566:4566 localstack/localstack

# 3. Arrancar la API
mvn spring-boot:run

# 4. Arrancar el Worker (en otra terminal)
mvn spring-boot:run -Dspring-boot.run.profiles=worker
```

### Comandos Maven

```bash
mvn clean compile                    # Compilar
mvn test                             # Ejecutar tests
mvn verify                           # Tests + JaCoCo gate ≥90%
mvn spring-boot:run                  # Arrancar API
mvn spring-boot:run -Dspring-boot.run.profiles=worker  # Arrancar Worker
mvn package                          # Build del JAR (API + Worker)
```

> **Nota:** Java 25 es requerido. Establecer `JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64`
> si es necesario.

### Configuración (`application.yml`)

```yaml
aws:
  dynamodb:
    region: us-east-1
    endpoint: http://localhost:8000      # DynamoDB Local; quitar en producción
    table-name: tickets
    events-table-name: events
    orders-table-name: orders
    transitions-table-name: ticket_transitions
  sqs:
    region: us-east-1
    endpoint: http://localhost:4566      # LocalStack; quitar en producción
    queue-name: order-created-queue
```

---

## Endpoints y ejemplos de uso

### Endpoints disponibles

| Método | Ruta | Descripción | Status |
|--------|------|-------------|--------|
| `POST` | `/api/events` | Crear evento (genera N entradas) | 201 |
| `GET` | `/api/events/{id}` | Obtener evento | 200 / 404 |
| `GET` | `/api/events` | Listar eventos | 200 |
| `GET` | `/api/events/{id}/availability` | Disponibilidad en tiempo real | 200 / 404 |
| `POST` | `/api/events/{id}/complimentary` | Conceder entradas de cortesía | 201 |
| `POST` | `/api/orders` | Iniciar compra (encola en SQS) | 202 |
| `GET` | `/api/orders/{id}` | Consultar estado de orden | 200 / 404 |
| `POST` | `/api/orders/{id}/confirm` | Confirmar compra → SOLD | 200 |

### Ejemplos

```bash
# 1. Crear evento con 500 entradas
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Concierto Rock",
    "date": "2026-12-15T19:30:00",
    "place": "Estadio Nacional",
    "totalCapacity": 500
  }'

# Respuesta (201):
# {
#   "id": "uuid",
#   "name": "Concierto Rock",
#   "totalCapacity": 500,
#   "available": 500, "reserved": 0, "sold": 0, "complimentary": 0
# }

# 2. Consultar disponibilidad
curl http://localhost:8080/api/events/{eventId}/availability
# { "totalCapacity": 500, "available": 500, "reserved": 0, "sold": 0, "complimentary": 0 }

# 3. Crear orden (202 — async, encola en SQS)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "{eventId}",
    "customerId": "customer-123",
    "quantity": 3
  }'
# Respuesta (202): { "id": "orderId", "status": "PENDING" }

# 4. Consultar estado de la orden (tras procesamiento del Worker)
curl http://localhost:8080/api/orders/{orderId}
# { "id": "orderId", "status": "RESERVED", "quantity": 3, "ticketIds": [...] }

# 5. Confirmar compra
curl -X POST http://localhost:8080/api/orders/{orderId}/confirm
# { "id": "orderId", "status": "SOLD" }

# 6. Conceder entradas de cortesía
curl -X POST http://localhost:8080/api/events/{eventId}/complimentary \
  -H "Content-Type: application/json" \
  -d '{ "customerId": "vip-guest", "quantity": 2 }'
# Respuesta (201): Order COMPLIMENTARY
```

---

## Testing

### Cobertura y frameworks

- **247 tests**, 0 failures, **JaCoCo ≥90%** line coverage (gate en `mvn verify`)
- **JUnit 5** + **Mockito** (mocks de repositorios) + **AssertJ** + **Reactor Test** (`StepVerifier`)

### Tipos de test

| Tipo | Descripción | Ejemplo |
|------|-------------|---------|
| **Dominio** | Modelos, value objects, máquinas de estados | `TicketTest`, `OrderTest`, `EventTest` |
| **Casos de uso** | Mocks de repositorios, `StepVerifier` reactivo | `ProcessOrderServiceTest`, `ConfirmOrderServiceTest` |
| **Controllers WebFlux** | `WebTestClient`, validación de DTOs, status codes | `EventControllerTest`, `OrderControllerTest` |
| **Persistencia** | Mocks de `DynamoDbTable`, `DynamoDbEnhancedClient` | `TicketDynamoDbRepositoryTest` |
| **Concurrencia** | 100 órdenes concurrentes, 10 entradas → sin sobreventa | `ProcessOrderConcurrencyTest` |
| **Integración E2E** | Flujo availability → order → reserve → confirm → SOLD | `TicketLifecycleIntegrationTest` |

### Test de concurrencia (el decisivo)

```java
// 100 órdenes concurrentes para 10 entradas → exactamente 10 RESERVED, 90 REJECTED
// In-memory repository simula atomicidad de DynamoDB (claim atómico en findAvailableByEvent)
// Verifica: sin sobreventa, contadores cuadran
```

---

## Decisiones arquitectónicas

### 1. Arquitectura hexagonal (Ports & Adapters)

**Por qué:** separa el núcleo de dominio de la infraestructura. Los casos de uso no conocen
Spring ni DynamoDB. Cambiar DynamoDB por PostgreSQL o SQS por Kafka no toca el dominio.

**Cómo:** los puertos son interfaces en `application.port.in` (entrada) y
`application.port.out` (salida). Los adaptadores en `infrastructure` las implementan.
Los casos de uso se registran como `@Bean` en `UseCaseConfiguration` — **sin `@Service`**
en la capa de aplicación.

### 2. Inventario híbrido (filas + contadores)

**Por qué:** las filas `Ticket` dan estado e identidad por entrada (auditoría, reservas
individuales). Los contadores en `Event` dan lectura O(1) de disponibilidad y un gate
anti-sobreventa sin escanear todas las filas.

**Cómo:** al crear un evento con `totalCapacity=N`, se generan N filas `AVAILABLE` vía
`BatchWriteItem` (lotes de 25) y se inicializan los contadores (`available=N`).

### 3. Topología API + Worker

**Por qué:** el pico de tráfico HTTP no debe competir con el procesamiento pesado de reservas.
El worker puede caerse/reiniciarse sin tumbar el API. Escalan independientemente.

**Cómo:** dos `@SpringBootApplication` (`TicketsApplication`, `WorkerApplication`) con perfiles
Spring (`api` / `worker`). El worker usa `@Profile("worker")` para activar el consumidor SQS
y el scheduler.

### 4. Reactivo (WebFlux + Reactor)

**Por qué:** el sistema maneja picos de concurrencia. WebFlux con Netty usa un event loop
no bloqueante. Las operaciones bloqueantes de DynamoDB se envuelven en
`Mono.fromCallable().subscribeOn(boundedElastic())` para no bloquear el event loop.

### 5. Sealed exceptions + pattern matching

**Por qué:** el `GlobalExceptionHandler` usa `switch` sobre una jerarquía `sealed` de
excepciones de dominio. El compilador verifica exhaustividad — añadir una nueva excepción
fuerza a manejarla.

### 6. Auditoría de transiciones

**Por qué:** cada cambio de estado de un `Ticket` se audita en la tabla `ticket_transitions`
con `fromStatus`, `toStatus`, `orderId` y `timestamp`. Permite reconstruir el historial
completo de cualquier entrada.

---

## Concurrencia y consistencia

### Anti-sobreventa

El sistema usa un **inventario híbrido** con dos niveles de consistencia:

1. **Contadores en `Event`** (`available >= quantity`): gate que impide reservar más entradas
   de las disponibles. En DynamoDB, esto se implementaría con `TransactWriteItems` y
   `ConditionExpression: available >= :qty`.

2. **Filas `Ticket`**: cada entrada se reserva individualmente (AVAILABLE→RESERVED) con
   condición `status = AVAILABLE`. Dos reservas concurrentes del mismo cupo: una gana, la
   otra falla.

### Test de concurrencia

El test `ProcessOrderConcurrencyTest` verifica el escenario decisivo:

- **100 órdenes concurrentes** para **10 entradas** (1 entrada por orden)
- Resultado: **exactamente 10 RESERVED**, **90 REJECTED**
- **Sin sobreventa**: 0 entradas AVAILABLE al final, contadores cuadran

El in-memory repository simula la atomicidad de DynamoDB con un claim atómico en
`findAvailableByEvent` (synchronized): encuentra y marca entradas como RESERVED en una sola
operación atómica, previniendo que dos hilos obtengan la misma entrada.

### Consistencia eventual

El sistema es **eventualmente consistente** entre el API y el Worker:

1. `POST /orders` crea la orden `PENDING` y publica a SQS → `202 Accepted` inmediato.
2. El Worker consume el mensaje y procesa la reserva → orden `RESERVED` o `REJECTED`.
3. `GET /orders/{id}` puede devolver `PENDING` brevemente antes de que el Worker procese.

---

## Asincronía y procesamiento basado en eventos

### Flujo asíncrono

```
Cliente                API (WebFlux)              SQS              Worker
  │                       │                       │                 │
  │── POST /orders ──────►│                       │                 │
  │                       │── Order PENDING ──►  │                 │
  │                       │── publish(msg) ────► │                 │
  │◄── 202 {orderId} ────│                       │                 │
  │                       │                       │── receive ────►│
  │                       │                       │                 │── reserve tickets
  │                       │                       │                 │── update counters
  │                       │                       │                 │── Order RESERVED
  │                       │                       │                 │
  │── GET /orders/{id} ──►│                       │                 │
  │◄── 200 {RESERVED} ───│                       │                 │
```

### Componentes

- **`CreateOrderService`**: crea orden `PENDING`, serializa payload JSON, publica vía
  `MessagePublisher` (SQS producer).
- **`SqsMessageConsumer`** (Worker): loop `receiveMessage` con `waitTimeSeconds=20` (long polling)
  en `boundedElastic`. Despacha a `MessageHandler` por tipo de mensaje.
- **`OrderCreatedHandler`**: invoca `ProcessOrderUseCase` → reserva atómica.
- **`ExpiredReservationScheduler`** (Worker): `@Scheduled(fixedDelay=30s)` → libera reservas
  con `reservedUntil < now`.

---

## Escalabilidad, resiliencia y tolerancia a fallos

### Escalabilidad

| Dimensión | Estrategia |
|-----------|-----------|
| **API horizontal** | Stateless — escala con más instancias detrás de un load balancer |
| **Worker horizontal** | Múltiples workers consumiendo SQS en paralelo (SQS garantiza entrega única) |
| **DynamoDB** | `BillingMode.PAY_PER_REQUEST` (on-demand) — escala automáticamente |
| **Disponibilidad O(1)** | Contadores en `Event` — no escanea filas para saber cupo |

### Resiliencia

| Mecanismo | Descripción |
|-----------|-------------|
| **Desacoplamiento API-Worker** | Si el Worker cae, el API sigue aceptando órdenes (quedan en SQS) |
| **SQS como buffer** | Absorbe picos — las órdenes se acumulan en la cola hasta que el Worker las procesa |
| **Long polling** | `waitTimeSeconds=20` reduce llamadas vacías al SQS |
| **TTL de reservas** | 10 min — las reservas no confirmadas se liberan automáticamente |
| **Scheduler** | Libera reservas expiradas cada 30s, devuelve entradas al pool AVAILABLE |

### Tolerancia a fallos

- **Orden perdida**: SQS garantiza at-least-once delivery. El Worker es idempotente: si recibe
  una orden ya procesada, el `OrderRepository.findById` devuelve la orden en su estado actual
  (RESERVED/REJECTED), y el `ProcessOrderService` no la reprocesa.
- **Worker crash**: las reservas en vuelo (RESERVED sin confirmar) se liberan por el scheduler
  al vencer el TTL.
- **DynamoDB throttle**: con `PAY_PER_REQUEST`, DynamoDB escala automáticamente. En caso de
  throttle, el SDK reintentá con backoff exponencial.

---

## Seguridad

### Manejo de secretos y credenciales

- **Local**: credenciales dummy (`AWS_ACCESS_KEY_ID=local`) — DynamoDB Local y LocalStack
  no las validan.
- **Producción**: credenciales vía **IAM roles** (no hardcodeadas). Eliminar `endpoint`
  de `application.yml` y configurar IAM role en la instancia/contenedor.
- **Variables de entorno**: todas las configuraciones sensibles se externalizan vía env vars
  en `docker-compose.yml` (no hay secrets en el código).

### Consideraciones frente a ataques comunes

| Vector | Mitigación |
|--------|-----------|
| **Reintentos maliciosos** | SQS deduplica mensajes; `ProcessOrderService` es idempotente (verifica estado antes de procesar) |
| **Idempotencia** | Si SQS entrega el mismo mensaje dos veces, el Worker verifica `order.status()` — no reprocesa órdenes ya RESERVED/REJECTED |
| **Abuso de recursos** | `@Positive` en quantity (no permite 0 o negativo); `totalCapacity` validado; WebFlux no bloqueante (no hay thread exhaustion por I/O) |
| **Replay de orden** | El `POST /orders/{id}/confirm` valida que la orden esté `RESERVED` y no expirada — no se puede confirmar una orden ya SOLD o EXPIRED |
| **Inyección** | No hay SQL — DynamoDB usa el Enhanced Client con `Key` builders. Validación de input con Jakarta Bean Validation (`@NotBlank`, `@Positive`) |

---

## Diagramas

### Diagrama de arquitectura

```
                    ┌─────────────────────────────────────────────────────┐
                    │                  INFRAESTRUCTURA                     │
                    │                                                     │
    HTTP Client ───►│  ┌──────────┐    ┌──────────────┐    ┌───────────┐  │
                    │  │ EventCtrl │    │ OrderCtrl    │    │ Worker    │  │
                    │  │ OrderCtrl │    │ ExceptionHdl │    │ SqsConsumer│ │
                    │  └────┬─────┘    └──────┬───────┘    └─────┬─────┘  │
                    │       │                 │                  │        │
                    │       ▼                 ▼                  ▼        │
                    │  ┌──────────────────────────────────────────────┐  │
                    │  │              APLICACIÓN (Use Cases)            │  │
                    │  │  CreateEvent · GetAvailability · CreateOrder  │  │
                    │  │  ProcessOrder · ConfirmOrder · GrantComplim.  │  │
                    │  │  ReleaseExpired · GetOrder · GetEvent · List  │  │
                    │  └──────────────────┬───────────────────────────┘  │
                    │                     │                               │
                    │       ┌─────────────┼─────────────┐                │
                    │       ▼             ▼             ▼                │
                    │  ┌─────────┐  ┌──────────┐  ┌──────────┐           │
                    │  │ DynamoDB│  │   SQS    │  │ Scheduler│           │
                    │  │  repos  │  │ producer │  │  (worker)│           │
                    │  └────┬────┘  └────┬─────┘  └──────────┘           │
                    └───────┼────────────┼───────────────────────────────┘
                            ▼            ▼
                    ┌────────────┐  ┌──────────┐
                    │  DynamoDB  │  │   SQS    │
                    │  (tables)  │  │ (queue)  │
                    └────────────┘  └──────────┘
```

### Diagrama de flujo: ciclo de vida de una orden

```
         ┌──────────┐
         │ PENDING  │  POST /orders → 202 + SQS publish
         └────┬─────┘
              │ Worker consume SQS
              ▼
    ┌─────────────────┐
    │ reserveOrReject │  findAvailableByEvent → enough tickets?
    └────────┬────────┘
             │
     ┌───────┴───────┐
     │               │
     ▼ Sí            ▼ No
┌──────────┐   ┌──────────┐
│ RESERVED │   │ REJECTED │  (TTL 10 min)
│ TTL 10m  │   └──────────┘
└────┬─────┘
     │
     ├── POST /confirm ──► ┌──────────┐
     │                      │  SOLD    │  (final)
     │                      └──────────┘
     │
     └── TTL expira ──► Scheduler ──► ┌──────────┐
                                        │ EXPIRED  │  (final)
                                        └──────────┘
```

### Máquina de estados de Ticket

```
          ┌─────────────┐   reservar   ┌────────────┐   TTL vence   ┌─────────────┐
   ┌────► │  AVAILABLE  │ ───────────► │  RESERVED  │ ────────────► │  AVAILABLE  │
   │      └─────────────┘              └────────────┘               └─────────────┘
   │          │                              │ confirm
   │          │ cortesía                     ▼
   │          ▼                   ┌────────────┐
   │  ┌───────────────┐           │    SOLD    │  (final)
   │  │ COMPLIMENTARY │ (final)   └────────────┘
   │  └───────────────┘
   └────────────────────────────────── (liberar: scheduler / fallo)
```

### Tablas DynamoDB y GSIs

```
events (PK: eventId)
  └── GSI name-index (name)

tickets (PK: ticketId)
  ├── GSI event-status-index (eventId + status)        ← reservas por evento
  └── GSI status-reservedUntil-index (status + reservedUntil) ← scheduler

orders (PK: orderId)

ticket_transitions (PK: transitionId)
  └── GSI ticket-id-index (ticketId + timestamp)       ← historial por entrada
```

---

## Limitaciones y mejoras futuras

### Limitaciones actuales

1. **No usa `TransactWriteItems`**: la reserva atómica usa read-check-update-save en lugar
   de una transacción DynamoDB. En picos extremos, dos reservas concurrentes del último cupo
   podrían ambas leer `available=1`. El gate del contador previene sobreventa a nivel de
   filas, pero los contadores podrían desincronizarse brevemente.

2. **No hay retry en el Worker**: si el procesamiento de una orden falla (ej. DynamoDB
   throttle), el mensaje vuelve a SQS tras el visibility timeout, pero no hay backoff
   exponencial ni dead-letter queue (DLQ) configurados.

3. **No hay autenticación/autorización**: los endpoints son públicos. No hay RBAC para
   distinguir who can create events vs. who can buy tickets vs. who can grant complimentary.

4. **No hay paginación en `GET /events`**: devuelve todos los eventos. En producción,
   necesitaría paginación o cursor.

5. **DynamoDB Local es in-memory**: los datos se pierden al detener los contenedores.
   No apto para persistencia real en desarrollo sin volúmenes.

6. **No hay métricas ni observabilidad**: no hay Micrometer, Prometheus, ni distributed
   tracing (OpenTelemetry/X-Ray).

7. **Pago simulado**: `POST /confirm` asume que el pago siempre es exitoso. No hay
   integración con un gateway de pago.

### Posibles mejoras

| Mejora | Descripción |
|--------|-------------|
| **`TransactWriteItems`** | Reemplazar read-check-update con transacción atómica DynamoDB (gate contador + N filas en una sola transacción) |
| **DLQ + retry** | Configurar dead-letter queue en SQS + backoff exponencial en el Worker |
| **OAuth2/JWT** | Añadir Spring Security con JWT para autenticación y RBAC |
| **Paginación** | Cursor-based pagination en `GET /events` usando DynamoDB `LastEvaluatedKey` |
| **Métricas** | Micrometer + Prometheus + Grafana para latencia, throughput, errores |
| **Distributed tracing** | AWS X-Ray o OpenTelemetry para trazar el flujo API → SQS → Worker |
| **Idempotency keys** | Aceptar `Idempotency-Key` header en `POST /orders` para deduplicar a nivel API |
| **Rate limiting** | Bucket4j o API Gateway rate limiting para prevenir abuso |
| **Pago real** | Integrar Stripe/PayPal en el flujo de confirmación |

### Qué cambiaría en un entorno productivo real

1. **`TransactWriteItems`** para reserva atómica real — la mejora más crítica para
   garantizar cero sobreventa bajo concurrencia extrema.

2. **Múltiple módulo Maven** (`core`, `app-api`, `app-worker`) en lugar de perfiles Spring —
   separación más limpia, builds independientes, JARs más pequeños.

3. **IAM roles + Secrets Manager** — nunca credenciales en env vars o config files.
   Usar AWS Secrets Manager o Parameter Store para secrets.

4. **DynamoDB On-Demand → Provisioned con autoscaling** — más cost-eficiente en producción
   con tráfico predecible.

5. **SQS FIFO** si el orden de procesamiento importa (ej. por evento). Actualmente usa
   Standard Queue (best-effort ordering).

6. **CI/CD pipeline** — GitHub Actions con build, test, JaCoCo gate, scan de vulnerabilidades
   (Dependabot/Trivy), deploy a ECS/EKS.

7. **Health checks** — `/actuator/health` con Spring Boot Actuator para readiness/liveness
   probes en Kubernetes.

8. **Event sourcing** — en lugar de solo auditar transiciones, usar event sourcing completo
   donde el estado del Ticket se reconstruye desde la secuencia de eventos.

---

## Git Flow

| Branch | Propósito | Origen |
|---|---|---|
| `main` | Código production-ready | — |
| `develop` | Integración de features | `main` |
| `feature/*` | Nuevas funcionalidades | `develop` |
| `release/*` | Preparación de release | `develop` |
| `hotfix/*` | Fixes en producción | `main` |

### Convenciones de commits

| Prefijo | Uso |
|---|---|
| `feat:` | Nueva funcionalidad |
| `fix:` | Corrección de bug |
| `docs:` | Documentación |
| `refactor:` | Refactorización |
| `test:` | Tests |
| `chore:` | Tareas de mantenimiento |
