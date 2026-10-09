package com.example.examseating.repository;

import com.example.examseating.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    @Query("SELECT COUNT(e) FROM Exam e JOIN e.selectedRooms r WHERE r.id = :roomId")
    long countBySelectedRoomId(@Param("roomId") Long roomId);
}
