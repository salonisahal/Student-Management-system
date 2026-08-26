# Student Management System (No Kafka, Only Spring Boot)

Enterprise-level **Student Management System** REST API built with Spring Boot 3, Spring Security,
JWT (access + refresh tokens), RBAC, Spring Data JPA/Hibernate, MySQL, Flyway, and OpenAPI/Swagger.

## 1. Overview

The system manages Users, Students, Teachers, Courses, Enrollments, Attendance, Grades, and Audit Logs.
Three roles are supported: **ADMIN**, **TEACHER**, **STUDENT**, each with strictly enforced,
resource-level authorization on the backend (not just UI-level restrictions).

## 2. Architecture

```
controller  -> REST endpoints (DTOs only, no entities exposed)
service     -> business logic, transactions, authorization rules
repository  -> Spring Data JPA interfaces
entity      -> JPA entities
dto         -> request/response payloads with Bean Validation
mapper      -> entity <-> DTO conversion
security    -> JwtUtil, JwtFilter, UserDetailsServiceImpl, UserPrincipal
exception   -> custom exceptions + @RestControllerAdvice global handler
config      -> SecurityConfig, CorsConfig, OpenApiConfig, WebSocketConfig
audit       -> AuditService (writes AuditLog rows for sensitive operations)
util        -> GradeCalculator, SecurityUtil, TokenHashUtil
```

Flow: **Controller -> Service -> Repository -> MySQL**. All business logic lives in the service layer.

## 3. Technology Stack

Java 17, Spring Boot 3.2.5, Spring Web, Spring Security, Spring Data JPA/Hibernate, MySQL 8,
Flyway, Bean Validation, Lombok, JJWT (HS256), springdoc-openapi 2.3.0, JUnit 5, Mockito.

## 4. Prerequisites

* JDK 17+
* Maven 3.9+
* MySQL 8+ reachable at the configured JDBC URL
* (Optional) Docker & Docker Compose

## 5. MySQL Setup

The application is pre-configured to use:

```
JDBC URL : jdbc:mysql://localhost:3306/gen_0cd68058ca0d
Username : myuser
Password : mypassword
```

Ensure this schema/user exists and is reachable, or override with environment variables (see below).

## 6. Environment Variables

