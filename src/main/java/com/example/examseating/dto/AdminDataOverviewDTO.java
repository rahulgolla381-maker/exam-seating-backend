package com.example.examseating.dto;

import java.util.ArrayList;
import java.util.List;

public class AdminDataOverviewDTO {

    // Total Record Counts
    private long departmentsCount;
    private long sectionsCount;
    private long studentsCount;
    private long buildingsCount;
    private long floorsCount;
    private long roomsCount;
    private long examsCount;
    private long seatAllocationsCount;
    private long attendancesCount;

    // Entity Lists
    private List<DepartmentItem> departments = new ArrayList<>();
    private List<SectionItem> sections = new ArrayList<>();
    private List<StudentItem> students = new ArrayList<>();
    private List<BuildingItem> buildings = new ArrayList<>();
    private List<FloorItem> floors = new ArrayList<>();
    private List<RoomItem> rooms = new ArrayList<>();
    private List<ExamItem> exams = new ArrayList<>();
    private List<AllocationItem> seatAllocations = new ArrayList<>();
    private List<AttendanceItem> attendances = new ArrayList<>();

    public AdminDataOverviewDTO() {}

    // Getters and Setters
    public long getDepartmentsCount() { return departmentsCount; }
    public void setDepartmentsCount(long departmentsCount) { this.departmentsCount = departmentsCount; }

    public long getSectionsCount() { return sectionsCount; }
    public void setSectionsCount(long sectionsCount) { this.sectionsCount = sectionsCount; }

    public long getStudentsCount() { return studentsCount; }
    public void setStudentsCount(long studentsCount) { this.studentsCount = studentsCount; }

    public long getBuildingsCount() { return buildingsCount; }
    public void setBuildingsCount(long buildingsCount) { this.buildingsCount = buildingsCount; }

    public long getFloorsCount() { return floorsCount; }
    public void setFloorsCount(long floorsCount) { this.floorsCount = floorsCount; }

    public long getRoomsCount() { return roomsCount; }
    public void setRoomsCount(long roomsCount) { this.roomsCount = roomsCount; }

    public long getExamsCount() { return examsCount; }
    public void setExamsCount(long examsCount) { this.examsCount = examsCount; }

    public long getSeatAllocationsCount() { return seatAllocationsCount; }
    public void setSeatAllocationsCount(long seatAllocationsCount) { this.seatAllocationsCount = seatAllocationsCount; }

    public long getAttendancesCount() { return attendancesCount; }
    public void setAttendancesCount(long attendancesCount) { this.attendancesCount = attendancesCount; }

    public List<DepartmentItem> getDepartments() { return departments; }
    public void setDepartments(List<DepartmentItem> departments) { this.departments = departments; }

    public List<SectionItem> getSections() { return sections; }
    public void setSections(List<SectionItem> sections) { this.sections = sections; }

    public List<StudentItem> getStudents() { return students; }
    public void setStudents(List<StudentItem> students) { this.students = students; }

    public List<BuildingItem> getBuildings() { return buildings; }
    public void setBuildings(List<BuildingItem> buildings) { this.buildings = buildings; }

    public List<FloorItem> getFloors() { return floors; }
    public void setFloors(List<FloorItem> floors) { this.floors = floors; }

    public List<RoomItem> getRooms() { return rooms; }
    public void setRooms(List<RoomItem> rooms) { this.rooms = rooms; }

    public List<ExamItem> getExams() { return exams; }
    public void setExams(List<ExamItem> exams) { this.exams = exams; }

    public List<AllocationItem> getSeatAllocations() { return seatAllocations; }
    public void setSeatAllocations(List<AllocationItem> seatAllocations) { this.seatAllocations = seatAllocations; }

    public List<AttendanceItem> getAttendances() { return attendances; }
    public void setAttendances(List<AttendanceItem> attendances) { this.attendances = attendances; }

    // Nested Item Classes
    public static class DepartmentItem {
        private Long id;
        private String code;
        private String name;
        private Long sectionCount;
        private Long studentCount;

        public DepartmentItem() {}
        public DepartmentItem(Long id, String code, String name, Long sectionCount, Long studentCount) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.sectionCount = sectionCount;
            this.studentCount = studentCount;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getSectionCount() { return sectionCount; }
        public void setSectionCount(Long sectionCount) { this.sectionCount = sectionCount; }
        public Long getStudentCount() { return studentCount; }
        public void setStudentCount(Long studentCount) { this.studentCount = studentCount; }
    }

