package com.example.app.repository;

import com.example.app.entity.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    List<Enrollment> findByStudentId(Long studentId);

    List<Enrollment> findByCourseId(Long courseId);

    @Query("SELECT e FROM Enrollment e WHERE " +
            "(:studentId IS NULL OR e.studentId = :studentId) AND " +
            "(:courseId IS NULL OR e.courseId = :courseId)")
    Page<Enrollment> search(@Param("studentId") Long studentId, @Param("courseId") Long courseId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e WHERE e.courseId IN (SELECT c.id FROM Course c WHERE c.teacherId = :teacherId) AND " +
            "(:studentId IS NULL OR e.studentId = :studentId) AND " +
            "(:courseId IS NULL OR e.courseId = :courseId)")
    Page<Enrollment> searchByTeacherId(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId, Pageable pageable);

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    long countByCourseId(Long courseId);
}
