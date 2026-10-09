package com.example.examseating.algorithm;

import com.example.examseating.entity.Department;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.Section;
import com.example.examseating.entity.Student;
import com.example.examseating.service.SeatAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SeatAllocationServiceTest {

    private SeatAllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new SeatAllocationService();
    }

    private Room createRoom(Long id, int rows, int cols) {
        Room r = new Room();
        r.setId(id);
        r.setRowsCount(rows);
        r.setColumnsCount(cols);
        r.setRoomNumber("ROOM-" + id);
        return r;
    }

    private Student createStudent(String code, String sectionName, String rollNo) {
        Department d = new Department();
        d.setCode(code);
        Section sec = new Section();
        sec.setName(sectionName);

        Student s = new Student();
        s.setDepartment(d);
        s.setSection(sec);
        s.setRollNumber(rollNo);
        s.setFullName("Student " + rollNo);
        return s;
    }

    private List<Student> generateStudents(String dept, String sec, int count, int startIndex) {
        List<Student> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            students.add(createStudent(dept, sec, dept + sec + (startIndex + i)));
        }
        return students;
    }

    private void validateResult(List<AllocationResult> result, int expectedSize) {
        assertEquals(expectedSize, result.size());

        List<String> seenStudents = new ArrayList<>();
        List<String> seenSeats = new ArrayList<>();

        for (AllocationResult r : result) {
            String studentId = r.getStudent().getRollNumber();
            String seatId = r.getSeatPosition().getRoom().getId() + "-" + r.getSeatPosition().getRow() + "-" + r.getSeatPosition().getCol();

            assertFalse(seenStudents.contains(studentId), "Duplicate student allocated: " + studentId);
            assertFalse(seenSeats.contains(seatId), "Duplicate seat allocated: " + seatId);

            seenStudents.add(studentId);
            seenSeats.add(seatId);
        }

        // Check adjacencies
        for (AllocationResult r1 : result) {
            for (AllocationResult r2 : result) {
                if (r1 == r2) continue;
                if (!r1.getSeatPosition().getRoom().getId().equals(r2.getSeatPosition().getRoom().getId())) continue;

                int r1Row = r1.getSeatPosition().getRow();
                int r1Col = r1.getSeatPosition().getCol();
                int r2Row = r2.getSeatPosition().getRow();
                int r2Col = r2.getSeatPosition().getCol();

                boolean isHorizontal = (r1Row == r2Row && Math.abs(r1Col - r2Col) == 1);
                boolean isVertical = (r1Col == r2Col && Math.abs(r1Row - r2Row) == 1);

                if (isHorizontal || isVertical) {
                    String g1 = r1.getStudent().getDepartment().getCode() + "-" + r1.getStudent().getSection().getName();
                    String g2 = r2.getStudent().getDepartment().getCode() + "-" + r2.getStudent().getSection().getName();
                    assertNotEquals(g1, g2, "Adjacent seats have the same group: " + g1 + " at " + r1Row + "," + r1Col + " and " + r2Row + "," + r2Col);
                }
            }
        }
    }

    @Test
    void test1_EnoughSeatsAndBalancedGroups() {
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 10, 1));
        students.addAll(generateStudents("ML", "A", 10, 1));
        
        List<Room> rooms = Arrays.asList(createRoom(1L, 4, 5)); // 20 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 20);
    }

    @Test
    void test2_MoreStudentsThanSeats() {
        List<Student> students = generateStudents("AI", "A", 25, 1);
        List<Room> rooms = Arrays.asList(createRoom(1L, 4, 5)); // 20 seats

        AllocationException ex = assertThrows(AllocationException.class, () -> allocationService.allocateSeating(students, rooms));
        assertTrue(ex.getMessage().contains("Insufficient seating capacity"));
    }

    @Test
    void test3_MultipleDepartmentsAndSections() {
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 5, 1));
        students.addAll(generateStudents("AI", "B", 5, 1));
        students.addAll(generateStudents("ML", "A", 5, 1));
        students.addAll(generateStudents("ML", "B", 5, 1));
        students.addAll(generateStudents("EC", "A", 5, 1));

        List<Room> rooms = Arrays.asList(createRoom(1L, 5, 6)); // 30 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 25);
    }

    @Test
    void test4_TwoGroupsOnlySmallRoom() {
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 2, 1));
        students.addAll(generateStudents("ML", "A", 2, 1));

        List<Room> rooms = Arrays.asList(createRoom(1L, 2, 2)); // 4 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 4);
    }

    @Test
    void test5_LargestGroupDifficultDistribution() {
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 10, 1));
        students.addAll(generateStudents("ML", "A", 3, 1));
        students.addAll(generateStudents("EC", "A", 3, 1));
        students.addAll(generateStudents("CAC", "A", 3, 1));

        // Total 19 students. If room is 4x5=20, it is feasible to separate AI-A (max capacity for 1 group is 10 without neighbors in 4x5)
        List<Room> rooms = Arrays.asList(createRoom(1L, 4, 5));

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 19);
    }

    @Test
    void test10_CapacitySufficientSingleGroupExceedsNonAdjacentCapacity() {
        List<Student> students = new ArrayList<>();
        // 9 students in one group, room is 3x3=9. Exceeds checkerboard capacity (5), but physical capacity is sufficient.
        students.addAll(generateStudents("AI", "A", 9, 1));

        List<Room> rooms = Arrays.asList(createRoom(1L, 3, 3)); 

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        assertNotNull(result);
        assertEquals(9, result.size());

        Set<String> seenStudents = new HashSet<>();
        Set<String> seenSeats = new HashSet<>();
        for (AllocationResult r : result) {
            assertTrue(seenStudents.add(r.getStudent().getRollNumber()), "Duplicate student: " + r.getStudent().getRollNumber());
            String seatKey = r.getSeatPosition().getRoom().getId() + "-" + r.getSeatPosition().getRow() + "-" + r.getSeatPosition().getCol();
            assertTrue(seenSeats.add(seatKey), "Duplicate seat: " + seatKey);
        }
    }

    @Test
    void test_AIB_39Students_PhysicalCapacitySufficient() {
        // AI-B with 39 students in a 45-seat room (5x9)
        // Checkerboard capacity is 23, but physical capacity (45) is sufficient
        List<Student> students = generateStudents("AI", "B", 39, 1);
        List<Room> rooms = Arrays.asList(createRoom(1L, 5, 9)); // 45 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        assertNotNull(result);
        assertEquals(39, result.size());

        Set<String> seenStudents = new HashSet<>();
        Set<String> seenSeats = new HashSet<>();
        for (AllocationResult r : result) {
            assertTrue(seenStudents.add(r.getStudent().getRollNumber()), "Duplicate student: " + r.getStudent().getRollNumber());
            String seatKey = r.getSeatPosition().getRoom().getId() + "-" + r.getSeatPosition().getRow() + "-" + r.getSeatPosition().getCol();
            assertTrue(seenSeats.add(seatKey), "Duplicate seat: " + seatKey);
        }
    }

    @Test
    void test_MultiGroup_ExceedsNonAdjacentCapacity_MinConflicts() {
        // AI-B (30 students) and EC-A (10 students) in a 45-seat room (5x9)
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "B", 30, 1));
        students.addAll(generateStudents("EC", "A", 10, 1));
        List<Room> rooms = Arrays.asList(createRoom(1L, 5, 9)); // 45 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        assertNotNull(result);
        assertEquals(40, result.size());

        Set<String> seenStudents = new HashSet<>();
        Set<String> seenSeats = new HashSet<>();
        for (AllocationResult r : result) {
            assertTrue(seenStudents.add(r.getStudent().getRollNumber()), "Duplicate student: " + r.getStudent().getRollNumber());
            String seatKey = r.getSeatPosition().getRoom().getId() + "-" + r.getSeatPosition().getRow() + "-" + r.getSeatPosition().getCol();
            assertTrue(seenSeats.add(seatKey), "Duplicate seat: " + seatKey);
        }
    }

    @Test
    void test6_SingleGroupWithSufficientCheckerboardCapacity() {
        // Single group: 10 AI-A students in a 5x6 room (30 seats, 15 checkerboard capacity)
        List<Student> students = generateStudents("AI", "A", 10, 1);
        List<Room> rooms = Arrays.asList(createRoom(1L, 5, 6));

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 10);
    }

    @Test
    void test7_MultipleRoomsIndependentAllocation() {
        // 2 rooms of 2x2 (4 seats each, total 8 seats)
        // Students: 4 from AI-A, 4 from ML-A (total 8 students)
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 4, 1));
        students.addAll(generateStudents("ML", "A", 4, 1));

        List<Room> rooms = Arrays.asList(createRoom(1L, 2, 2), createRoom(2L, 2, 2));

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 8);
    }

    @Test
    void test8_RoomBoundaryIndependence() {
        // Test that seat at end of Room 1 and seat at start of Room 2 do not conflict
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 2, 1));
        students.addAll(generateStudents("ML", "A", 2, 1));

        List<Room> rooms = Arrays.asList(createRoom(1L, 1, 2), createRoom(2L, 1, 2));

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        validateResult(result, 4);
    }

    @Test
    void test9_DuplicateStudentValidation() {
        Student s1 = createStudent("AI", "A", "23AI001");
        s1.setId(101L);
        Student s2 = createStudent("AI", "A", "23AI001_DUP");
        s2.setId(101L); // duplicate ID

        List<Student> students = Arrays.asList(s1, s2);
        List<Room> rooms = Arrays.asList(createRoom(1L, 2, 2));

        AllocationException ex = assertThrows(AllocationException.class, () -> allocationService.allocateSeating(students, rooms));
        assertTrue(ex.getMessage().contains("Duplicate students"));
    }

    @Test
    void test11_UnavailableOrMaintenanceRoomValidation() {
        List<Student> students = generateStudents("AI", "A", 2, 1);
        Room r = createRoom(1L, 2, 2);
        r.setStatus("MAINTENANCE");

        AllocationException ex = assertThrows(AllocationException.class, () -> allocationService.allocateSeating(students, Arrays.asList(r)));
        assertTrue(ex.getMessage().contains("MAINTENANCE"));
    }

    @Test
    void test_MultiGroup_AI_A_AI_B_AIML_H_EC_A_Allocation() {
        // Multi-cohort allocation: AI-A (63), AIML-H (56), EC-A (40), AI-B (39) = 198 students
        List<Student> students = new ArrayList<>();
        students.addAll(generateStudents("AI", "A", 63, 1));
        students.addAll(generateStudents("AIML", "H", 56, 1));
        students.addAll(generateStudents("EC", "A", 40, 1));
        students.addAll(generateStudents("AI", "B", 39, 1));

        assertEquals(198, students.size());

        // Rooms totaling 215 seats (e.g. 10 rooms of ~22 seats)
        List<Room> rooms = new ArrayList<>();
        rooms.add(createRoom(1L, 5, 5)); // 25
        rooms.add(createRoom(2L, 5, 5)); // 25
        rooms.add(createRoom(3L, 5, 5)); // 25
        rooms.add(createRoom(4L, 5, 5)); // 25
        rooms.add(createRoom(5L, 5, 5)); // 25
        rooms.add(createRoom(6L, 5, 5)); // 25
        rooms.add(createRoom(7L, 5, 5)); // 25
        rooms.add(createRoom(8L, 4, 5)); // 20
        rooms.add(createRoom(9L, 4, 5)); // 20
        // Total = 215 seats

        List<AllocationResult> result = allocationService.allocateSeating(students, rooms);
        assertNotNull(result);
        assertEquals(198, result.size());

        Set<String> seenStudents = new HashSet<>();
        Set<String> seenSeats = new HashSet<>();
        for (AllocationResult ar : result) {
            assertTrue(seenStudents.add(ar.getStudent().getRollNumber()),
                    "Duplicate student: " + ar.getStudent().getRollNumber());
            String seatKey = ar.getSeatPosition().getRoom().getId() + "-" +
                    ar.getSeatPosition().getRow() + "-" + ar.getSeatPosition().getCol();
            assertTrue(seenSeats.add(seatKey), "Duplicate seat: " + seatKey);
        }

        // Count same Department + Section adjacencies
        int sameGroupConflicts = 0;
        for (AllocationResult r1 : result) {
            for (AllocationResult r2 : result) {
                if (r1 == r2) continue;
                if (!r1.getSeatPosition().getRoom().getId().equals(r2.getSeatPosition().getRoom().getId())) continue;

                int r1Row = r1.getSeatPosition().getRow();
                int r1Col = r1.getSeatPosition().getCol();
                int r2Row = r2.getSeatPosition().getRow();
                int r2Col = r2.getSeatPosition().getCol();

                boolean isAdjacent = (r1Row == r2Row && Math.abs(r1Col - r2Col) == 1) ||
                                     (r1Col == r2Col && Math.abs(r1Row - r2Row) == 1);
                if (isAdjacent) {
                    String g1 = r1.getStudent().getDepartment().getCode() + "-" + r1.getStudent().getSection().getName();
                    String g2 = r2.getStudent().getDepartment().getCode() + "-" + r2.getStudent().getSection().getName();
                    if (g1.equals(g2)) {
                        sameGroupConflicts++;
                    }
                }
            }
        }
        // Each undirected edge is counted twice in double loop
        int uniqueConflicts = sameGroupConflicts / 2;
        // With interleaving across 4 distinct groups, same-group conflicts should be minimized (<= 5)
        assertTrue(uniqueConflicts <= 5, "Expected <= 5 same group conflicts but got: " + uniqueConflicts);
    }
}
