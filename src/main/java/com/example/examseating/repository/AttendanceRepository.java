package com.example.examseating.repository;

import com.example.examseating.entity.Attendance;
import com.example.examseating.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByExamIdOrderBySeatNumberAsc(Long examId);
    List<Attendance> findByExamIdAndRoomIdOrderBySeatNumberAsc(Long examId, Long roomId);
    Optional<Attendance> findByExamIdAndStudentId(Long examId, Long studentId);
    boolean existsByExamIdAndStudentId(Long examId, Long studentId);
    long countByExamId(Long examId);
    long countByExamIdAndStatus(Long examId, AttendanceStatus status);
    boolean existsByRoomId(Long roomId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.section.id = :sectionId")
    long countByStudentSectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT a FROM Attendance a " +
           "JOIN FETCH a.exam " +
           "JOIN FETCH a.student s " +
           "JOIN FETCH a.room " +
           "WHERE s.section.id = :sectionId")
    List<Attendance> findByStudentSectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.department.id = :departmentId")
    long countByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT a FROM Attendance a WHERE a.student.department.id = :departmentId")
    List<Attendance> findByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.student.id IN :studentIds")
    void deleteByStudentIdIn(@Param("studentIds") List<Long> studentIds);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.student.section.id = :sectionId")
    void deleteByStudentSectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.student.department.id = :departmentId")
    void deleteByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("DELETE FROM Attendance a WHERE a.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);
}
