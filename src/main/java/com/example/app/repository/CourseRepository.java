package com.example.app.repository;

import com.example.app.entity.Course;
import com.example.app.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCourseCode(String courseCode);

    boolean existsByCourseCode(String courseCode);

    List<Course> findByTeacherId(Long teacherId);

    long countByTeacherId(Long teacherId);

    @Query("SELECT c FROM Course c WHERE " +
            "(:department IS NULL OR c.department = :department) AND " +
            "(:status IS NULL OR c.status = :status) AND " +
            "(:teacherId IS NULL OR c.teacherId = :teacherId) AND " +
            "(:search IS NULL OR LOWER(c.courseName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Course> search(@Param("department") String department, @Param("status") UserStatus status,
                         @Param("teacherId") Long teacherId, @Param("search") String search, Pageable pageable);
}
