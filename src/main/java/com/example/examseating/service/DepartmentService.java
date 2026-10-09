package com.example.examseating.service;

import com.example.examseating.dto.DepartmentDetailsDTO;
import com.example.examseating.entity.Department;
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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DepartmentService {

    @Autowired
    private DepartmentRepository repository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Department> getAll() {
        List<Department> list = repository.findAll();
        for (Department d : list) {
            enrichDepartmentCounts(d);
        }
        return list;
    }

    public Department getById(Long id) {
        Department d = repository.findById(id).orElse(null);
        if (d != null) {
            enrichDepartmentCounts(d);
        }
        return d;
    }

    public Department save(Department entity) {
        Department saved = repository.save(entity);
        enrichDepartmentCounts(saved);
        return saved;
    }

    public DepartmentDetailsDTO getDepartmentDetails(Long id) {
        Department d = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Department with ID " + id + " not found."));

        long secCount = sectionRepository.countByDepartmentId(id);
        long studCount = studentRepository.countBySectionDepartmentId(id);
        long allocCount = seatAllocationRepository.countByStudentDepartmentId(id);
        long attCount = attendanceRepository.countByStudentDepartmentId(id);

        List<Section> sections = sectionRepository.findByDepartmentId(id);
        List<String> secNames = sections.stream().map(Section::getName).collect(Collectors.toList());

        return new DepartmentDetailsDTO(
                d.getId(),
                d.getName(),
                d.getCode(),
                secCount,
                studCount,
                allocCount,
                attCount,
                secNames
        );
    }

    @Transactional
    public void deleteDepartmentCascade(Long departmentId) {
        Department department = repository.findById(departmentId).orElse(null);
        if (department == null) return;

        // 1. Find all Sections belonging to the Department
        List<Section> sections = sectionRepository.findByDepartmentId(departmentId);
        List<Long> sectionIds = sections.stream().map(Section::getId).filter(Objects::nonNull).toList();

        // 2. Find all Students belonging to those Sections OR belonging directly to Department
        Set<Student> studentsSet = new HashSet<>(studentRepository.findByDepartmentId(departmentId));
        if (!sectionIds.isEmpty()) {
            studentsSet.addAll(studentRepository.findBySectionIdIn(sectionIds));
        }
        List<Long> studentIds = studentsSet.stream().map(Student::getId).filter(Objects::nonNull).toList();

        if (!studentIds.isEmpty()) {
            // 3. Delete attendance records belonging to those students
            attendanceRepository.deleteByStudentIdIn(studentIds);
            // 4. Delete seat allocations belonging to those students
            seatAllocationRepository.deleteByStudentIdIn(studentIds);
            // 5. Delete the Student records
            studentRepository.deleteByIdIn(studentIds);
        }

        // Clean up any lingering direct records for safety
        attendanceRepository.deleteByStudentDepartmentId(departmentId);
        seatAllocationRepository.deleteByStudentDepartmentId(departmentId);
        studentRepository.deleteByDepartmentId(departmentId);

        // 6. Delete the Sections
        if (!sectionIds.isEmpty()) {
            sectionRepository.deleteByIdIn(sectionIds);
        }
        sectionRepository.deleteByDepartmentId(departmentId);

        // 7. Delete the Department
        repository.deleteById(departmentId);
    }

    public void deleteSafe(Long id) {
        Department dept = repository.findById(id).orElse(null);
        if (dept == null) return;

        long secCount = sectionRepository.countByDepartmentId(id);
        long studCount = studentRepository.countByDepartmentId(id);
        if (secCount > 0 || studCount > 0) {
            throw new IllegalStateException("Cannot delete department '" + dept.getCode() + "': It has " +
                    secCount + " section(s) and " + studCount + " student(s) associated with it. Use cascade delete to remove all dependent data.");
        }
        repository.deleteById(id);
    }

    @Transactional
    public void delete(Long id) {
        deleteSafe(id);
    }

    private void enrichDepartmentCounts(Department d) {
        if (d != null && d.getId() != null) {
            d.setSectionCount(sectionRepository.countByDepartmentId(d.getId()));
            d.setStudentCount(studentRepository.countBySectionDepartmentId(d.getId()));
        }
    }
}
