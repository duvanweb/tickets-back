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
├── Dockerfile                          # Multi-stage build (Maven+JDK25 → JRE25)
├── docker-compose.yml                  # DynamoDB Local + app
├── .dockerignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/tickets/
    │   │   ├── TicketsApplication.java
    │   │   ├── domain/
    │   │   │   ├── model/event/           # Event, EventId, EventStatus
    │   │   │   └── exception/            # DomainException (sealed), InvalidEvent, NotFound
    │   │   ├── application/
    │   │   │   ├── port/in/event/         # Use case interfaces
    │   │   │   ├── port/out/repositories/ # Repository interfaces
    │   │   │   └── usecase/event/         # Use case implementations
    │   │   └── infrastructure/
    │   │       ├── adapter/in/web/        # REST controllers, DTOs
    │   │       ├── adapter/out/persistence/ # DynamoDB adapters
    │   │       └── config/               # Spring configuration
    │   └── resources/
    │       └── application.yml
    └── test/
```

## Prerrequisitos

- **Docker** + **Docker Compose** (recomendado — no requiere Java ni Maven instalados)
- O, para ejecución local: **Java 25** (OpenJDK 25+) y **Maven 3.9+**

## Ejecución

### Con Docker Compose (recomendado)

Levanta DynamoDB Local y la aplicación con un solo comando. El build del JAR
se realiza dentro del contenedor (multi-stage build), por lo que no requiere
Java ni Maven instalados localmente.

```bash
# Construir y arrancar (primera vez o tras cambios en el código)
docker compose up --build

# Arrancar en background
docker compose up --build -d

# Ver logs
docker compose logs -f app

# Detener
docker compose down
```

Servicios expuestos:

| Servicio | URL | Descripción |
|---|---|---|
| Aplicación | `http://localhost:8080` | API REST Spring Boot |
| DynamoDB Local | `http://localhost:8000` | Inspección con AWS CLI o GUI |

La tabla `events` se crea automáticamente al arrancar. DynamoDB Local
opera en modo in-memory (los datos se pierden al detener los contenedores).

### Local sin Docker

Alternativa para desarrollo con hot reload. Requiere Java 25 y Maven.

```bash
# Iniciar DynamoDB Local
docker run -p 8000:8000 amazon/dynamodb-local

# Arrancar la aplicación
mvn spring-boot:run
```

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
| `POST` | `/api/events` | Crea un evento | 201 Created |
| `GET` | `/api/events/{id}` | Consulta por ID | 200 OK / 404 Not Found |
| `GET` | `/api/events` | Lista todos los eventos | 200 OK |

### Ejemplo

```bash
# Crear evento
curl -X POST http://localhost:8080/api/events \
  -H "Content-Type: application/json" \
  -d '{"name": "Concierto Docker", "date": "2026-12-15", "place": "Estadio Nacional", "totalCapacity": 500}'

# Consultar evento
curl http://localhost:8080/api/events/{id}

# Listar todos los eventos
curl http://localhost:8080/api/events
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
