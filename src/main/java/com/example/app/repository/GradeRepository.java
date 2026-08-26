package com.example.app.repository;

import com.example.app.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudentId(Long studentId);

    @Query("SELECT g FROM Grade g WHERE " +
            "(:studentId IS NULL OR g.studentId = :studentId) AND " +
            "(:courseId IS NULL OR g.courseId = :courseId) AND " +
            "(:grade IS NULL OR g.grade = :grade) AND " +
            "(:minMarks IS NULL OR g.marks >= :minMarks) AND " +
            "(:maxMarks IS NULL OR g.marks <= :maxMarks)")
    Page<Grade> search(@Param("studentId") Long studentId, @Param("courseId") Long courseId,
                        @Param("grade") String grade, @Param("minMarks") Double minMarks,
                        @Param("maxMarks") Double maxMarks, Pageable pageable);

    @Query("SELECT g FROM Grade g WHERE g.courseId IN (SELECT c.id FROM Course c WHERE c.teacherId = :teacherId) AND " +
            "(:studentId IS NULL OR g.studentId = :studentId) AND " +
            "(:courseId IS NULL OR g.courseId = :courseId) AND " +
            "(:grade IS NULL OR g.grade = :grade) AND " +
            "(:minMarks IS NULL OR g.marks >= :minMarks) AND " +
            "(:maxMarks IS NULL OR g.marks <= :maxMarks)")
    Page<Grade> searchByTeacherId(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId,
                                   @Param("courseId") Long courseId, @Param("grade") String grade,
                                   @Param("minMarks") Double minMarks, @Param("maxMarks") Double maxMarks, Pageable pageable);
}
