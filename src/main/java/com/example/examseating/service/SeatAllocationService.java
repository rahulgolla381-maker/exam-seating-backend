package com.example.examseating.service;

import com.example.examseating.algorithm.*;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.Student;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SeatAllocationService {

    private static final int MAX_ITERATIONS = 100000;
    private int iterations;

    public List<AllocationResult> allocateSeating(List<Student> students, List<Room> rooms) {
        if (students == null || students.isEmpty()) {
            throw new AllocationException("No students provided for allocation.");
        }
        if (rooms == null || rooms.isEmpty()) {
            throw new AllocationException("No rooms provided for allocation.");
        }

        // Validate duplicates
        Set<Long> seenStudentIds = new HashSet<>();
        for (Student s : students) {
            if (s.getId() != null && !seenStudentIds.add(s.getId())) {
                throw new AllocationException("Duplicate students provided for allocation: Student ID " + s.getId());
            }
        }

        Set<Long> seenRoomIds = new HashSet<>();
        for (Room r : rooms) {
            if (r.getId() != null && !seenRoomIds.add(r.getId())) {
                throw new AllocationException("Duplicate rooms provided for allocation: Room ID " + r.getId());
            }
        }

        // Room status validation
        for (Room r : rooms) {
            if (r.getStatus() != null && !"AVAILABLE".equalsIgnoreCase(r.getStatus())) {
                throw new AllocationException("Room " + r.getRoomNumber() + " is " + r.getStatus() + " and cannot be used for examination.");
            }
        }

        int totalCapacity = rooms.stream()
                .mapToInt(r -> r.getCapacity() > 0 ? r.getCapacity() : r.getRowsCount() * r.getColumnsCount())
                .sum();
        if (students.size() > totalCapacity) {
            throw new AllocationException("Insufficient seating capacity. Selected rooms provide " + totalCapacity + " seats, but " + students.size() + " students were selected.");
        }

        // Group students by Department-Section
        Map<String, BranchGroup> groups = new LinkedHashMap<>();
        for (Student s : students) {
            String deptCode = (s.getDepartment() != null && s.getDepartment().getCode() != null) ? s.getDepartment().getCode() : "DEPT";
            String secName = (s.getSection() != null && s.getSection().getName() != null) ? s.getSection().getName() : "SEC";
            String groupName = deptCode + "-" + secName;
            groups.putIfAbsent(groupName, new BranchGroup(groupName));
            groups.get(groupName).addStudent(s);
        }

        // Theoretical maximum non-adjacent capacity across all rooms
        int maxNonAdjacentCapacity = 0;
        for (Room r : rooms) {
            maxNonAdjacentCapacity += (r.getRowsCount() * r.getColumnsCount() + 1) / 2;
        }

        BranchGroup largest = groups.values().stream()
                .max(Comparator.comparingInt(BranchGroup::getRemaining))
                .orElse(null);

        List<AllocationResult> result = null;

        // PRIMARY OBJECTIVE: Attempt zero-conflict allocation if largest group fits non-adjacent capacity
        if (largest == null || largest.getRemaining() <= maxNonAdjacentCapacity) {
            // Single group scenario
            if (groups.size() == 1) {
                List<AllocationResult> singleResult = trySingleGroupCheckerboard(students, rooms);
                if (singleResult != null && singleResult.size() == students.size()) {
                    result = singleResult;
                }
            } else if (students.size() <= 40) {
                // Multi-group: Strategy 1 - contiguous seating (standard compact distribution)
                List<AllocationResult> contigResult = tryContiguousAllocation(students, rooms, groups);
                if (contigResult != null && contigResult.size() == students.size()) {
                    result = contigResult;
                } else {
                    // Multi-group: Strategy 2 - checkerboard spaced seating across room grids
                    List<AllocationResult> spacedResult = trySpacedAllocation(students, rooms, groups);
                    if (spacedResult != null && spacedResult.size() == students.size()) {
                        result = spacedResult;
                    }
                }
            }
        }

        // SECONDARY OBJECTIVE: If perfect non-adjacency is impossible or zero-conflict search
        // did not find a 100% complete solution, ALWAYS generate an arrangement with the MINIMUM possible conflicts
        if (result == null || result.size() != students.size()) {
            result = allocateWithMinimumConflicts(students, rooms, groups);
        }

        // Final verification before returning to application service
        if (result == null || result.size() != students.size()) {
            throw new AllocationException(String.format(
                "Seating allocation failure: Selected %d students, but algorithm generated %d seats.",
                students.size(), result == null ? 0 : result.size()
            ));
        }

        Set<Object> allocatedStudentKeys = new HashSet<>();
        for (AllocationResult ar : result) {
            if (ar.getStudent() == null) {
                throw new AllocationException("Algorithm generated an allocation with a missing student.");
            }
            Object key = ar.getStudent().getId() != null ? ar.getStudent().getId() :
                    (ar.getStudent().getRollNumber() != null ? ar.getStudent().getRollNumber() : ar.getStudent());
            if (!allocatedStudentKeys.add(key)) {
                throw new AllocationException("Algorithm generated a duplicate allocation for student: " + key);
            }
        }

        return result;
    }

    private List<AllocationResult> trySingleGroupCheckerboard(List<Student> students, List<Room> rooms) {
        List<AllocationResult> result = new ArrayList<>();
        int studentIdx = 0;

        for (Room r : rooms) {
            int rows = r.getRowsCount();
            int cols = r.getColumnsCount();

            for (int i = 0; i < rows && studentIdx < students.size(); i++) {
                for (int j = 0; j < cols && studentIdx < students.size(); j++) {
                    if ((i + j) % 2 == 0) {
                        int seatNum = i * cols + j + 1;
                        SeatPosition pos = new SeatPosition(r, i, j, seatNum);
                        result.add(new AllocationResult(pos, students.get(studentIdx++)));
                    }
                }
            }
            if (studentIdx >= students.size()) break;
        }

        if (result.size() < students.size()) {
            return null;
        }
        return result;
    }

    private List<AllocationResult> tryContiguousAllocation(List<Student> students, List<Room> rooms, Map<String, BranchGroup> groups) {
        PriorityQueue<BranchGroup> maxHeap = new PriorityQueue<>();
        for (BranchGroup bg : groups.values()) {
            BranchGroup copy = new BranchGroup(bg.getGroupName());
            for (Student s : bg.getStudents()) {
                copy.addStudent(s);
            }
            maxHeap.add(copy);
        }

        List<SeatPosition> availableSeats = new ArrayList<>();
        Map<Long, String[][]> roomGrids = new HashMap<>();

        for (Room r : rooms) {
            int seatCounter = 1;
            roomGrids.put(r.getId(), new String[r.getRowsCount()][r.getColumnsCount()]);
            for (int i = 0; i < r.getRowsCount(); i++) {
                for (int j = 0; j < r.getColumnsCount(); j++) {
                    availableSeats.add(new SeatPosition(r, i, j, seatCounter++));
                    if (availableSeats.size() == students.size()) break;
                }
                if (availableSeats.size() == students.size()) break;
            }
            if (availableSeats.size() == students.size()) break;
        }

        List<AllocationResult> result = new ArrayList<>();
        this.iterations = 0;
        if (backtrack(0, availableSeats, maxHeap, roomGrids, result, students.size())) {
            return result;
        }
        return null;
    }

    private List<AllocationResult> trySpacedAllocation(List<Student> students, List<Room> rooms, Map<String, BranchGroup> groups) {
        PriorityQueue<BranchGroup> maxHeap = new PriorityQueue<>();
        for (BranchGroup bg : groups.values()) {
            BranchGroup copy = new BranchGroup(bg.getGroupName());
            for (Student s : bg.getStudents()) {
                copy.addStudent(s);
            }
            maxHeap.add(copy);
        }

        // Collect even parity seats first across rooms, then odd parity seats
        List<SeatPosition> evenSeats = new ArrayList<>();
        List<SeatPosition> oddSeats = new ArrayList<>();
        Map<Long, String[][]> roomGrids = new HashMap<>();

        for (Room r : rooms) {
            roomGrids.put(r.getId(), new String[r.getRowsCount()][r.getColumnsCount()]);
            for (int i = 0; i < r.getRowsCount(); i++) {
                for (int j = 0; j < r.getColumnsCount(); j++) {
                    int seatNum = i * r.getColumnsCount() + j + 1;
                    SeatPosition pos = new SeatPosition(r, i, j, seatNum);
                    if ((i + j) % 2 == 0) {
                        evenSeats.add(pos);
                    } else {
                        oddSeats.add(pos);
                    }
                }
            }
        }

        List<SeatPosition> availableSeats = new ArrayList<>();
        availableSeats.addAll(evenSeats);
        availableSeats.addAll(oddSeats);

        if (availableSeats.size() > students.size()) {
            availableSeats = availableSeats.subList(0, students.size());
        }

        List<AllocationResult> result = new ArrayList<>();
        this.iterations = 0;
        if (backtrack(0, availableSeats, maxHeap, roomGrids, result, students.size())) {
            return result;
        }
        return null;
    }

    private boolean backtrack(int seatIdx, List<SeatPosition> seats, PriorityQueue<BranchGroup> maxHeap,
                              Map<Long, String[][]> roomGrids, List<AllocationResult> result, int totalStudents) {

        if (seatIdx == totalStudents) {
            return true;
        }

        iterations++;
        if (iterations > MAX_ITERATIONS) {
            return false;
        }

        SeatPosition pos = seats.get(seatIdx);
        Long roomId = pos.getRoom().getId();
        int r = pos.getRow();
        int c = pos.getCol();
        int rows = pos.getRoom().getRowsCount();
        int cols = pos.getRoom().getColumnsCount();
        String[][] grid = roomGrids.get(roomId);

        String leftGroup = (c > 0) ? grid[r][c - 1] : null;
        String rightGroup = (c < cols - 1) ? grid[r][c + 1] : null;
        String topGroup = (r > 0) ? grid[r - 1][c] : null;
        String bottomGroup = (r < rows - 1) ? grid[r + 1][c] : null;

        List<BranchGroup> temp = new ArrayList<>();
        while (!maxHeap.isEmpty()) {
            temp.add(maxHeap.poll());
        }

        for (int i = 0; i < temp.size(); i++) {
            BranchGroup bg = temp.get(i);
            String groupName = bg.getGroupName();

            if (!groupName.equals(leftGroup) && !groupName.equals(rightGroup)
                    && !groupName.equals(topGroup) && !groupName.equals(bottomGroup)) {

                Student s = bg.popStudent();
                grid[r][c] = groupName;
                result.add(new AllocationResult(pos, s));

                PriorityQueue<BranchGroup> nextHeap = new PriorityQueue<>();
                for (BranchGroup group : temp) {
                    if (group.getRemaining() > 0) {
                        nextHeap.offer(group);
                    }
                }

                if (backtrack(seatIdx + 1, seats, nextHeap, roomGrids, result, totalStudents)) {
                    return true;
                }

                // Undo
                bg.pushStudent(s);
                grid[r][c] = null;
                result.remove(result.size() - 1);
            }
        }

        return false;
    }

    private List<AllocationResult> allocateWithMinimumConflicts(List<Student> students, List<Room> rooms, Map<String, BranchGroup> groups) {
        // Build graph of all seats across all rooms
        List<SeatNode> allSeats = new ArrayList<>();
        int seatIndexCounter = 0;

        for (Room r : rooms) {
            int rows = r.getRowsCount();
            int cols = r.getColumnsCount();
            SeatNode[][] grid = new SeatNode[rows][cols];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    int seatNum = i * cols + j + 1;
                    SeatPosition pos = new SeatPosition(r, i, j, seatNum);
                    SeatNode node = new SeatNode(pos, seatIndexCounter++);
                    grid[i][j] = node;
                    allSeats.add(node);
                }
            }

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    SeatNode node = grid[i][j];
                    if (i > 0) node.neighbors.add(grid[i - 1][j]);
                    if (i < rows - 1) node.neighbors.add(grid[i + 1][j]);
                    if (j > 0) node.neighbors.add(grid[i][j - 1]);
                    if (j < cols - 1) node.neighbors.add(grid[i][j + 1]);
                }
            }
        }

        // Group students directly from input to guarantee exact set and count
        Map<String, List<Student>> groupMap = new LinkedHashMap<>();
        for (Student s : students) {
            String deptCode = (s.getDepartment() != null && s.getDepartment().getCode() != null) ? s.getDepartment().getCode() : "DEPT";
            String secName = (s.getSection() != null && s.getSection().getName() != null) ? s.getSection().getName() : "SEC";
            String groupName = deptCode + "-" + secName;
            groupMap.computeIfAbsent(groupName, k -> new ArrayList<>()).add(s);
        }

        List<List<Student>> groupLists = new ArrayList<>(groupMap.values());
        groupLists.sort(Comparator.comparingInt(List<Student>::size).reversed());

        // Interleave students across groups round-robin (e.g. AI-A, AIML-H, EC-A, AI-B)
        List<Student> orderedStudents = new ArrayList<>();
        int maxGroupSize = 0;
        for (List<Student> gList : groupLists) {
            maxGroupSize = Math.max(maxGroupSize, gList.size());
        }
        for (int i = 0; i < maxGroupSize; i++) {
            for (List<Student> gList : groupLists) {
                if (i < gList.size()) {
                    orderedStudents.add(gList.get(i));
                }
            }
        }

        // Greedy initial placement minimizing local conflict penalty
        Set<SeatNode> unoccupied = new HashSet<>(allSeats);

        for (Student s : orderedStudents) {
            SeatNode bestSeat = null;
            int bestPenalty = Integer.MAX_VALUE;
            int bestParity = 1;
            int bestDegree = Integer.MAX_VALUE;

            for (SeatNode seat : unoccupied) {
                int penalty = 0;
                for (SeatNode neighbor : seat.neighbors) {
                    if (neighbor.assignedStudent != null) {
                        penalty += getConflictPenalty(s, neighbor.assignedStudent);
                    }
                }

                // Tie breaking: min penalty, then parity 0, then min neighbor degree
                if (penalty < bestPenalty ||
                    (penalty == bestPenalty && seat.parity < bestParity) ||
                    (penalty == bestPenalty && seat.parity == bestParity && seat.neighbors.size() < bestDegree)) {
                    bestPenalty = penalty;
                    bestSeat = seat;
                    bestParity = seat.parity;
                    bestDegree = seat.neighbors.size();
                }
            }

            if (bestSeat != null) {
                bestSeat.assignedStudent = s;
                unoccupied.remove(bestSeat);
            }
        }

        // Min-conflicts local search to further minimize or eliminate conflicts
        int currentConflicts = calculateTotalConflicts(allSeats);
        if (currentConflicts > 0) {
            // Track best assignment found
            int bestConflicts = currentConflicts;
            Map<SeatNode, Student> bestAssignment = captureAssignment(allSeats);

            Random rng = new Random(42);
            int maxSearchIterations = 5000;

            for (int iter = 0; iter < maxSearchIterations && bestConflicts > 0; iter++) {
                // Find all occupied seats that have conflicts
                List<SeatNode> conflictingSeats = new ArrayList<>();
                for (SeatNode seat : allSeats) {
                    if (seat.assignedStudent != null && getSeatConflict(seat) > 0) {
                        conflictingSeats.add(seat);
                    }
                }

                if (conflictingSeats.isEmpty()) {
                    bestConflicts = 0;
                    bestAssignment = captureAssignment(allSeats);
                    break;
                }

                // Pick a conflicting seat randomly
                SeatNode u = conflictingSeats.get(rng.nextInt(conflictingSeats.size()));
                Student su = u.assignedStudent;

                int bestDelta = 0;
                SeatNode bestEmptyTarget = null;
                SeatNode bestSwapTarget = null;

                // 1. Evaluate moving su to an empty seat
                List<SeatNode> emptySeats = new ArrayList<>();
                for (SeatNode seat : allSeats) {
                    if (seat.assignedStudent == null) {
                        emptySeats.add(seat);
                    }
                }

                int uOldConflict = getSeatConflict(u);

                for (SeatNode e : emptySeats) {
                    int eNewConflict = 0;
                    for (SeatNode neighbor : e.neighbors) {
                        if (neighbor.assignedStudent != null && neighbor != u) {
                            eNewConflict += getConflictPenalty(su, neighbor.assignedStudent);
                        }
                    }
                    int delta = eNewConflict - uOldConflict;
                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestEmptyTarget = e;
                        bestSwapTarget = null;
                    }
                }

                // 2. Evaluate swapping su with another occupied seat w
                List<SeatNode> occupiedSeats = new ArrayList<>();
                for (SeatNode seat : allSeats) {
                    if (seat.assignedStudent != null && seat != u) {
                        occupiedSeats.add(seat);
                    }
                }

                int sampleSize = Math.min(occupiedSeats.size(), 40);
                if (occupiedSeats.size() > sampleSize) {
                    Collections.shuffle(occupiedSeats, rng);
                }

                for (int i = 0; i < sampleSize; i++) {
                    SeatNode w = occupiedSeats.get(i);
                    Student sw = w.assignedStudent;

                    // Only consider swap if students belong to different groups
                    if (isSameGroup(su, sw)) {
                        continue;
                    }

                    boolean areNeighbors = u.neighbors.contains(w);
                    int oldEdgePenalty = areNeighbors ? getConflictPenalty(su, sw) : 0;
                    int oldSum = getSeatConflict(u) + getSeatConflict(w) - oldEdgePenalty;

                    // Temporary swap
                    u.assignedStudent = sw;
                    w.assignedStudent = su;

                    int newEdgePenalty = areNeighbors ? getConflictPenalty(sw, su) : 0;
                    int newSum = getSeatConflict(u) + getSeatConflict(w) - newEdgePenalty;
                    int delta = newSum - oldSum;

                    // Revert swap
                    u.assignedStudent = su;
                    w.assignedStudent = sw;

                    if (delta < bestDelta) {
                        bestDelta = delta;
                        bestSwapTarget = w;
                        bestEmptyTarget = null;
                    }
                }

                // Apply move or swap
                if (bestDelta < 0) {
                    if (bestEmptyTarget != null) {
                        bestEmptyTarget.assignedStudent = su;
                        u.assignedStudent = null;
                    } else if (bestSwapTarget != null) {
                        u.assignedStudent = bestSwapTarget.assignedStudent;
                        bestSwapTarget.assignedStudent = su;
                    }
                    currentConflicts += bestDelta;

                    if (currentConflicts < bestConflicts) {
                        bestConflicts = currentConflicts;
                        bestAssignment = captureAssignment(allSeats);
                    }
                } else if (rng.nextDouble() < 0.10 && !emptySeats.isEmpty()) {
                    // Plateau exploration / random perturbation
                    SeatNode randomEmpty = emptySeats.get(rng.nextInt(emptySeats.size()));
                    int eNewConflict = 0;
                    for (SeatNode neighbor : randomEmpty.neighbors) {
                        if (neighbor.assignedStudent != null && neighbor != u) {
                            eNewConflict += getConflictPenalty(su, neighbor.assignedStudent);
                        }
                    }
                    int delta = eNewConflict - uOldConflict;
                    randomEmpty.assignedStudent = su;
                    u.assignedStudent = null;
                    currentConflicts += delta;

                    if (currentConflicts < bestConflicts) {
                        bestConflicts = currentConflicts;
                        bestAssignment = captureAssignment(allSeats);
                    }
                }
            }

            // Restore the best assignment found
            for (SeatNode seat : allSeats) {
                seat.assignedStudent = bestAssignment.get(seat);
            }
        }

        // Build and return AllocationResult list
        List<AllocationResult> result = new ArrayList<>();
        for (SeatNode seat : allSeats) {
            if (seat.assignedStudent != null) {
                result.add(new AllocationResult(seat.pos, seat.assignedStudent));
            }
        }

        if (result.size() != students.size()) {
            throw new AllocationException(String.format(
                "Algorithm failure: Expected %d allocations but generated %d.",
                students.size(), result.size()
            ));
        }

        return result;
    }

    private static class SeatNode {
        final SeatPosition pos;
        final int index;
        final int parity;
        final List<SeatNode> neighbors = new ArrayList<>(4);
        Student assignedStudent;

        SeatNode(SeatPosition pos, int index) {
            this.pos = pos;
            this.index = index;
            this.parity = (pos.getRow() + pos.getCol()) % 2;
        }
    }

    private static int getConflictPenalty(Student s1, Student s2) {
        if (s1 == null || s2 == null) {
            return 0;
        }
        String dept1 = (s1.getDepartment() != null && s1.getDepartment().getCode() != null) ? s1.getDepartment().getCode().trim() : "";
        String dept2 = (s2.getDepartment() != null && s2.getDepartment().getCode() != null) ? s2.getDepartment().getCode().trim() : "";

        String sec1 = (s1.getSection() != null && s1.getSection().getName() != null) ? s1.getSection().getName().trim() : "";
        String sec2 = (s2.getSection() != null && s2.getSection().getName() != null) ? s2.getSection().getName().trim() : "";

        boolean sameDept = !dept1.isEmpty() && dept1.equalsIgnoreCase(dept2);
        boolean sameSec = !sec1.isEmpty() && sec1.equalsIgnoreCase(sec2);

        if (sameDept && sameSec) {
            return 1000;
        } else if (sameDept) {
            return 10;
        } else {
            return 0;
        }
    }

    private static boolean isSameGroup(Student s1, Student s2) {
        if (s1 == null || s2 == null) return false;
        String g1 = ((s1.getDepartment() != null && s1.getDepartment().getCode() != null) ? s1.getDepartment().getCode().trim() : "") + "-" +
                    ((s1.getSection() != null && s1.getSection().getName() != null) ? s1.getSection().getName().trim() : "");
        String g2 = ((s2.getDepartment() != null && s2.getDepartment().getCode() != null) ? s2.getDepartment().getCode().trim() : "") + "-" +
                    ((s2.getSection() != null && s2.getSection().getName() != null) ? s2.getSection().getName().trim() : "");
        return g1.equalsIgnoreCase(g2);
    }

    private static int getSeatConflict(SeatNode u) {
        if (u.assignedStudent == null) return 0;
        int conflict = 0;
        for (SeatNode neighbor : u.neighbors) {
            if (neighbor.assignedStudent != null) {
                conflict += getConflictPenalty(u.assignedStudent, neighbor.assignedStudent);
            }
        }
        return conflict;
    }

    private static int calculateTotalConflicts(List<SeatNode> allSeats) {
        int total = 0;
        for (SeatNode u : allSeats) {
            if (u.assignedStudent == null) continue;
            for (SeatNode v : u.neighbors) {
                if (u.index < v.index && v.assignedStudent != null) {
                    total += getConflictPenalty(u.assignedStudent, v.assignedStudent);
                }
            }
        }
        return total;
    }

    private static Map<SeatNode, Student> captureAssignment(List<SeatNode> allSeats) {
        Map<SeatNode, Student> map = new HashMap<>();
        for (SeatNode seat : allSeats) {
            map.put(seat, seat.assignedStudent);
        }
        return map;
    }
}
