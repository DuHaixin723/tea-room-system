# Tea Room Management Backend

## Stack

- Spring Boot 3
- Spring Web
- Spring Data JPA
- Flyway
- MySQL

## Run

1. Create database:

```sql
CREATE DATABASE tea_room_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

2. Update datasource in `src/main/resources/application.yml` if needed.
3. Start:

```bash
mvn spring-boot:run
```

Flyway will execute `V1__init_schema.sql` automatically at startup.

## Current scope

- Backend project scaffold
- Initial domain model
- Initial database schema with Flyway
- Health check endpoint: `GET /api/health`
- REST APIs for auth, user, tea room, tea, reservation, review, report, activity, order, favorite, consultation
- Statistics API: `GET /api/statistics/overview`
- Recommendation API: `GET /api/recommendations?userId=1`
- System config API: `/api/system-configs`
- WebSocket endpoint: `/ws-consultation`
- STOMP send destination: `/app/consultation.send`
- Topic subscription: `/topic/consultation/{sessionId}`
