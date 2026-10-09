package com.example.examseating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String roomNumber;

    private String building;
    private String floor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "building_entity_id")
    private Building buildingEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "floor_entity_id")
    private Floor floorEntity;

    @Min(1)
    private int rowsCount;

    @Min(1)
    private int columnsCount;

    private String status = "AVAILABLE"; // AVAILABLE, UNAVAILABLE, MAINTENANCE

    @Transient
    public int getCapacity() { return rowsCount * columnsCount; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public Building getBuildingEntity() { return buildingEntity; }
    public void setBuildingEntity(Building buildingEntity) {
        this.buildingEntity = buildingEntity;
        if (buildingEntity != null && (this.building == null || this.building.isBlank())) {
            this.building = buildingEntity.getName();
        }
    }

    public Floor getFloorEntity() { return floorEntity; }
    public void setFloorEntity(Floor floorEntity) {
        this.floorEntity = floorEntity;
        if (floorEntity != null && (this.floor == null || this.floor.isBlank())) {
            this.floor = floorEntity.getFloorName();
        }
    }

    public int getRowsCount() { return rowsCount; }
    public void setRowsCount(int rowsCount) { this.rowsCount = rowsCount; }
    public int getColumnsCount() { return columnsCount; }
    public void setColumnsCount(int columnsCount) { this.columnsCount = columnsCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
