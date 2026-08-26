package com.example.app.repository;

import com.example.app.entity.Attendance;
import com.example.app.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByStudentIdAndCourseIdAndAttendanceDate(Long studentId, Long courseId, LocalDate attendanceDate);

    List<Attendance> findByStudentId(Long studentId);

    @Query("SELECT a FROM Attendance a WHERE " +
            "(:studentId IS NULL OR a.studentId = :studentId) AND " +
            "(:courseId IS NULL OR a.courseId = :courseId) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:dateFrom IS NULL OR a.attendanceDate >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.attendanceDate <= :dateTo) AND " +
            "(:status IS NULL OR a.status = :status)")
    Page<Attendance> search(@Param("studentId") Long studentId, @Param("courseId") Long courseId,
                             @Param("date") LocalDate date, @Param("dateFrom") LocalDate dateFrom,
                             @Param("dateTo") LocalDate dateTo, @Param("status") AttendanceStatus status, Pageable pageable);

    @Query("SELECT a FROM Attendance a WHERE a.courseId IN (SELECT c.id FROM Course c WHERE c.teacherId = :teacherId) AND " +
            "(:studentId IS NULL OR a.studentId = :studentId) AND " +
            "(:courseId IS NULL OR a.courseId = :courseId) AND " +
            "(:date IS NULL OR a.attendanceDate = :date) AND " +
            "(:dateFrom IS NULL OR a.attendanceDate >= :dateFrom) AND " +
            "(:dateTo IS NULL OR a.attendanceDate <= :dateTo) AND " +
            "(:status IS NULL OR a.status = :status)")
    Page<Attendance> searchByTeacherId(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId,
                                        @Param("courseId") Long courseId, @Param("date") LocalDate date,
                                        @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,
                                        @Param("status") AttendanceStatus status, Pageable pageable);

    List<Attendance> findByStudentIdAndCourseIdIn(Long studentId, List<Long> courseIds);

    List<Attendance> findByStudentIdAndCourseId(Long studentId, Long courseId);
}
