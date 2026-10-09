package com.example.examseating.repository;

import com.example.examseating.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByCode(String code);
    Optional<Building> findByName(String name);
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, Long id);
}
