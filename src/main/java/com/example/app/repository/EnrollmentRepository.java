package com.example.app.repository;

import com.example.app.entity.Enrollment;
import com.example.app.entity.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseIdAndStatus(Long studentId, Long courseId, EnrollmentStatus status);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    List<Enrollment> findByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    @Query("SELECT e FROM Enrollment e WHERE " +
            "(:studentId IS NULL OR e.studentId = :studentId) AND " +
            "(:courseId IS NULL OR e.courseId = :courseId) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<Enrollment> search(@Param("studentId") Long studentId, @Param("courseId") Long courseId,
                             @Param("status") EnrollmentStatus status, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.courseId IN (SELECT c.id FROM Course c WHERE c.teacherId = :teacherId) AND " +
            "(:studentId IS NULL OR e.studentId = :studentId) AND " +
            "(:courseId IS NULL OR e.courseId = :courseId) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<Enrollment> searchByTeacherId(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId, @Param("status") EnrollmentStatus status,
                                        Pageable pageable);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseId(Long courseId);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    long countByCourseIdAndStatusAndIdLessThan(Long courseId, EnrollmentStatus status, Long id);

    /**
     * Oldest (first-come-first-served) waitlisted enrollment for a course, used to promote
     * the next student in line whenever a seat becomes available.
     */
    Optional<Enrollment> findFirstByCourseIdAndStatusOrderByIdAsc(Long courseId, EnrollmentStatus status);
}