    public static class SectionItem {
        private Long id;
        private Long departmentId;
        private String departmentCode;
        private String departmentName;
        private String name;
        private Long actualStudents;
        private Integer expectedStudents;
        private String rollRange;
        private int detainedCount;
        private int eligibleCount;
        private boolean isConfigured;

        public SectionItem() {}
        public SectionItem(Long id, Long departmentId, String departmentCode, String departmentName,
                           String name, Long actualStudents, Integer expectedStudents, String rollRange,
                           int detainedCount, int eligibleCount, boolean isConfigured) {
            this.id = id;
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentName = departmentName;
            this.name = name;
            this.actualStudents = actualStudents;
            this.expectedStudents = expectedStudents;
            this.rollRange = rollRange;
            this.detainedCount = detainedCount;
            this.eligibleCount = eligibleCount;
            this.isConfigured = isConfigured;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getDepartmentId() { return departmentId; }
        public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getDepartmentName() { return departmentName; }
        public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getActualStudents() { return actualStudents; }
        public void setActualStudents(Long actualStudents) { this.actualStudents = actualStudents; }
        public Integer getExpectedStudents() { return expectedStudents; }
        public void setExpectedStudents(Integer expectedStudents) { this.expectedStudents = expectedStudents; }
        public String getRollRange() { return rollRange; }
        public void setRollRange(String rollRange) { this.rollRange = rollRange; }
        public int getDetainedCount() { return detainedCount; }
        public void setDetainedCount(int detainedCount) { this.detainedCount = detainedCount; }
        public int getEligibleCount() { return eligibleCount; }
        public void setEligibleCount(int eligibleCount) { this.eligibleCount = eligibleCount; }
        public boolean isConfigured() { return isConfigured; }
        public void setConfigured(boolean configured) { isConfigured = configured; }
    }

    public static class StudentItem {
        private Long id;
        private String rollNumber;
        private String fullName;
        private String email;
        private Integer semester;
        private Integer year;
        private String departmentCode;
        private String sectionName;
        private String status;

