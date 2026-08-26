# API Test Results — Student Management System

Base URL: `http://localhost:29909`
Test date: Iteration 0 (baseline) — all endpoints exercised via inline curl commands.

Legend: PASS = behaved as expected (2xx for success cases, or correct 4xx for negative/authz cases). FAIL = unexpected status/body. SKIPPED = not independently testable.

## Auth (`/api/v1/auth`)

| Endpoint | Case | Result | Notes |
|---|---|---|---|
| POST /auth/register | ADMIN creates user | PASS | 201, returns user w/o password |
| POST /auth/register | no token | PASS | 403 (access denied, correctly rejected) |
| POST /auth/register | STUDENT token | PASS | 403 rejected |
| POST /auth/register | duplicate email | PASS | 409 |
| POST /auth/register | invalid email format | PASS | 400 with field error |
| POST /auth/login | ADMIN/TEACHER/STUDENT valid creds | PASS | 200, tokens + user info returned |
| POST /auth/login | wrong password | PASS | 401 |
| POST /auth/refresh | valid refresh token | PASS | 200, new access+refresh (rotated) |
| POST /auth/refresh | reuse rotated/revoked token | PASS | 401 "revoked" |
| POST /auth/refresh | garbage token | PASS | 401 "invalid" |
| POST /auth/logout | valid refresh token | PASS | 200, revokes token |
| POST /auth/logout | reuse token after logout | PASS | 401 revoked |
| POST /auth/change-password | wrong current password | PASS | 400 |
| POST /auth/change-password | correct flow | PASS | 200; old password rejected on next login, new password works |

## Users (`/api/v1/users`) — ADMIN only

| Endpoint | Case | Result |
|---|---|---|
| POST /users | create | PASS (201) |
| GET /users | paginated list (admin) | PASS (200) |
| GET /users | TEACHER token | PASS (403 rejected) |
| GET /users/{id} | get by id | PASS (200) |
| PUT /users/{id} | update | PASS (200) |
| PATCH /users/{id}/status | lock user | PASS (200) |
| DELETE /users/{id} | soft delete | PASS (204); subsequent GET returns 200 w/ status=INACTIVE (soft-delete by design) |

## Students (`/api/v1/students`)

| Endpoint | Case | Result |
|---|---|---|
| POST /students | ADMIN create (creates User+Student) | PASS (201) |
| POST /students | duplicate studentNumber | PASS (409) |
| POST /students | invalid payload | PASS (400, multiple field errors) |
| POST /students | TEACHER token | PASS (403) |
| GET /students | ADMIN (all) | PASS (200) |
| GET /students | TEACHER (scoped to assigned students) | PASS (200) |
| GET /students/{id} | ADMIN any | PASS (200) |
| GET /students/{id} | unrelated STUDENT | PASS (403) |
| PUT /students/{id} | ADMIN full update | PASS (200) |
| PUT /students/{id} | STUDENT own record, admin fields ignored | PASS (200; department/studentNumber/status unchanged) |
| PUT /students/{id} | STUDENT other's record | PASS (403) |
| DELETE /students/{id} | ADMIN soft delete | PASS (204) |
| GET /students/{id}/courses | self | PASS (200) |
| GET /students/{id}/attendance | self | PASS (200) |
| GET /students/{id}/grades | self | PASS (200) |
| GET /students/{studentId}/enrollments | self | PASS (200) |
| GET /students/{studentId}/attendance/summary | self, verified calc (50%) | PASS (200) |
| GET /students/{studentId}/grades/summary | self, verified calc (avg 81.25) | PASS (200) |

## Teachers (`/api/v1/teachers`)

| Endpoint | Case | Result |
|---|---|---|
| POST /teachers | ADMIN create | PASS (201) |
| POST /teachers | duplicate employeeNumber | PASS (409) |
| POST /teachers | STUDENT token | PASS (403) |
| GET /teachers | ADMIN & TEACHER | PASS (200) |
| GET /teachers/{id} | get by id | PASS (200) |
| PUT /teachers/{id} | own profile | PASS (200) |
| PUT /teachers/{id} | other teacher's profile | PASS (403) |
| DELETE /teachers/{id} | ADMIN | PASS (204) |
| GET /teachers/{id}/courses | own | PASS (200) |

## Courses (`/api/v1/courses`)

| Endpoint | Case | Result |
|---|---|---|
| POST /courses | ADMIN create | PASS (201) |
| POST /courses | duplicate courseCode | PASS (409) |
| POST /courses | TEACHER token | PASS (403) |
| GET /courses | STUDENT view | PASS (200) |
| GET /courses/{id} | STUDENT view | PASS (200) |
| PUT /courses/{id} | ADMIN | PASS (200) |
| PUT /courses/{id} | TEACHER (403) | PASS (403) |
| PUT /courses/{courseId}/teacher/{teacherId} | assign | PASS (200) |
| GET /courses/{courseId}/students | assigned teacher | PASS (200) |
| GET /courses/{courseId}/students | unassigned teacher | PASS (403) |
| DELETE /courses/{id} | ADMIN | PASS (204) |

