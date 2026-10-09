package com.example.examseating.service;

import com.example.examseating.algorithm.AllocationException;
import com.example.examseating.algorithm.AllocationResult;
import com.example.examseating.algorithm.SeatPosition;
import com.example.examseating.dto.GenerateAllocationRequest;
import com.example.examseating.entity.*;
import com.example.examseating.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeatAllocationAppServiceTest {

    @Mock
    private ExamRepository examRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private SeatAllocationRepository seatAllocationRepository;
    @Mock
    private com.example.examseating.repository.AttendanceRepository attendanceRepository;
    @Mock
    private SeatAllocationService seatAllocationService;

    @InjectMocks
    private SeatAllocationAppService appService;

    private Exam exam;
    private Student student;
    private Room room;

    @BeforeEach
    void setUp() {
        exam = new Exam();
        exam.setId(1L);

        Department d = new Department();
        d.setCode("AI");
        Section s = new Section();
        s.setName("A");

        student = new Student();
        student.setId(10L);
        student.setDepartment(d);
        student.setSection(s);
        student.setRollNumber("23AI001");

        room = new Room();
        room.setId(100L);
        room.setRoomNumber("C-101");
        room.setRowsCount(5);
        room.setColumnsCount(6);
    }

    @Test
    void testGenerateAllocation_Success() {
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(1L);
        request.setStudentIds(Arrays.asList(10L));
        request.setRoomIds(Arrays.asList(100L));

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(studentRepository.findAllById(anyList())).thenReturn(Arrays.asList(student));
        when(roomRepository.findAllById(anyList())).thenReturn(Arrays.asList(room));

        SeatPosition pos = new SeatPosition(room, 0, 0, 1);
        AllocationResult result = new AllocationResult(pos, student);
        when(seatAllocationService.allocateSeating(anyList(), anyList())).thenReturn(Arrays.asList(result));

        when(seatAllocationRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        var dtos = appService.generateAllocation(request);

        assertEquals(1, dtos.size());
        assertEquals("23AI001", dtos.get(0).getRollNumber());
        assertEquals("C-101", dtos.get(0).getRoomNumber());
        verify(seatAllocationRepository, times(1)).saveAll(anyList());
    }

    @Test
    void testGenerateAllocation_InvalidExam() {
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(99L);
        request.setStudentIds(Arrays.asList(10L));
        request.setRoomIds(Arrays.asList(100L));

        when(examRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> appService.generateAllocation(request));
    }

    @Test
    void testGenerateAllocation_DuplicateStudentInRequest() {
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(1L);
        request.setStudentIds(Arrays.asList(10L, 10L));
        request.setRoomIds(Arrays.asList(100L));

        assertThrows(IllegalArgumentException.class, () -> appService.generateAllocation(request));
    }

    @Test
    void testGenerateAllocation_DetainedStudentRejected() {
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(1L);
        request.setStudentIds(Arrays.asList(10L));
        request.setRoomIds(Arrays.asList(100L));

        student.setStatus("DETAINED");
        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(studentRepository.findAllById(anyList())).thenReturn(Arrays.asList(student));

        Exception ex = assertThrows(IllegalArgumentException.class, () -> appService.generateAllocation(request));
        assertTrue(ex.getMessage().contains("DETAINED"));
    }

    @Test
    void testGenerateAllocation_AlgorithmThrowsException() {
        GenerateAllocationRequest request = new GenerateAllocationRequest();
        request.setExamId(1L);
        request.setStudentIds(Arrays.asList(10L));
        request.setRoomIds(Arrays.asList(100L));

        when(examRepository.findById(1L)).thenReturn(Optional.of(exam));
        when(studentRepository.findAllById(anyList())).thenReturn(Arrays.asList(student));
        when(roomRepository.findAllById(anyList())).thenReturn(Arrays.asList(room));
        
        when(seatAllocationService.allocateSeating(anyList(), anyList())).thenThrow(new AllocationException("Impossible seating"));

        assertThrows(AllocationException.class, () -> appService.generateAllocation(request));
        
        // verify saveAll is NEVER called so no partial records exist
        verify(seatAllocationRepository, never()).saveAll(anyList());
    }
}
