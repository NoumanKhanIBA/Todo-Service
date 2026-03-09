# Todo Service

A RESTful backend service for managing a simple to-do list, built with Spring Boot and Java 21.

---

## Description & Assumptions

The service manages to-do items with the following lifecycle:

- **NOT_DONE** – default status on creation
- **DONE** – manually set; records the timestamp of completion
- **PAST_DUE** – automatically applied by a scheduler when an item's due date passes

### Key assumptions

| Topic | Decision |
|---|---|
| Past-due enforcement | A background scheduler runs **every minute**, fetches all `NOT_DONE` items whose `dueAt` has elapsed, and updates each entity individually in code. The cron expression is configurable via `app.scheduler.past-due-cron`. |
| Modifying DONE items | `DONE` items *can* be updated (description changed or reverted to `NOT_DONE`), since the spec only forbids modifying `PAST_DUE` items. |
| Idempotency | Marking a `NOT_DONE` item as `NOT_DONE` again is a no-op; `doneAt` remains `null`. |
| Due date on creation | The `dueAt` field must be in the future at the time of creation. |
| Error format | Errors are returned as [RFC 9457 Problem Details](https://www.rfc-editor.org/rfc/rfc9457) (`application/problem+json`). |
| Authentication | Not implemented as per the requirements. |
| Persistence | H2 in-memory database – data is lost on restart. |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.2 |
| Persistence | Spring Data JPA + H2 (in-memory) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Scheduling | Spring `@Scheduled` |
| Build | Maven 3.9 |
| Containerization | Docker (multi-stage build) |
| Testing | JUnit 5, Mockito, Spring MockMvc |

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/todos` | Create a new todo item |
| `PATCH` | `/api/todos/{id}/description` | Update description |
| `PATCH` | `/api/todos/{id}/done` | Mark as done |
| `PATCH` | `/api/todos/{id}/not-done` | Mark as not done |
| `GET` | `/api/todos` | List NOT_DONE items (`?all=true` for all) |
| `GET` | `/api/todos/{id}` | Get item details |

### Example: Create a todo

```bash
curl -X POST http://localhost:8080/api/todos \
  -H "Content-Type: application/json" \
  -d '{"description": "Buy groceries", "dueAt": "2026-12-31T18:00:00"}'
```

### Example: Get all items (including done/past-due)

```bash
curl "http://localhost:8080/api/todos?all=true"
```

---

## How To

### Build

```bash
mvn clean package
```

The built JAR is placed in `target/`.

### Run Automatic Tests

```bash
mvn test
```

Test reports are generated at `target/surefire-reports/`.

### Run Locally (without Docker)

```bash
mvn spring-boot:run
```

The service starts on **http://localhost:8080**.

The H2 console is available at **http://localhost:8080/h2-console** (JDBC URL: `jdbc:h2:mem:tododb`, user: `sa`, no password).

### Build & Run with Docker

```bash
# Build image
docker build -t todo-service .

# Run container
docker run -p 8080:8080 --name todo-service --rm todo-service
```

> `--name todo-service` gives the container a fixed name in Docker Desktop instead of a random one.  
> `--rm` auto-removes the container when stopped so no leftover containers pile up.

If you stop and re-run and see `container name already in use`, remove it first:

```bash
docker rm todo-service
```

### Run with Docker Compose

```bash
docker compose up --build
```

To stop:

```bash
docker compose down
```
