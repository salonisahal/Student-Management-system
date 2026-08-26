package com.example.app.service;

import com.example.app.dto.*;
import com.example.app.entity.*;
import com.example.app.exception.ResourceNotFoundException;
import com.example.app.mapper.CourseMapper;
import com.example.app.mapper.GradeMapper;
import com.example.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradeRepository gradeRepository;

    @Transactional(readOnly = true)
    public AdminDashboardDto getAdminDashboard() {
        long totalStudents = studentRepository.count();
        long totalTeachers = teacherRepository.count();
        long totalCourses = courseRepository.count();
        long activeUsers = userRepository.findAll().stream().filter(u -> u.getStatus() == UserStatus.ACTIVE).count();
        long inactiveUsers = userRepository.count() - activeUsers;

        Map<String, Long> enrollmentStats = enrollmentRepository.findAll().stream()
                .collect(Collectors.groupingBy(e -> e.getStatus().name(), Collectors.counting()));
        Map<String, Long> attendanceStats = attendanceRepository.findAll().stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));
        Map<String, Long> gradeStats = gradeRepository.findAll().stream()
                .collect(Collectors.groupingBy(Grade::getGrade, Collectors.counting()));

        return new AdminDashboardDto(totalStudents, totalTeachers, totalCourses, activeUsers, inactiveUsers,
                enrollmentStats, attendanceStats, gradeStats);
    }

    @Transactional(readOnly = true)
    public TeacherDashboardDto getTeacherDashboard(Long userId) {
        Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for current user"));
        List<Course> courses = courseRepository.findByTeacherId(teacher.getId());
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        long totalStudents = courseIds.stream()
                .flatMap(cid -> enrollmentRepository.findByCourseIdAndStatus(cid, EnrollmentStatus.ACTIVE).stream())
                .map(Enrollment::getStudentId).distinct().count();

        Map<String, Long> attendanceStats = attendanceRepository.findAll().stream()
                .filter(a -> courseIds.contains(a.getCourseId()))
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));

        Map<String, Long> gradeStats = gradeRepository.findAll().stream()
                .filter(g -> courseIds.contains(g.getCourseId()))
                .collect(Collectors.groupingBy(Grade::getGrade, Collectors.counting()));

        return new TeacherDashboardDto(courses.size(), totalStudents, attendanceStats, gradeStats);
    }

    @Transactional(readOnly = true)
    public StudentDashboardDto getStudentDashboard(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found for current user"));

        List<Long> courseIds = enrollmentRepository.findByStudentId(student.getId()).stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .map(Enrollment::getCourseId).collect(Collectors.toList());
        List<CourseDto> courses = courseRepository.findAllById(courseIds).stream()
                .map(CourseMapper::toDto).collect(Collectors.toList());

        List<Attendance> attendanceRecords = attendanceRepository.findByStudentId(student.getId());
        long total = attendanceRecords.size();
        long presentOrLate = attendanceRecords.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double attendancePercentage = total == 0 ? 0.0 : Math.round((presentOrLate * 10000.0) / total) / 100.0;

        List<Grade> grades = gradeRepository.findByStudentId(student.getId());
        List<GradeDto> gradeDtos = grades.stream().map(GradeMapper::toDto).collect(Collectors.toList());

        GradeSummaryDto summary;
        if (grades.isEmpty()) {
            summary = new GradeSummaryDto(0, 0, 0, 0, "N/A");
        } else {
            double avg = grades.stream().mapToDouble(Grade::getMarks).average().orElse(0);
            double max = grades.stream().mapToDouble(Grade::getMarks).max().orElse(0);
            double min = grades.stream().mapToDouble(Grade::getMarks).min().orElse(0);
            summary = new GradeSummaryDto(grades.size(), Math.round(avg * 100) / 100.0, max, min,
                    com.example.app.util.GradeCalculator.calculate(avg));
        }

        return new StudentDashboardDto(courses, attendancePercentage, gradeDtos, summary);
    }
}
