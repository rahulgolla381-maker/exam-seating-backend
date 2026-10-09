package com.example.examseating.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "seat_allocations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"exam_id", "student_id"}),
    @UniqueConstraint(columnNames = {"exam_id", "room_id", "seat_number"})
})
public class SeatAllocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "exam_id")
    private Exam exam;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @NotNull
    @Column(name = "row_num")
    private Integer rowNumber;

    @NotNull
    @Column(name = "col_num")
    private Integer columnNumber;

    @NotNull
    private Integer seatNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Exam getExam() { return exam; }
    public void setExam(Exam exam) { this.exam = exam; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public Integer getColumnNumber() { return columnNumber; }
    public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
    public Integer getSeatNumber() { return seatNumber; }
    public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
}
