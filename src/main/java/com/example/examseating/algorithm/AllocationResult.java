package com.example.examseating.algorithm;
import com.example.examseating.entity.Student;

public class AllocationResult {
    private SeatPosition seatPosition;
    private Student student;

    public AllocationResult(SeatPosition seatPosition, Student student) {
        this.seatPosition = seatPosition;
        this.student = student;
    }

    public SeatPosition getSeatPosition() { return seatPosition; }
    public Student getStudent() { return student; }
}
