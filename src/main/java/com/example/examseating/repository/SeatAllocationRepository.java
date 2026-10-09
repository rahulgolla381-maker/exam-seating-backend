package com.example.examseating.repository;

import com.example.examseating.entity.SeatAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeatAllocationRepository extends JpaRepository<SeatAllocation, Long> {

    @Query("SELECT sa FROM SeatAllocation sa " +
           "JOIN FETCH sa.exam " +
           "JOIN FETCH sa.student s " +
           "JOIN FETCH s.department " +
           "JOIN FETCH s.section " +
           "JOIN FETCH sa.room " +
           "WHERE sa.exam.id = :examId")
    List<SeatAllocation> findByExamId(@Param("examId") Long examId);

    @Query("SELECT sa FROM SeatAllocation sa " +
           "JOIN FETCH sa.exam " +
           "JOIN FETCH sa.student s " +
           "JOIN FETCH s.department " +
           "JOIN FETCH s.section " +
           "JOIN FETCH sa.room " +
           "WHERE sa.exam.id = :examId AND sa.room.id = :roomId")
    List<SeatAllocation> findByExamIdAndRoomId(@Param("examId") Long examId, @Param("roomId") Long roomId);

    @Query("SELECT sa FROM SeatAllocation sa " +
           "JOIN FETCH sa.exam " +
           "JOIN FETCH sa.student s " +
           "JOIN FETCH s.department " +
           "JOIN FETCH s.section " +
           "JOIN FETCH sa.room " +
           "WHERE sa.exam.id = :examId AND sa.student.id = :studentId")
    Optional<SeatAllocation> findByExamIdAndStudentId(@Param("examId") Long examId, @Param("studentId") Long studentId);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    boolean existsByRoomId(Long roomId);

    @Query("SELECT COUNT(sa) FROM SeatAllocation sa WHERE sa.student.section.id = :sectionId")
    long countByStudentSectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(sa) FROM SeatAllocation sa WHERE sa.student.id = :studentId")
    long countByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT sa FROM SeatAllocation sa " +
           "JOIN FETCH sa.exam " +
           "JOIN FETCH sa.student s " +
           "JOIN FETCH sa.room " +
           "WHERE s.section.id = :sectionId")
    List<SeatAllocation> findByStudentSectionId(@Param("sectionId") Long sectionId);

    @Query("SELECT COUNT(sa) FROM SeatAllocation sa WHERE sa.student.department.id = :departmentId")
    long countByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT sa FROM SeatAllocation sa WHERE sa.student.department.id = :departmentId")
    List<SeatAllocation> findByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("DELETE FROM SeatAllocation sa WHERE sa.student.id IN :studentIds")
    void deleteByStudentIdIn(@Param("studentIds") List<Long> studentIds);

    @Modifying
    @Query("DELETE FROM SeatAllocation sa WHERE sa.student.section.id = :sectionId")
    void deleteByStudentSectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("DELETE FROM SeatAllocation sa WHERE sa.student.department.id = :departmentId")
    void deleteByStudentDepartmentId(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("DELETE FROM SeatAllocation sa WHERE sa.exam.id = :examId")
    void deleteByExamId(@Param("examId") Long examId);
}
