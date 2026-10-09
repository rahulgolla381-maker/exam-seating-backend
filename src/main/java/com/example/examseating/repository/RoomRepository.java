package com.example.examseating.repository;

import com.example.examseating.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByBuildingEntityId(Long buildingId);
    List<Room> findByFloorEntityId(Long floorId);
    List<Room> findByBuilding(String building);
    List<Room> findByStatus(String status);
    Optional<Room> findByRoomNumber(String roomNumber);
    boolean existsByRoomNumber(String roomNumber);
    boolean existsByRoomNumberAndIdNot(String roomNumber, Long id);
    long countByBuildingEntityId(Long buildingId);
    long countByFloorEntityId(Long floorId);

    @Query("SELECT COALESCE(SUM(r.rowsCount * r.columnsCount), 0) FROM Room r WHERE r.buildingEntity.id = :buildingId")
    int sumCapacityByBuildingId(@Param("buildingId") Long buildingId);

    @Query("SELECT COALESCE(SUM(r.rowsCount * r.columnsCount), 0) FROM Room r WHERE r.floorEntity.id = :floorId")
    int sumCapacityByFloorId(@Param("floorId") Long floorId);

    @Query("SELECT COALESCE(SUM(r.rowsCount * r.columnsCount), 0) FROM Room r")
    int sumTotalCapacity();
}
