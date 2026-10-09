package com.example.examseating.service;

import com.example.examseating.dto.BatchSectionCreateDTO;
import com.example.examseating.dto.SectionConfigDTO;
import com.example.examseating.dto.SectionDetailsDTO;
import com.example.examseating.entity.Attendance;
import com.example.examseating.entity.Department;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.entity.Section;
import com.example.examseating.entity.Student;
import com.example.examseating.repository.AttendanceRepository;
import com.example.examseating.repository.DepartmentRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import com.example.examseating.repository.SectionRepository;
import com.example.examseating.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SectionService {

    private static final Pattern ROLL_PATTERN = Pattern.compile("^(.*?)([0-9]+)$");

    @Autowired
    private SectionRepository repository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public Section enrichWithActualCounts(Section section) {
        if (section == null || section.getId() == null) return section;
        long total = studentRepository.countBySectionId(section.getId());
        long active = studentRepository.countBySectionIdAndStatus(section.getId(), "ACTIVE");
        section.setActualStudentCount(total);
        section.setActualActiveStudentCount(active);
        return section;
    }

    public List<Section> getAll() {
        List<Section> list = repository.findAll();
        for (Section s : list) {
            enrichWithActualCounts(s);
        }
        return list;
    }

    public Section getById(Long id) {
        Section s = repository.findById(id).orElse(null);
        return enrichWithActualCounts(s);
    }

    public List<Section> getByDepartmentId(Long departmentId) {
        List<Section> list = repository.findByDepartmentId(departmentId);
        for (Section s : list) {
            enrichWithActualCounts(s);
        }
        return list;
    }

    @Transactional
    public Section save(Section entity) {
        if (entity.getName() == null || entity.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Section name is required.");
        }
        String cleanName = entity.getName().trim();
        entity.setName(cleanName);

        if (entity.getDepartment() == null || entity.getDepartment().getId() == null) {
            throw new IllegalArgumentException("Department is required.");
        }
        Department dept = departmentRepository.findById(entity.getDepartment().getId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));
        entity.setDepartment(dept);

        // Duplicate section name prevention within the same department
        if (entity.getId() == null) {
            if (repository.existsByNameAndDepartmentId(cleanName, dept.getId())) {
                throw new IllegalArgumentException("Section '" + cleanName + "' already exists in department '" + dept.getName() + "'.");
            }
        } else {
            Optional<Section> existing = repository.findByNameAndDepartmentId(cleanName, dept.getId());
            if (existing.isPresent() && !existing.get().getId().equals(entity.getId())) {
                throw new IllegalArgumentException("Section '" + cleanName + "' already exists in department '" + dept.getName() + "'.");
            }
        }

        // Validate roll number range if provided
        boolean hasStart = entity.getStartRollNumber() != null && !entity.getStartRollNumber().trim().isEmpty();
        boolean hasEnd = entity.getEndRollNumber() != null && !entity.getEndRollNumber().trim().isEmpty();

        if (hasStart || hasEnd) {
            if (!hasStart || !hasEnd) {
                throw new IllegalArgumentException("Both starting and ending roll numbers are required.");
            }
            validatePattern(entity.getStartRollNumber(), entity.getEndRollNumber());
            String cleanStart = entity.getStartRollNumber().trim().toUpperCase();
            String cleanEnd = entity.getEndRollNumber().trim().toUpperCase();
            validateNoRollNumberConflicts(entity, cleanStart, cleanEnd);
            entity.setStartRollNumber(cleanStart);
            entity.setEndRollNumber(cleanEnd);
            entity.setExpectedStudentCount(calculateExpectedCount(cleanStart, cleanEnd));

            // Validate detained roll numbers against range
            List<String> detained = entity.getDetainedList();
            if (!detained.isEmpty()) {
                RollPatternInfo startInfo = parseRollNumber(cleanStart);
                RollPatternInfo endInfo = parseRollNumber(cleanEnd);
                List<String> validatedDetained = new ArrayList<>();

                for (String rawRoll : detained) {
                    if (rawRoll == null || rawRoll.trim().isEmpty()) continue;
                    String roll = rawRoll.trim().toUpperCase();
                    RollPatternInfo info = parseRollNumber(roll);

                    if (!info.getPrefix().equalsIgnoreCase(startInfo.getPrefix())) {
                        throw new IllegalArgumentException("Detained roll number '" + roll + "' prefix does not match section prefix '" + startInfo.getPrefix() + "'.");
                    }
                    if (info.getNumber() < startInfo.getNumber() || info.getNumber() > endInfo.getNumber()) {
                        throw new IllegalArgumentException("Detained roll number '" + roll + "' is outside section range [" + cleanStart + " - " + cleanEnd + "].");
                    }
                    if (validatedDetained.contains(roll)) {
                        throw new IllegalArgumentException("Duplicate detained roll number: " + roll);
                    }
                    validatedDetained.add(roll);
                }
                entity.setDetainedList(validatedDetained);
            }
        } else {
            // No pattern configured
            if (entity.getDetainedRollNumbers() != null && !entity.getDetainedRollNumbers().trim().isEmpty()) {
                throw new IllegalArgumentException("Cannot specify detained roll numbers without configuring roll number range.");
            }
        }

        Section saved = repository.save(entity);

        // Synchronize student records if pattern is configured
        if (saved.getId() != null && saved.getStartRollNumber() != null && saved.getEndRollNumber() != null) {
            syncSectionRoster(saved);
        } else if (saved.getId() != null) {
            // Synchronize with existing Student records if any
            List<String> detainedList = saved.getDetainedList();
            List<Student> students = studentRepository.findBySectionId(saved.getId());
            for (Student s : students) {
                boolean isDetained = detainedList.stream().anyMatch(d -> d.equalsIgnoreCase(s.getRollNumber()));
                if (isDetained && !"DETAINED".equalsIgnoreCase(s.getStatus())) {
                    s.setStatus("DETAINED");
                    studentRepository.save(s);
                } else if (!isDetained && "DETAINED".equalsIgnoreCase(s.getStatus())) {
                    s.setStatus("ACTIVE");
                    studentRepository.save(s);
                }
            }
        }

        return enrichWithActualCounts(saved);
    }

    @Transactional
    public void deleteSectionCascade(Long sectionId) {
        Section section = repository.findById(sectionId).orElse(null);
        if (section == null) return;

        List<Student> students = studentRepository.findBySectionId(sectionId);
        List<Long> studentIds = students.stream().map(Student::getId).filter(Objects::nonNull).toList();

        if (!studentIds.isEmpty()) {
            // 1. Attendance records belonging to students in the Section
            attendanceRepository.deleteByStudentIdIn(studentIds);
            // 2. Seat allocations belonging to students in the Section
            seatAllocationRepository.deleteByStudentIdIn(studentIds);
            // 3. Student records belonging to the Section
            studentRepository.deleteByIdIn(studentIds);
        }

        // Also clean up any lingering direct records for safety
        attendanceRepository.deleteByStudentSectionId(sectionId);
        seatAllocationRepository.deleteByStudentSectionId(sectionId);
        studentRepository.deleteBySectionId(sectionId);

        // 4. The Section itself
        repository.deleteById(sectionId);
    }

    @Transactional
    public void deleteSafe(Long id) {
        Section section = repository.findById(id).orElse(null);
        if (section == null) return;

        List<Student> students = studentRepository.findBySectionId(id);
        long studentCount = students.size();
        long allocationCount = seatAllocationRepository.countByStudentSectionId(id);
        long attendanceCount = attendanceRepository.countByStudentSectionId(id);

        if (studentCount > 0 || allocationCount > 0 || attendanceCount > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot delete this section: Students are currently assigned / associated with it. ");
            if (studentCount > 0) {
                sb.append(studentCount).append(" student(s) currently assigned (");
                sb.append(students.stream().map(Student::getRollNumber).limit(10).collect(Collectors.joining(", ")));
                if (studentCount > 10) sb.append(", ...");
                sb.append("). ");
            }
            if (allocationCount > 0) {
                sb.append(allocationCount).append(" seat allocation(s) depend on this section's students. ");
            }
            if (attendanceCount > 0) {
                sb.append(attendanceCount).append(" attendance record(s) depend on this section's students. ");
            }
            sb.append("Remove or reassign the students first.");
            throw new IllegalStateException(sb.toString());
        }
        repository.deleteById(id);
    }

    @Transactional
    public void delete(Long id) {
        deleteSafe(id);
    }

    public SectionDetailsDTO getSectionDetails(Long id) {
        Section section = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + id + " not found."));

        SectionDetailsDTO dto = new SectionDetailsDTO();
        dto.setSectionId(section.getId());
        dto.setSectionName(section.getName());

        if (section.getDepartment() != null) {
            dto.setDepartmentId(section.getDepartment().getId());
            dto.setDepartmentName(section.getDepartment().getName());
            dto.setDepartmentCode(section.getDepartment().getCode());
        }

        dto.setStartRollNumber(section.getStartRollNumber());
        dto.setEndRollNumber(section.getEndRollNumber());
        dto.setExpectedStudentCount(section.getExpectedStudentCount());
        dto.setDetainedRollNumbers(section.getDetainedRollNumbers());
        dto.setDetainedCount(section.getDetainedCount());
        dto.setEligibleStudentCount(section.getEligibleStudentCount());
        dto.setConfigured(section.isConfigured());

        long totalStudents = studentRepository.countBySectionId(id);
        long activeStudents = studentRepository.countBySectionIdAndStatus(id, "ACTIVE");
        long detainedStudents = studentRepository.countBySectionIdAndStatus(id, "DETAINED");
        long inactiveStudents = Math.max(0, totalStudents - activeStudents);

        dto.setActualStudentCount(totalStudents);
        dto.setActualActiveStudentCount(activeStudents);
        dto.setActualInactiveStudentCount(inactiveStudents);
        dto.setActualDetainedStudentCount(detainedStudents);

        long allocCount = seatAllocationRepository.countByStudentSectionId(id);
        long attCount = attendanceRepository.countByStudentSectionId(id);

        dto.setStudentCount(totalStudents);
        dto.setAllocationCount(allocCount);
        dto.setAttendanceCount(attCount);

        boolean canDelete = (totalStudents == 0 && allocCount == 0 && attCount == 0);
        dto.setCanDelete(canDelete);

        // Consistency Status
        if (!section.isConfigured()) {
            dto.setConsistencyCode("UNCONFIGURED");
            if (totalStudents > 0) {
                dto.setConsistencyStatus("Legacy / Configuration Not Set (" + totalStudents + " student records exist in database).");
            } else {
                dto.setConsistencyStatus("Roll-number configuration has not been set for this section.");
            }
        } else if (section.getExpectedStudentCount() != null && section.getExpectedStudentCount().longValue() == totalStudents) {
            dto.setConsistencyCode("MATCH");
            dto.setConsistencyStatus("Configuration matches actual students (" + totalStudents + " records).");
        } else {
            dto.setConsistencyCode("MISMATCH");
            dto.setConsistencyStatus("Configuration expects " + (section.getExpectedStudentCount() != null ? section.getExpectedStudentCount() : 0) +
                    " students, but " + totalStudents + " student record(s) are currently assigned in database.");
        }

        // Students list
        List<Student> studentEntities = studentRepository.findBySectionId(id);
        List<SectionDetailsDTO.StudentItemDTO> studentItems = new ArrayList<>();
        List<String> sampleRolls = new ArrayList<>();
        for (Student s : studentEntities) {
            studentItems.add(new SectionDetailsDTO.StudentItemDTO(
                    s.getId(),
                    s.getRollNumber(),
                    s.getFullName(),
                    s.getEmail(),
                    s.getPhoneNumber(),
                    s.getSemester(),
                    s.getYear(),
                    s.getDepartment() != null ? s.getDepartment().getName() : null,
                    s.getDepartment() != null ? s.getDepartment().getCode() : null,
                    section.getName(),
                    s.getStatus()
            ));
            if (sampleRolls.size() < 10) {
                sampleRolls.add(s.getRollNumber());
            }
        }
        dto.setStudents(studentItems);

        // Allocations list
        List<SeatAllocation> allocationEntities = seatAllocationRepository.findByStudentSectionId(id);
        List<SectionDetailsDTO.AllocationItemDTO> allocationItems = new ArrayList<>();
        for (SeatAllocation sa : allocationEntities) {
            allocationItems.add(new SectionDetailsDTO.AllocationItemDTO(
                    sa.getId(),
                    sa.getExam() != null ? sa.getExam().getId() : null,
                    sa.getExam() != null ? sa.getExam().getSubjectCode() : null,
                    sa.getExam() != null ? sa.getExam().getSubjectName() : null,
                    sa.getRoom() != null ? sa.getRoom().getRoomNumber() : null,
                    sa.getRoom() != null ? sa.getRoom().getBuilding() : null,
                    sa.getSeatNumber(),
                    sa.getRowNumber(),
                    sa.getColumnNumber(),
                    sa.getStudent() != null ? sa.getStudent().getId() : null,
                    sa.getStudent() != null ? sa.getStudent().getRollNumber() : null,
                    sa.getStudent() != null ? sa.getStudent().getFullName() : null
            ));
        }
        dto.setSeatAllocations(allocationItems);

        // Attendances list
        List<Attendance> attendanceEntities = attendanceRepository.findByStudentSectionId(id);
        List<SectionDetailsDTO.AttendanceItemDTO> attendanceItems = new ArrayList<>();
        for (Attendance att : attendanceEntities) {
            attendanceItems.add(new SectionDetailsDTO.AttendanceItemDTO(
                    att.getId(),
                    att.getExam() != null ? att.getExam().getId() : null,
                    att.getExam() != null ? att.getExam().getSubjectCode() : null,
                    att.getRoom() != null ? att.getRoom().getRoomNumber() : null,
                    att.getSeatNumber(),
                    att.getStatus() != null ? att.getStatus().name() : null,
                    att.getMarkedAt() != null ? att.getMarkedAt().toString() : null,
                    att.getStudent() != null ? att.getStudent().getId() : null,
                    att.getStudent() != null ? att.getStudent().getRollNumber() : null,
                    att.getStudent() != null ? att.getStudent().getFullName() : null
            ));
        }
        dto.setAttendances(attendanceItems);

        if (!canDelete) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot delete Section '").append(section.getName()).append("': ");
            if (totalStudents > 0) {
                sb.append(totalStudents).append(" student(s) currently assigned (").append(String.join(", ", sampleRolls));
                if (totalStudents > sampleRolls.size()) sb.append(", ...");
                sb.append("). ");
            }
            if (allocCount > 0) {
                sb.append(allocCount).append(" seat allocation(s) depend on this section. ");
            }
            if (attCount > 0) {
                sb.append(attCount).append(" attendance record(s) depend on this section. ");
            }
            sb.append("Remove or reassign the students first.");
            dto.setDeleteBlockedReason(sb.toString());
        }

        return dto;
    }

    @Transactional
    public List<Section> createBatchSections(BatchSectionCreateDTO dto) {
        if (dto.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID is required.");
        }
        Department dept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found."));

        if (dto.getSectionNames() == null || dto.getSectionNames().isEmpty()) {
            throw new IllegalArgumentException("Section names list cannot be empty.");
        }

        List<Section> createdSections = new ArrayList<>();
        for (String rawName : dto.getSectionNames()) {
            if (rawName == null || rawName.trim().isEmpty()) {
                throw new IllegalArgumentException("Section name cannot be blank.");
            }
            String name = rawName.trim();
            if (repository.existsByNameAndDepartmentId(name, dept.getId())) {
                throw new IllegalArgumentException("Section '" + name + "' already exists in department '" + dept.getName() + "'.");
            }
            Section section = new Section();
            section.setName(name);
            section.setDepartment(dept);
            createdSections.add(enrichWithActualCounts(repository.save(section)));
        }

        return createdSections;
    }

    @Transactional
    public Section configurePattern(Long sectionId, SectionConfigDTO configDTO) {
        Section section = repository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));

        validatePattern(configDTO.getStartRollNumber(), configDTO.getEndRollNumber());
        String cleanStart = configDTO.getStartRollNumber().trim().toUpperCase();
        String cleanEnd = configDTO.getEndRollNumber().trim().toUpperCase();
        validateNoRollNumberConflicts(section, cleanStart, cleanEnd);

        section.setStartRollNumber(cleanStart);
        section.setEndRollNumber(cleanEnd);
        section.setExpectedStudentCount(calculateExpectedCount(cleanStart, cleanEnd));

        // Re-validate existing detained numbers against new range
        List<String> detained = section.getDetainedList();
        if (!detained.isEmpty()) {
            RollPatternInfo startInfo = parseRollNumber(cleanStart);
            RollPatternInfo endInfo = parseRollNumber(cleanEnd);
            List<String> validDetained = new ArrayList<>();
            for (String r : detained) {
                RollPatternInfo rInfo = parseRollNumber(r);
                if (rInfo.getPrefix().equalsIgnoreCase(startInfo.getPrefix())
                        && rInfo.getNumber() >= startInfo.getNumber()
                        && rInfo.getNumber() <= endInfo.getNumber()) {
                    validDetained.add(r);
                }
            }
            section.setDetainedList(validDetained);
        }

        Section saved = repository.save(section);
        syncSectionRoster(saved);
        return enrichWithActualCounts(saved);
    }

    @Transactional
    public Section addDetainedRollNumber(Long sectionId, String rollNumber) {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number cannot be empty.");
        }
        Section section = repository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));

        if (section.getStartRollNumber() == null || section.getEndRollNumber() == null) {
            throw new IllegalStateException("Section roll number range is not configured. Please configure start and end roll numbers first.");
        }

        String cleanRoll = rollNumber.trim().toUpperCase();
        RollPatternInfo detainedInfo = parseRollNumber(cleanRoll);
        RollPatternInfo startInfo = parseRollNumber(section.getStartRollNumber());
        RollPatternInfo endInfo = parseRollNumber(section.getEndRollNumber());

        if (!detainedInfo.getPrefix().equalsIgnoreCase(startInfo.getPrefix())) {
            throw new IllegalArgumentException("Roll number '" + cleanRoll + "' prefix does not match section prefix '" + startInfo.getPrefix() + "'.");
        }

        if (detainedInfo.getNumber() < startInfo.getNumber() || detainedInfo.getNumber() > endInfo.getNumber()) {
            throw new IllegalArgumentException("Roll number '" + cleanRoll + "' is outside section range [" + section.getStartRollNumber() + " - " + section.getEndRollNumber() + "].");
        }

        List<String> detainedList = section.getDetainedList();
        if (detainedList.stream().anyMatch(r -> r.equalsIgnoreCase(cleanRoll))) {
            throw new IllegalArgumentException("Roll number '" + cleanRoll + "' is already marked as detained.");
        }

        detainedList.add(cleanRoll);
        section.setDetainedList(detainedList);
        Section saved = repository.save(section);

        // Synchronize with Student entity if exists in DB
        studentRepository.findByRollNumber(cleanRoll).ifPresent(student -> {
            student.setStatus("DETAINED");
            studentRepository.save(student);
        });

        return enrichWithActualCounts(saved);
    }

    @Transactional
    public Section removeDetainedRollNumber(Long sectionId, String rollNumber) {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number cannot be empty.");
        }
        Section section = repository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));

        String cleanRoll = rollNumber.trim().toUpperCase();
        List<String> detainedList = section.getDetainedList();
        boolean removed = detainedList.removeIf(r -> r.equalsIgnoreCase(cleanRoll));
        if (!removed) {
            throw new IllegalArgumentException("Roll number '" + cleanRoll + "' is not in the detained list for this section.");
        }

        section.setDetainedList(detainedList);
        Section saved = repository.save(section);

        // Synchronize with Student entity if exists in DB
        studentRepository.findByRollNumber(cleanRoll).ifPresent(student -> {
            if ("DETAINED".equalsIgnoreCase(student.getStatus())) {
                student.setStatus("ACTIVE");
                studentRepository.save(student);
            }
        });

        return enrichWithActualCounts(saved);
    }

    public List<String> getDetainedRollNumbers(Long sectionId) {
        Section section = repository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));
        return section.getDetainedList();
    }

    @Transactional
    public void syncSectionRoster(Section section) {
        if (section == null || section.getId() == null) return;
        String startRoll = section.getStartRollNumber();
        String endRoll = section.getEndRollNumber();
        if (startRoll == null || startRoll.trim().isEmpty() || endRoll == null || endRoll.trim().isEmpty()) {
            return;
        }

        RollPatternInfo startInfo = parseRollNumber(startRoll.trim());
        RollPatternInfo endInfo = parseRollNumber(endRoll.trim());
        String prefix = startInfo.getPrefix();
        int suffixLength = startInfo.getSuffixLength();
        long startNum = startInfo.getNumber();
        long endNum = endInfo.getNumber();

        List<String> detainedList = section.getDetainedList();
        Set<String> expectedRolls = new LinkedHashSet<>();
        for (long num = startNum; num <= endNum; num++) {
            String roll = prefix + String.format("%0" + suffixLength + "d", num);
            expectedRolls.add(roll);
        }

        List<Student> currentStudents = studentRepository.findBySectionId(section.getId());
        Map<String, Student> currentMap = currentStudents.stream()
                .collect(Collectors.toMap(s -> s.getRollNumber().trim().toUpperCase(), s -> s, (s1, s2) -> s1));

        Department dept = section.getDepartment();

        for (String roll : expectedRolls) {
            boolean isDetained = detainedList.stream().anyMatch(d -> d.equalsIgnoreCase(roll));
            String targetStatus = isDetained ? "DETAINED" : "ACTIVE";

            if (currentMap.containsKey(roll)) {
                Student st = currentMap.get(roll);
                boolean changed = false;
                if (!targetStatus.equalsIgnoreCase(st.getStatus())) {
                    st.setStatus(targetStatus);
                    changed = true;
                }
                if (st.getDepartment() == null || !st.getDepartment().getId().equals(dept.getId())) {
                    st.setDepartment(dept);
                    changed = true;
                }
                if (changed) {
                    studentRepository.save(st);
                }
            } else {
                Optional<Student> existingOpt = studentRepository.findByRollNumber(roll);
                if (existingOpt.isPresent()) {
                    Student st = existingOpt.get();
                    if (st.getSection() != null && !st.getSection().getId().equals(section.getId())) {
                        // Safety guard: student belongs to another section, do not modify or reassign!
                        continue;
                    }
                    st.setSection(section);
                    st.setDepartment(dept);
                    st.setStatus(targetStatus);
                    studentRepository.save(st);
                } else {
                    Student newSt = new Student();
                    newSt.setRollNumber(roll);
                    newSt.setFullName(null);
                    newSt.setDepartment(dept);
                    newSt.setSection(section);
                    newSt.setStatus(targetStatus);
                    newSt.setSemester(1);
                    newSt.setYear(1);
                    newSt.setEmail(roll.toLowerCase() + "@college.edu");
                    studentRepository.save(newSt);
                }
            }
        }

        // For any student in this section whose roll is not in expected rolls:
        for (Student st : currentStudents) {
            String roll = st.getRollNumber().trim().toUpperCase();
            if (!expectedRolls.contains(roll)) {
                long allocCount = seatAllocationRepository.countByStudentId(st.getId());
                long attCount = attendanceRepository.countByStudentId(st.getId());
                if (allocCount > 0 || attCount > 0) {
                    // Preserve student record due to dependent historical exam allocations or attendance
                } else {
                    studentRepository.delete(st);
                }
            }
        }
    }

    @Transactional
    public Section syncSectionRosterById(Long sectionId) {
        Section section = repository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section with ID " + sectionId + " not found."));
        syncSectionRoster(section);
        return enrichWithActualCounts(repository.findById(sectionId).orElse(section));
    }

    public void validateNoRollNumberConflicts(Section section, String startRoll, String endRoll) {
        if (startRoll == null || startRoll.trim().isEmpty() || endRoll == null || endRoll.trim().isEmpty()) {
            return;
        }
        RollPatternInfo startInfo = parseRollNumber(startRoll.trim());
        RollPatternInfo endInfo = parseRollNumber(endRoll.trim());
        String prefix = startInfo.getPrefix();
        long startNum = startInfo.getNumber();
        long endNum = endInfo.getNumber();
        int suffixLen = startInfo.getSuffixLength();

        Long currentSecId = (section != null) ? section.getId() : null;

        // 1. Check against other sections' configured ranges
        List<Section> allSections = repository.findAll();
        for (Section other : allSections) {
            if (currentSecId != null && currentSecId.equals(other.getId())) {
                continue;
            }
            if (other.getStartRollNumber() != null && other.getEndRollNumber() != null) {
                try {
                    RollPatternInfo oStart = parseRollNumber(other.getStartRollNumber().trim());
                    RollPatternInfo oEnd = parseRollNumber(other.getEndRollNumber().trim());
                    if (oStart.getPrefix().equalsIgnoreCase(prefix)) {
                        long oS = oStart.getNumber();
                        long oE = oEnd.getNumber();
                        if (Math.max(startNum, oS) <= Math.min(endNum, oE)) {
                            String otherDeptName = other.getDepartment() != null ? other.getDepartment().getName() : "Unknown";
                            throw new IllegalArgumentException(
                                    "Roll number range [" + startRoll + " - " + endRoll + "] conflicts with section '" +
                                    other.getName() + "' in department '" + otherDeptName + "' which uses range [" +
                                    other.getStartRollNumber() + " - " + other.getEndRollNumber() + "]. Each section must have distinct roll numbers."
                            );
                        }
                    }
                } catch (IllegalArgumentException e) {
                    if (e.getMessage() != null && e.getMessage().contains("conflicts with section")) {
                        throw e;
                    }
                }
            }
        }

        // 2. Check against existing students in database belonging to other sections
        for (long num = startNum; num <= endNum; num++) {
            String roll = prefix + String.format("%0" + suffixLen + "d", num);
            Optional<Student> stOpt = studentRepository.findByRollNumber(roll);
            if (stOpt.isPresent()) {
                Student st = stOpt.get();
                if (st.getSection() != null && (currentSecId == null || !currentSecId.equals(st.getSection().getId()))) {
                    String assignedSec = st.getSection().getName();
                    String assignedDept = st.getDepartment() != null ? st.getDepartment().getName() : "Unknown";
                    throw new IllegalArgumentException(
                            "Roll number '" + roll + "' is already assigned to section '" + assignedSec +
                            "' in department '" + assignedDept + "'. Roll numbers cannot be duplicated across sections."
                    );
                }
            }
        }
    }

    public static RollPatternInfo parseRollNumber(String rollNumber) {
        if (rollNumber == null || rollNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll number cannot be blank.");
        }
        Matcher matcher = ROLL_PATTERN.matcher(rollNumber.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Roll number '" + rollNumber + "' must end with numeric digits (e.g. 24EG107A01).");
        }
        String prefix = matcher.group(1).toUpperCase();
        String suffix = matcher.group(2);
        try {
            long num = Long.parseLong(suffix);
            return new RollPatternInfo(prefix, num, suffix.length());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid numeric suffix in roll number '" + rollNumber + "'.");
        }
    }

    public static void validatePattern(String startRoll, String endRoll) {
        if (startRoll == null || startRoll.trim().isEmpty() || endRoll == null || endRoll.trim().isEmpty()) {
            throw new IllegalArgumentException("Both start and end roll numbers are required.");
        }
        RollPatternInfo startInfo = parseRollNumber(startRoll.trim());
        RollPatternInfo endInfo = parseRollNumber(endRoll.trim());

        if (!startInfo.getPrefix().equalsIgnoreCase(endInfo.getPrefix())) {
            throw new IllegalArgumentException("Roll number prefix mismatch: Start prefix '" + startInfo.getPrefix() + "' does not match End prefix '" + endInfo.getPrefix() + "'.");
        }

        if (startInfo.getNumber() > endInfo.getNumber()) {
            throw new IllegalArgumentException("Start roll number (" + startInfo.getNumber() + ") cannot be greater than End roll number (" + endInfo.getNumber() + ").");
        }
    }

    public static int calculateExpectedCount(String startRoll, String endRoll) {
        RollPatternInfo startInfo = parseRollNumber(startRoll.trim());
        RollPatternInfo endInfo = parseRollNumber(endRoll.trim());
        return (int) (endInfo.getNumber() - startInfo.getNumber() + 1);
    }

    public static class RollPatternInfo {
        private final String prefix;
        private final long number;
        private final int suffixLength;

        public RollPatternInfo(String prefix, long number, int suffixLength) {
            this.prefix = prefix;
            this.number = number;
            this.suffixLength = suffixLength;
        }

        public String getPrefix() { return prefix; }
        public long getNumber() { return number; }
        public int getSuffixLength() { return suffixLength; }
    }
}
