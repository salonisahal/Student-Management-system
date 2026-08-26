-- Enrollment status now supports ACTIVE, WAITLISTED, and CANCELLED (see EnrollmentStatus).
-- The existing VARCHAR(20) status column already accommodates these values; add an index to
-- efficiently look up a course's waitlist and active-seat count when enforcing capacity limits
-- and promoting waitlisted students.
CREATE INDEX idx_enrollments_course_status ON enrollments(course_id, status);
