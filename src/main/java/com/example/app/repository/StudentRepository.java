package com.example.app.repository;

import com.example.app.entity.Student;
import com.example.app.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUserId(Long userId);

    boolean existsByStudentNumber(String studentNumber);

    boolean existsByEmail(String email);

    @Query("SELECT s FROM Student s WHERE " +
            "(:department IS NULL OR s.department = :department) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:search IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.studentNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Student> search(@Param("department") String department, @Param("status") UserStatus status, @Param("search") String search, Pageable pageable);

    @Query("SELECT DISTINCT s FROM Student s JOIN Enrollment e ON e.studentId = s.id WHERE e.courseId IN " +
            "(SELECT c.id FROM Course c WHERE c.teacherId = :teacherId)")
    List<Student> findStudentsByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT s FROM Student s JOIN Enrollment e ON e.studentId = s.id WHERE e.courseId IN " +
            "(SELECT c.id FROM Course c WHERE c.teacherId = :teacherId) AND " +
            "(:department IS NULL OR s.department = :department) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:search IS NULL OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(s.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Student> searchByTeacherId(@Param("teacherId") Long teacherId, @Param("department") String department,
                                     @Param("status") UserStatus status, @Param("search") String search, Pageable pageable);
}