        public StudentItem() {}
        public StudentItem(Long id, String rollNumber, String fullName, String email,
                           Integer semester, Integer year, String departmentCode,
                           String sectionName, String status) {
            this.id = id;
            this.rollNumber = rollNumber;
            this.fullName = fullName;
            this.email = email;
            this.semester = semester;
            this.year = year;
            this.departmentCode = departmentCode;
            this.sectionName = sectionName;
            this.status = status;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRollNumber() { return rollNumber; }
        public void setRollNumber(String rollNumber) { this.rollNumber = rollNumber; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public Integer getSemester() { return semester; }
        public void setSemester(Integer semester) { this.semester = semester; }
        public Integer getYear() { return year; }
        public void setYear(Integer year) { this.year = year; }
        public String getDepartmentCode() { return departmentCode; }
        public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }
        public String getSectionName() { return sectionName; }
        public void setSectionName(String sectionName) { this.sectionName = sectionName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class BuildingItem {
        private Long id;
        private String code;
        private String name;
        private String description;

        public BuildingItem() {}
        public BuildingItem(Long id, String code, String name, String description) {
            this.id = id;
            this.code = code;
            this.name = name;
            this.description = description;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class FloorItem {
        private Long id;
        private String buildingCode;
        private String buildingName;
        private Integer floorNumber;
        private String floorName;

        public FloorItem() {}
        public FloorItem(Long id, String buildingCode, String buildingName, Integer floorNumber, String floorName) {
            this.id = id;
            this.buildingCode = buildingCode;
            this.buildingName = buildingName;
            this.floorNumber = floorNumber;
            this.floorName = floorName;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getBuildingCode() { return buildingCode; }
        public void setBuildingCode(String buildingCode) { this.buildingCode = buildingCode; }
        public String getBuildingName() { return buildingName; }
        public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
        public Integer getFloorNumber() { return floorNumber; }
        public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }
        public String getFloorName() { return floorName; }
        public void setFloorName(String floorName) { this.floorName = floorName; }
    }

    public static class RoomItem {
        private Long id;
        private String roomNumber;
        private String building;
        private String floor;
        private Integer capacity;
        private Integer rowsCount;
        private Integer columnsCount;

        public RoomItem() {}
        public RoomItem(Long id, String roomNumber, String building, String floor,
                        Integer capacity, Integer rowsCount, Integer columnsCount) {
            this.id = id;
            this.roomNumber = roomNumber;
            this.building = building;
            this.floor = floor;
            this.capacity = capacity;
            this.rowsCount = rowsCount;
            this.columnsCount = columnsCount;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public String getBuilding() { return building; }
        public void setBuilding(String building) { this.building = building; }
        public String getFloor() { return floor; }
        public void setFloor(String floor) { this.floor = floor; }
        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
        public Integer getRowsCount() { return rowsCount; }
        public void setRowsCount(Integer rowsCount) { this.rowsCount = rowsCount; }
        public Integer getColumnsCount() { return columnsCount; }
        public void setColumnsCount(Integer columnsCount) { this.columnsCount = columnsCount; }
    }

    public static class ExamItem {
        private Long id;
        private String subjectCode;
        private String subjectName;
        private String examDate;
        private String startTime;
        private String endTime;
        private String session;
        private String status;

        public ExamItem() {}
        public ExamItem(Long id, String subjectCode, String subjectName, String examDate,
                        String startTime, String endTime, String session, String status) {
            this.id = id;
            this.subjectCode = subjectCode;
            this.subjectName = subjectName;
            this.examDate = examDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.session = session;
            this.status = status;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getSubjectName() { return subjectName; }
        public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
        public String getExamDate() { return examDate; }
        public void setExamDate(String examDate) { this.examDate = examDate; }
        public String getStartTime() { return startTime; }
        public void setStartTime(String startTime) { this.startTime = startTime; }
        public String getEndTime() { return endTime; }
        public void setEndTime(String endTime) { this.endTime = endTime; }
        public String getSession() { return session; }
        public void setSession(String session) { this.session = session; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class AllocationItem {
        private Long id;
        private Long examId;
        private String subjectCode;
        private String roomNumber;
        private Integer seatNumber;
        private Integer rowNumber;
        private Integer columnNumber;
        private String studentRoll;
        private String studentName;

        public AllocationItem() {}
        public AllocationItem(Long id, Long examId, String subjectCode, String roomNumber,
                              Integer seatNumber, Integer rowNumber, Integer columnNumber,
                              String studentRoll, String studentName) {
            this.id = id;
            this.examId = examId;
            this.subjectCode = subjectCode;
            this.roomNumber = roomNumber;
            this.seatNumber = seatNumber;
            this.rowNumber = rowNumber;
            this.columnNumber = columnNumber;
            this.studentRoll = studentRoll;
            this.studentName = studentName;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getExamId() { return examId; }
        public void setExamId(Long examId) { this.examId = examId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public Integer getRowNumber() { return rowNumber; }
        public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
        public Integer getColumnNumber() { return columnNumber; }
        public void setColumnNumber(Integer columnNumber) { this.columnNumber = columnNumber; }
        public String getStudentRoll() { return studentRoll; }
        public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
    }

    public static class AttendanceItem {
        private Long id;
        private Long examId;
        private String subjectCode;
        private String roomNumber;
        private Integer seatNumber;
        private String status;
        private String markedAt;
        private String studentRoll;
        private String studentName;

        public AttendanceItem() {}
        public AttendanceItem(Long id, Long examId, String subjectCode, String roomNumber,
                              Integer seatNumber, String status, String markedAt,
                              String studentRoll, String studentName) {
            this.id = id;
            this.examId = examId;
            this.subjectCode = subjectCode;
            this.roomNumber = roomNumber;
            this.seatNumber = seatNumber;
            this.status = status;
            this.markedAt = markedAt;
            this.studentRoll = studentRoll;
            this.studentName = studentName;
        }
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getExamId() { return examId; }
        public void setExamId(Long examId) { this.examId = examId; }
        public String getSubjectCode() { return subjectCode; }
        public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
        public String getRoomNumber() { return roomNumber; }
        public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
        public Integer getSeatNumber() { return seatNumber; }
        public void setSeatNumber(Integer seatNumber) { this.seatNumber = seatNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMarkedAt() { return markedAt; }
        public void setMarkedAt(String markedAt) { this.markedAt = markedAt; }
        public String getStudentRoll() { return studentRoll; }
        public void setStudentRoll(String studentRoll) { this.studentRoll = studentRoll; }
        public String getStudentName() { return studentName; }
        public void setStudentName(String studentName) { this.studentName = studentName; }
    }
}
