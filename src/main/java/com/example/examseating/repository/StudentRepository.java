package com.example.examseating.repository;

import com.example.examseating.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByRollNumber(String rollNumber);
    Optional<Student> findByEmail(String email);
    boolean existsBySectionId(Long sectionId);
    List<Student> findBySectionId(Long sectionId);
    long countBySectionId(Long sectionId);
    long countBySectionIdAndStatus(Long sectionId, String status);
    long countByDepartmentId(Long departmentId);
    @Query("SELECT COUNT(s) FROM Student s WHERE s.section.department.id = :departmentId")
    long countBySectionDepartmentId(@Param("departmentId") Long departmentId);
    List<Student> findByDepartmentId(Long departmentId);
    List<Student> findBySectionIdIn(List<Long> sectionIds);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.id IN :studentIds")
    void deleteByIdIn(@Param("studentIds") List<Long> studentIds);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.section.id = :sectionId")
    void deleteBySectionId(@Param("sectionId") Long sectionId);

    @Modifying
    @Query("DELETE FROM Student s WHERE s.department.id = :departmentId")
    void deleteByDepartmentId(@Param("departmentId") Long departmentId);
}
