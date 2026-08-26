CREATE TABLE course_prerequisites (
    course_id BIGINT NOT NULL,
    prerequisite_course_id BIGINT NOT NULL,
    PRIMARY KEY (course_id, prerequisite_course_id),
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT fk_prereq_prerequisite FOREIGN KEY (prerequisite_course_id) REFERENCES courses(id) ON DELETE CASCADE,
    CONSTRAINT chk_prereq_not_self CHECK (course_id <> prerequisite_course_id)
);
CREATE INDEX idx_course_prerequisites_prereq ON course_prerequisites(prerequisite_course_id);
