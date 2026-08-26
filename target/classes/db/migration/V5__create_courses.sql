CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code VARCHAR(50) NOT NULL,
    course_name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    credits INT NOT NULL DEFAULT 0,
    department VARCHAR(100),
    teacher_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uq_courses_code UNIQUE (course_code),
    CONSTRAINT fk_courses_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE SET NULL
);
CREATE INDEX idx_courses_department ON courses(department);
CREATE INDEX idx_courses_status ON courses(status);
CREATE INDEX idx_courses_teacher_id ON courses(teacher_id);