| Variable | Purpose | Default |
|---|---|---|
| `JWT_SECRET` | Base64 HMAC secret for signing JWTs | built-in fallback (dev only) |
| `JWT_ACCESS_EXPIRATION` | Access token TTL in ms | `1800000` (30 min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms | `604800000` (7 days) |
| `SPRING_DATASOURCE_URL` | JDBC URL | `jdbc:mysql://localhost:3306/gen_0cd68058ca0d` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `myuser` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `mypassword` |

Secrets are never hardcoded in source; the values above are just safe local-dev fallbacks.

## 7. Database Migration

Flyway migrations under `src/main/resources/db/migration` create all 9 tables
(`users`, `refresh_tokens`, `students`, `teachers`, `courses`, `enrollments`,
`attendance`, `grades`, `audit_logs`) with indexes, unique constraints, and foreign keys.
They run automatically on startup (`spring.flyway.enabled=true`). `data.sql` seeds sample rows afterwards.

## 8. How to Run

### Local (Maven)
```bash
mvn clean install
mvn spring-boot:run
```

### Using start scripts
```bash
chmod +x start.sh && ./start.sh     # Linux/Mac
start.bat                           # Windows
```

### Docker Compose (app + MySQL)
```bash
docker-compose up --build
```

The API is served on **http://localhost:29909**.

## 9. Running Tests
```bash
mvn test
```
Unit tests (JUnit 5 + Mockito) cover `JwtUtil` and `AuthService`. Integration tests use an in-memory
H2 database (`test` Spring profile) with MockMvc to verify authentication, RBAC, and health checks.

## 10. Swagger / OpenAPI

* Swagger UI: `http://localhost:29909/docs`
* OpenAPI JSON: `http://localhost:29909/api-docs`

Use the "Authorize" button with a Bearer access token to test protected endpoints directly.

## 11. Authentication Flow

1. `POST /api/v1/auth/register` (ADMIN only) creates a user with a given role.
2. `POST /api/v1/auth/login` returns `accessToken` (30 min TTL) + `refreshToken` (7 day TTL, persisted
   hashed in MySQL).
3. Every protected request sends `Authorization: Bearer <accessToken>`.
4. `POST /api/v1/auth/refresh` exchanges a valid, non-revoked refresh token for a new access token
   (refresh token rotation: old one is revoked, a new one issued).
5. `POST /api/v1/auth/logout` revokes the given refresh token.
6. `POST /api/v1/auth/change-password` verifies the current password, hashes the new one, and revokes
   all existing refresh tokens for that user.

## 12. RBAC Rules (summary)

| Module | ADMIN | TEACHER | STUDENT |
|---|---|---|---|
| User management | Full | - | - |
| Students | Full | View/Update assigned | Own record only |
| Teachers | Full | Own profile | - |
| Courses | Full | View/assigned | View |
| Enrollments | Full | View assigned | View own |
| Attendance | Full | Manage assigned courses | View own |
| Grades | Full | Manage assigned courses | View own |
| Dashboards | Admin dashboard | Teacher dashboard | Student dashboard |
| Audit logs | Full | - | - |

All authorization is enforced server-side via `@PreAuthorize` plus service-layer ownership checks
(e.g. a teacher can only touch students/attendance/grades tied to their own assigned courses).

## 13. Endpoint List

Base path: `/api/v1`

```
POST   /auth/register
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
POST   /auth/change-password

POST   /users
GET    /users
GET    /users/{id}
PUT    /users/{id}
PATCH  /users/{id}/status
DELETE /users/{id}

POST   /students
GET    /students
GET    /students/{id}
PUT    /students/{id}
DELETE /students/{id}
GET    /students/{id}/courses
GET    /students/{id}/attendance
GET    /students/{id}/grades
GET    /students/{studentId}/enrollments
GET    /students/{studentId}/attendance/summary
GET    /students/{studentId}/grades/summary

POST   /teachers
GET    /teachers
GET    /teachers/{id}
PUT    /teachers/{id}
DELETE /teachers/{id}
GET    /teachers/{id}/courses

POST   /courses
GET    /courses
GET    /courses/{id}
PUT    /courses/{id}
DELETE /courses/{id}
PUT    /courses/{courseId}/teacher/{teacherId}
GET    /courses/{courseId}/students

POST   /enrollments
GET    /enrollments
GET    /enrollments/{id}
DELETE /enrollments/{id}

POST   /attendance
GET    /attendance
GET    /attendance/{id}
PUT    /attendance/{id}
DELETE /attendance/{id}

POST   /grades
GET    /grades
GET    /grades/{id}
PUT    /grades/{id}
DELETE /grades/{id}

GET    /profile
PUT    /profile

GET    /dashboard/admin
GET    /dashboard/teacher
GET    /dashboard/student

GET    /audit-logs
GET    /audit-logs/{id}

GET    /health
```

## 14. Sample Requests

### Login
```bash
curl -X POST http://localhost:29909/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"Admin@123"}'
```

### Authenticated request
```bash
curl -X GET http://localhost:29909/api/v1/students \
  -H "Authorization: Bearer <accessToken>"
```

### Refresh token
```bash
curl -X POST http://localhost:29909/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

### Sample error response
```json
{
  "timestamp": "2024-05-01T10:00:00",
  "status": 403,
  "message": "Access denied",
  "data": null
}
```

## 15. Seeded Accounts

| Email | Password | Role |
|---|---|---|
| admin@example.com | Admin@123 | ADMIN |
| teacher1@example.com | Teacher@123 | TEACHER |
| teacher2@example.com | Teacher@123 | TEACHER |
| student1@example.com | Student@123 | STUDENT |
| student2@example.com | Student@123 | STUDENT |
| student3@example.com | Student@123 | STUDENT |

## 16. Notes on Delete Semantics

User/Student/Teacher/Course `DELETE` endpoints perform an **enterprise-safe soft delete**
(status set to `INACTIVE`) to preserve referential integrity and audit history, per the business
requirements. Enrollment/Attendance/Grade `DELETE` endpoints perform a **hard delete** since they
represent removable transactional records.
