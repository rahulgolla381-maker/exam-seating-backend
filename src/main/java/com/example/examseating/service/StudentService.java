package com.example.examseating.service;

import com.example.examseating.entity.Section;
import com.example.examseating.entity.Student;
import com.example.examseating.repository.AttendanceRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import com.example.examseating.repository.SectionRepository;
import com.example.examseating.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentService {
    @Autowired
    private StudentRepository repository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<Student> getAll() { return repository.findAll(); }
    public Student getById(Long id) { return repository.findById(id).orElse(null); }
    public List<Student> getBySectionId(Long sectionId) { return repository.findBySectionId(sectionId); }

    @Transactional
    public Student save(Student entity) {
        if (entity.getSemester() == null) {
            entity.setSemester(1);
        }
        if (entity.getYear() == null) {
            entity.setYear(1);
        }
        if (entity.getId() != null) {
            Student existing = repository.findById(entity.getId()).orElse(null);
            if (existing != null) {
                if (entity.getEmail() == null || entity.getEmail().isBlank()) {
                    entity.setEmail(existing.getEmail());
                }
                if (entity.getPhoneNumber() == null) {
                    entity.setPhoneNumber(existing.getPhoneNumber());
                }
                if (entity.getSemester() == null || entity.getSemester() == 1) {
                    if (existing.getSemester() != null) {
                        entity.setSemester(existing.getSemester());
                    }
                }
                if (entity.getYear() == null || entity.getYear() == 1) {
                    if (existing.getYear() != null) {
                        entity.setYear(existing.getYear());
                    }
                }
            }
        }
        if (entity.getEmail() == null || entity.getEmail().isBlank()) {
            if (entity.getRollNumber() != null) {
                entity.setEmail(entity.getRollNumber().trim().toLowerCase() + "@college.edu");
            }
        }
        Student saved = repository.save(entity);
        if (saved.getSection() != null && saved.getSection().getId() != null && saved.getRollNumber() != null) {
            Section section = sectionRepository.findById(saved.getSection().getId()).orElse(null);
            if (section != null) {
                List<String> detainedList = section.getDetainedList();
                String roll = saved.getRollNumber().trim().toUpperCase();
                if ("DETAINED".equalsIgnoreCase(saved.getStatus())) {
                    if (detainedList.stream().noneMatch(r -> r.equalsIgnoreCase(roll))) {
                        detainedList.add(roll);
                        section.setDetainedList(detainedList);
                        sectionRepository.save(section);
                    }
                } else {
                    if (detainedList.removeIf(r -> r.equalsIgnoreCase(roll))) {
                        section.setDetainedList(detainedList);
                        sectionRepository.save(section);
                    }
                }
            }
        }
        return saved;
    }

    @Transactional
    public void delete(Long id) {
        Student student = repository.findById(id).orElse(null);
        if (student == null) return;

        long allocCount = seatAllocationRepository.countByStudentId(id);
        long attCount = attendanceRepository.countByStudentId(id);
        if (allocCount > 0 || attCount > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Cannot delete student '").append(student.getRollNumber()).append("': Student has ");
            if (allocCount > 0) {
                sb.append(allocCount).append(" active seat allocation(s) ");
            }
            if (attCount > 0) {
                if (allocCount > 0) sb.append("and ");
                sb.append(attCount).append(" attendance record(s) ");
            }
            sb.append("associated with it.");
            throw new IllegalStateException(sb.toString());
        }

        if (student.getSection() != null && student.getRollNumber() != null) {
            Section section = sectionRepository.findById(student.getSection().getId()).orElse(null);
            if (section != null) {
                List<String> detainedList = section.getDetainedList();
                String roll = student.getRollNumber().trim().toUpperCase();
                if (detainedList.removeIf(r -> r.equalsIgnoreCase(roll))) {
                    section.setDetainedList(detainedList);
                    sectionRepository.save(section);
                }
            }
        }
        repository.deleteById(id);
    }
}