## Enrollments (`/api/v1/enrollments`)

| Endpoint | Case | Result |
|---|---|---|
| POST /enrollments | ADMIN create | PASS (201) |
| POST /enrollments | duplicate | PASS (409) |
| POST /enrollments | nonexistent student | PASS (404) |
| POST /enrollments | TEACHER token | PASS (403) |
| GET /enrollments | ADMIN | PASS (200) |
| GET /enrollments/{id} | ADMIN | PASS (200) |
| GET /enrollments/{id} | own (student) | PASS (200) |
| GET /enrollments/{id} | unrelated student | PASS (403) |
| DELETE /enrollments/{id} | ADMIN (hard delete) | PASS (204); GET afterwards returns 404 |
| DELETE /enrollments/{id} | TEACHER (403) | PASS (403) |

## Attendance (`/api/v1/attendance`)

| Endpoint | Case | Result |
|---|---|---|
| POST /attendance | assigned TEACHER create | PASS (201) |
| POST /attendance | duplicate (student/course/date) | PASS (409) |
| POST /attendance | unassigned teacher | PASS (403) |
| POST /attendance | student not enrolled | PASS (400) |
| POST /attendance | future date | PASS (400 validation) |
| GET /attendance | filters (studentId/courseId) | PASS (200) |
| GET /attendance/{id} | ADMIN | PASS (200) |
| GET /attendance/{id} | owning student | PASS (200) |
| PUT /attendance/{id} | assigned teacher | PASS (200) |
| PUT /attendance/{id} | unassigned teacher | PASS (403) |
| GET /students/{id}/attendance/summary | verified calc (66.67%) | PASS (200) |
| DELETE /attendance/{id} | assigned teacher (hard delete) | PASS (204); GET afterwards 404 |

## Grades (`/api/v1/grades`)

| Endpoint | Case | Result |
|---|---|---|
| POST /grades | assigned teacher, auto-calc grade (93→A+) | PASS (201, grade="A+") |
| POST /grades | marks > 100 | PASS (400 validation) |
| POST /grades | unassigned teacher | PASS (403) |
| POST /grades | student not enrolled | PASS (400) |
| GET /grades | filters | PASS (200) |
| GET /grades/{id} | ADMIN | PASS (200) |
| GET /grades/{id} | unrelated student | PASS (403) |
| PUT /grades/{id} | recalculates grade (65→C) | PASS (200, grade="C") |
| GET /students/{id}/grades/summary | verified calc | PASS (200) |
| DELETE /grades/{id} | TEACHER (403, ADMIN only) | PASS (403) |
| DELETE /grades/{id} | ADMIN (hard delete) | PASS (204); GET afterwards 404 |

## Profile (`/api/v1/profile`)

| Endpoint | Case | Result |
|---|---|---|
| GET /profile | authenticated | PASS (200) |
| PUT /profile | update own info | PASS (200) |
| GET /profile | no token | PASS (403 rejected) |

## Dashboard (`/api/v1/dashboard`)

| Endpoint | Case | Result |
|---|---|---|
| GET /dashboard/admin | ADMIN | PASS (200) |
| GET /dashboard/admin | TEACHER | PASS (403) |
| GET /dashboard/teacher | TEACHER | PASS (200) |
| GET /dashboard/student | STUDENT | PASS (200) |

## Audit Logs (`/api/v1/audit-logs`) — ADMIN only

| Endpoint | Case | Result |
|---|---|---|
| GET /audit-logs | ADMIN paginated | PASS (200) |
| GET /audit-logs | TEACHER | PASS (403) |
| GET /audit-logs/{id} | ADMIN | PASS (200) |

## Health

| Endpoint | Case | Result |
|---|---|---|
| GET /api/v1/health | public | PASS (200, db UP) |
| GET /actuator/health | public | PASS (200) |

## Infra / Docs

| Endpoint | Case | Result |
|---|---|---|
| GET /docs | Swagger UI | PASS (302 → index.html) |
| GET /api-docs | OpenAPI JSON | PASS (200, 36 paths documented) |
| GET /ws | WebSocket/SockJS info endpoint | PASS (200) |

## Security

| Case | Result |
|---|---|
| Invalid/malformed JWT on protected endpoint | PASS (403 rejected) |
| Expired token rejected | PASS (verified via JwtUtilTest unit test) |
| Revoked refresh token rejected | PASS |

## Summary

All endpoint groups defined in the specification were exercised. All test cases PASSED
on the first full run (iteration 0). No FAILED or SKIPPED endpoints were recorded.
Test data created during testing (extra user/student/teacher/course/enrollment/attendance/grade
records) was cleaned up (soft-deleted or hard-deleted) after verification, leaving the original
seed data intact.
