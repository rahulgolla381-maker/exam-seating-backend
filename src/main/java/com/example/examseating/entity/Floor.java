package com.example.examseating.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "floors", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"building_id", "floor_number"})
})
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @NotNull
    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber; // 0 = Ground Floor, 1 = First Floor, 2 = Second Floor, etc.

    @NotBlank
    @Column(name = "floor_name", nullable = false)
    private String floorName;

    public Floor() {}

    public Floor(Building building, Integer floorNumber, String floorName) {
        this.building = building;
        this.floorNumber = floorNumber;
        this.floorName = floorName;
    }

    public static String getStandardFloorName(int number) {
        switch (number) {
            case 0: return "Ground Floor";
            case 1: return "First Floor";
            case 2: return "Second Floor";
            case 3: return "Third Floor";
            case 4: return "Fourth Floor";
            case 5: return "Fifth Floor";
            case 6: return "Sixth Floor";
            case 7: return "Seventh Floor";
            case 8: return "Eighth Floor";
            default: return "Floor " + number;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public String getFloorName() {
        return floorName;
    }

    public void setFloorName(String floorName) {
        this.floorName = floorName;
    }
}
