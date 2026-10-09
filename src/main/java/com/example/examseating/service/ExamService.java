package com.example.examseating.service;

import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.repository.ExamRepository;
import com.example.examseating.repository.RoomRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExamService {
    @Autowired
    private ExamRepository repository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    @Autowired
    private com.example.examseating.repository.AttendanceRepository attendanceRepository;

    public List<Exam> getAll() { return repository.findAll(); }
    public Exam getById(Long id) { return repository.findById(id).orElse(null); }
    public Exam save(Exam entity) { return repository.save(entity); }

    @Transactional
    public void delete(Long id) {
        attendanceRepository.deleteByExamId(id);
        seatAllocationRepository.deleteByExamId(id);
        repository.deleteById(id);
    }

    public List<Room> getSelectedRooms(Long examId) {
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));
        if (exam.getSelectedRooms() != null && !exam.getSelectedRooms().isEmpty()) {
            return exam.getSelectedRooms();
        }
        // Fallback to rooms that have allocations for this exam
        List<SeatAllocation> allocations = seatAllocationRepository.findByExamId(examId);
        return allocations.stream()
                .map(SeatAllocation::getRoom)
                .distinct()
                .toList();
    }

    @Transactional
    public List<Room> setSelectedRooms(Long examId, List<Long> roomIds) {
        Exam exam = repository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));
        List<Room> rooms = roomRepository.findAllById(roomIds);
        exam.setSelectedRooms(rooms);
        repository.save(exam);
        return rooms;
    }
}
