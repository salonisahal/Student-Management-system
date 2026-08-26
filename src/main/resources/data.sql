-- Seed data for Student Management System
-- Passwords (BCrypt hashed):
--   admin@example.com    -> Admin@123
--   teacher1@example.com -> Teacher@123
--   teacher2@example.com -> Teacher@123
--   student1@example.com -> Student@123
--   student2@example.com -> Student@123
--   student3@example.com -> Student@123

INSERT IGNORE INTO users (id, first_name, last_name, email, password, phone, role, status, created_at, updated_at) VALUES
(1, 'System', 'Admin', 'admin@example.com', '$2b$12$byGyRXtZxDFdr3Ftv2HkW.cMOVm2Utpj.2MXZO3PW7iczRkdYp7Tq', '1000000001', 'ADMIN', 'ACTIVE', NOW(), NOW()),
(2, 'John', 'Carter', 'teacher1@example.com', '$2b$12$vbwIXSlLWdAqDk3ap4FMCe8GEzVCQ2w9W03ncv2VNZK7Z0PfnI77i', '1000000002', 'TEACHER', 'ACTIVE', NOW(), NOW()),
(3, 'Emily', 'Stone', 'teacher2@example.com', '$2b$12$vbwIXSlLWdAqDk3ap4FMCe8GEzVCQ2w9W03ncv2VNZK7Z0PfnI77i', '1000000003', 'TEACHER', 'ACTIVE', NOW(), NOW()),
(4, 'Alice', 'Johnson', 'student1@example.com', '$2b$12$V1jp/0i5aKqmX4lONW58kur5.9XDr52qbN0jr5.TGUH6iP1npeyFe', '1000000004', 'STUDENT', 'ACTIVE', NOW(), NOW()),
(5, 'Bob', 'Williams', 'student2@example.com', '$2b$12$V1jp/0i5aKqmX4lONW58kur5.9XDr52qbN0jr5.TGUH6iP1npeyFe', '1000000005', 'STUDENT', 'ACTIVE', NOW(), NOW()),
(6, 'Clara', 'Davis', 'student3@example.com', '$2b$12$V1jp/0i5aKqmX4lONW58kur5.9XDr52qbN0jr5.TGUH6iP1npeyFe', '1000000006', 'STUDENT', 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO teachers (id, user_id, employee_number, first_name, last_name, email, phone, department, joining_date, status, created_at, updated_at) VALUES
(1, 2, 'EMP001', 'John', 'Carter', 'teacher1@example.com', '1000000002', 'Computer Science', '2020-01-15', 'ACTIVE', NOW(), NOW()),
(2, 3, 'EMP002', 'Emily', 'Stone', 'teacher2@example.com', '1000000003', 'Mathematics', '2019-08-01', 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO students (id, user_id, student_number, first_name, last_name, email, phone, date_of_birth, gender, address, department, admission_date, status, created_at, updated_at) VALUES
(1, 4, 'STU001', 'Alice', 'Johnson', 'student1@example.com', '1000000004', '2003-05-10', 'FEMALE', '12 Elm Street', 'Computer Science', '2022-09-01', 'ACTIVE', NOW(), NOW()),
(2, 5, 'STU002', 'Bob', 'Williams', 'student2@example.com', '1000000005', '2002-11-22', 'MALE', '45 Oak Avenue', 'Computer Science', '2021-09-01', 'ACTIVE', NOW(), NOW()),
(3, 6, 'STU003', 'Clara', 'Davis', 'student3@example.com', '1000000006', '2003-02-14', 'FEMALE', '78 Pine Road', 'Mathematics', '2022-09-01', 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO courses (id, course_code, course_name, description, credits, department, teacher_id, status, created_at, updated_at) VALUES
(1, 'CS101', 'Introduction to Programming', 'Fundamentals of programming using Java', 4, 'Computer Science', 1, 'ACTIVE', NOW(), NOW()),
(2, 'CS201', 'Data Structures', 'Core data structures and algorithms', 4, 'Computer Science', 1, 'ACTIVE', NOW(), NOW()),
(3, 'MATH101', 'Calculus I', 'Differential and integral calculus', 3, 'Mathematics', 2, 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO enrollments (id, student_id, course_id, enrollment_date, status, created_at, updated_at) VALUES
(1, 1, 1, '2023-09-05', 'ACTIVE', NOW(), NOW()),
(2, 1, 2, '2023-09-05', 'ACTIVE', NOW(), NOW()),
(3, 2, 1, '2023-09-06', 'ACTIVE', NOW(), NOW()),
(4, 3, 3, '2023-09-06', 'ACTIVE', NOW(), NOW());

INSERT IGNORE INTO attendance (id, student_id, course_id, attendance_date, status, remarks, marked_by, created_at, updated_at) VALUES
(1, 1, 1, '2024-01-10', 'PRESENT', 'On time', 2, NOW(), NOW()),
(2, 1, 1, '2024-01-11', 'ABSENT', 'Sick leave', 2, NOW(), NOW()),
(3, 2, 1, '2024-01-10', 'LATE', 'Arrived 10 min late', 2, NOW(), NOW()),
(4, 3, 3, '2024-01-10', 'PRESENT', NULL, 3, NOW(), NOW());

INSERT IGNORE INTO grades (id, student_id, course_id, marks, grade, remarks, graded_by, created_at, updated_at) VALUES
(1, 1, 1, 88.0, 'A', 'Excellent work', 2, NOW(), NOW()),
(2, 1, 2, 74.5, 'B', 'Good understanding', 2, NOW(), NOW()),
(3, 2, 1, 65.0, 'C', 'Needs improvement', 2, NOW(), NOW()),
(4, 3, 3, 91.0, 'A+', 'Outstanding', 3, NOW(), NOW());
