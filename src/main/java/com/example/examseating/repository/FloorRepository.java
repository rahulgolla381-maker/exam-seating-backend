package com.example.examseating.repository;

import com.example.examseating.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FloorRepository extends JpaRepository<Floor, Long> {
    List<Floor> findByBuildingIdOrderByFloorNumberAsc(Long buildingId);
    Optional<Floor> findByBuildingIdAndFloorNumber(Long buildingId, Integer floorNumber);
    boolean existsByBuildingIdAndFloorNumber(Long buildingId, Integer floorNumber);
    long countByBuildingId(Long buildingId);
}
