package com.example.app.repository;

import com.example.app.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByEmployeeNumber(String employeeNumber);

    boolean existsByEmail(String email);

    @Query("SELECT t FROM Teacher t WHERE " +
            "(:search IS NULL OR LOWER(t.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(t.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Teacher> search(@Param("search") String search, Pageable pageable);
}
