# Tickets Backend

Backend de gestión de tickets construido con **Java 25**, **Spring Boot 4**, **Spring WebFlux** (reactivo) y **AWS DynamoDB**. Sigue una **Arquitectura Hexagonal** (Ports & Adapters) estricta.

## Stack Tecnológico

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 25 (Virtual Threads, Records, Sealed Classes, Pattern Matching) |
| Framework | Spring Boot 4.0 |
| Web | Spring WebFlux (Netty, reactivo — `Mono`/`Flux`) |
| Persistencia | AWS DynamoDB (SDK v2 Enhanced Client) |
| Build | Maven |
| Validación | Jakarta Bean Validation |
| Testing | JUnit 5, Mockito, AssertJ, Reactor Test (`StepVerifier`) |

## Arquitectura

El proyecto sigue una arquitectura hexagonal con tres capas y dependencias unidireccionales:

```
infraestructura → aplicación → dominio
dominio → (nada, 100% puro)
```

| Capa | Paquete base | Responsabilidad |
|---|---|---|
| **Domain** | `com.tickets.domain` | Modelos de dominio, Value Objects y excepciones. Sin frameworks. |
| **Application** | `com.tickets.application` | Puertos de entrada/salida y casos de uso. Sin anotaciones de Spring. |
| **Infrastructure** | `com.tickets.infrastructure` | Adaptadores REST, DynamoDB y configuración. Aquí viven las anotaciones de Spring. |

## Estructura del Proyecto

```
tickets-back/
├── pom.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/tickets/
    │   │   ├── TicketsApplication.java
    │   │   ├── domain/
    │   │   │   ├── model/ticket/          # Ticket, TicketId, TicketStatus
    │   │   │   └── exception/             # DomainException (sealed), InvalidTicket, NotFound
    │   │   ├── application/
    │   │   │   ├── port/in/ticket/        # Use case interfaces
    │   │   │   ├── port/out/repositories/ # Repository interfaces
    │   │   │   └── usecase/ticket/        # Use case implementations
    │   │   └── infrastructure/
    │   │       ├── adapter/in/web/        # REST controllers, DTOs
    │   │       ├── adapter/out/persistence/ # DynamoDB adapters
    │   │       └── config/               # Spring configuration
    │   └── resources/
    │       └── application.yml
    └── test/
```

## Prerrequisitos

- **Java 25** (OpenJDK 25+)
- **Maven 3.9+**
- **DynamoDB Local** (para desarrollo) o acceso a AWS DynamoDB

## Ejecución

### Desarrollo con DynamoDB Local

```bash
# Iniciar DynamoDB Local
docker run -p 8000:8000 amazon/dynamodb-local

# Arrancar la aplicación
mvn spring-boot:run
```

La aplicación está disponible en `http://localhost:8080`.

### Comandos Maven

```bash
mvn clean compile          # Compilar
mvn test                   # Ejecutar tests
mvn spring-boot:run        # Arrancar aplicación
mvn package                # Build del JAR
```

## Endpoints

| Método | Ruta | Descripción | Status |
|---|---|---|---|
| `POST` | `/api/tickets` | Crea un ticket | 201 Created |
| `GET` | `/api/tickets/{id}` | Consulta por ID | 200 OK / 404 Not Found |

### Ejemplo

```bash
# Crear ticket
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"title": "Bug en login", "description": "No funciona el botón"}'

# Consultar ticket
curl http://localhost:8080/api/tickets/{id}
```

## Git Flow

Este proyecto utiliza el modelo **Git Flow** para la gestión de branches:

| Branch | Propósito | Origen |
|---|---|---|
| `main` | Código production-ready | — |
| `develop` | Integración de features | `main` |
| `feature/*` | Nuevas funcionalidades | `develop` |
| `release/*` | Preparación de release | `develop` |
| `hotfix/*` | Fixes en producción | `main` |

### Workflow típico

```bash
# 1. Crear feature branch desde develop
git checkout develop
git checkout -b feature/nueva-funcionalidad

# 2. Desarrollar y commit
git commit -m "feat: descripción de la funcionalidad"

# 3. Merge a develop
git checkout develop
git merge feature/nueva-funcionalidad
git branch -d feature/nueva-funcionalidad

# 4. Release (de develop a main)
git checkout main
git merge develop
git tag v1.0.0
```

### Convenciones de commits

| Prefijo | Uso |
|---|---|
| `feat:` | Nueva funcionalidad |
| `fix:` | Corrección de bug |
| `docs:` | Documentación |
| `refactor:` | Refactorización |
| `test:` | Tests |
| `chore:` | Tareas de mantenimiento |
