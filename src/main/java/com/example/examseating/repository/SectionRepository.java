package com.example.examseating.repository;

import com.example.examseating.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {
    boolean existsByNameAndDepartmentId(String name, Long departmentId);
    Optional<Section> findByNameAndDepartmentId(String name, Long departmentId);
    List<Section> findByDepartmentId(Long departmentId);
    long countByDepartmentId(Long departmentId);

    @Modifying
    @Query("DELETE FROM Section s WHERE s.id IN :sectionIds")
    void deleteByIdIn(@Param("sectionIds") List<Long> sectionIds);

    @Modifying
    @Query("DELETE FROM Section s WHERE s.department.id = :departmentId")
    void deleteByDepartmentId(@Param("departmentId") Long departmentId);
}
