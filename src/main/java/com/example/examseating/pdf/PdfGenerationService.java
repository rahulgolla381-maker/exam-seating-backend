package com.example.examseating.pdf;

import com.example.examseating.entity.Department;
import com.example.examseating.entity.Exam;
import com.example.examseating.entity.Room;
import com.example.examseating.entity.SeatAllocation;
import com.example.examseating.repository.ExamRepository;
import com.example.examseating.repository.RoomRepository;
import com.example.examseating.repository.SeatAllocationRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service generating authoritative, print-ready PDF documents for examination seating.
 * Produces:
 * 1. Complete Exam Seating Arrangement PDF (College Format with Master Summary Table & Detailed Room Rosters)
 * 2. Room-wise Seating Chart PDF (physical 2D visual grid)
 * 3. Room-wise Invigilator Attendance Sheet PDF
 */
@Service
public class PdfGenerationService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatAllocationRepository seatAllocationRepository;

    private static final BaseColor PRIMARY_NAVY = new BaseColor(30, 58, 138);       // #1e3a8a
    private static final BaseColor ACCENT_BLUE = new BaseColor(3, 105, 161);        // #0369a1
    private static final BaseColor LIGHT_BLUE = new BaseColor(239, 246, 255);       // #eff6ff
    private static final BaseColor STAGE_DARK = new BaseColor(51, 65, 85);          // #334155
    private static final BaseColor BORDER_GRAY = new BaseColor(203, 213, 225);      // #cbd5e1
    private static final BaseColor MUTED_TEXT = new BaseColor(100, 116, 139);       // #64748b
    private static final BaseColor ROW_ALT = new BaseColor(248, 250, 252);          // #f8fafc

    private static final Font INSTITUTION_FONT = new Font(Font.FontFamily.HELVETICA, 13, Font.BOLD, PRIMARY_NAVY);
    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD, PRIMARY_NAVY);
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, STAGE_DARK);
    private static final Font META_LABEL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, MUTED_TEXT);
    private static final Font META_VAL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.BLACK);
    private static final Font TABLE_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);
    private static final Font TABLE_BODY_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK);
    private static final Font TABLE_BOLD_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.BLACK);
    private static final Font GRID_STAGE_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    private static final Font GRID_SEAT_NUM_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, ACCENT_BLUE);
    private static final Font GRID_NAME_FONT = new Font(Font.FontFamily.HELVETICA, 7, Font.BOLD, BaseColor.BLACK);
    private static final Font GRID_ROLL_FONT = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, STAGE_DARK);
    private static final Font GRID_EMPTY_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, MUTED_TEXT);

    /**
     * Page number and footer event handler for A4 printing ("Page X of Y").
     */
    private static class PageNumberHelper extends PdfPageEventHelper {
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(40, 16);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // Ignore font load failure
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            String pageText = "Page " + writer.getPageNumber() + " of ";
            float textSize = (baseFont != null) ? baseFont.getWidthPoint(pageText, 8) : 40;
            float textBase = document.bottom() - 15;

            // Footer left
            cb.beginText();
            if (baseFont != null) cb.setFontAndSize(baseFont, 8);
            cb.setColorFill(MUTED_TEXT);
            cb.setTextMatrix(document.left(), textBase);
            cb.showText("Exam Hall Seating Allocator System • Official College Seating Plan");
            cb.endText();

            // Footer right (Page X of Y)
            float rightX = document.right() - 25;
            cb.beginText();
            if (baseFont != null) cb.setFontAndSize(baseFont, 8);
            cb.setColorFill(MUTED_TEXT);
            cb.setTextMatrix(rightX - textSize, textBase);
            cb.showText(pageText);
            cb.endText();

            cb.addTemplate(totalPagesTemplate, rightX, textBase);
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            totalPagesTemplate.beginText();
            if (baseFont != null) totalPagesTemplate.setFontAndSize(baseFont, 8);
            totalPagesTemplate.setColorFill(MUTED_TEXT);
            totalPagesTemplate.setTextMatrix(0, 0);
            totalPagesTemplate.showText(String.valueOf(writer.getPageNumber()));
            totalPagesTemplate.endText();
        }
    }

    /**
     * Helper to safely resolve building name from Room entity.
     */
    public String getBuildingName(Room r) {
        if (r == null) return "MAIN BLOCK";
        if (r.getBuildingEntity() != null && r.getBuildingEntity().getName() != null && !r.getBuildingEntity().getName().isBlank()) {
            return r.getBuildingEntity().getName();
        }
        if (r.getBuilding() != null && !r.getBuilding().isBlank()) {
            return r.getBuilding();
        }
        return "MAIN BLOCK";
    }

    /**
     * Helper to safely resolve floor name from Room entity.
     */
    public String getFloorName(Room r) {
        if (r == null) return "Floor 1";
        if (r.getFloorEntity() != null && r.getFloorEntity().getFloorName() != null && !r.getFloorEntity().getFloorName().isBlank()) {
            return r.getFloorEntity().getFloorName();
        }
        if (r.getFloor() != null && !r.getFloor().isBlank()) {
            return r.getFloor();
        }
        return "Floor 1";
    }

    /**
     * 1. COMPLETE EXAM SEATING ARRANGEMENT PDF (Part 5 Requirements)
     * Endpoint: GET /api/allocations/{examId}/pdf
     *
     * Features:
     * A. Header: College Name, EXAM SEATING PLAN, Exam/Subject, Code, Date, Session, Semester, Academic Year.
     * B. Primary College Seating Plan Table:
     *    | PROGRAM | FROM | TO | BLOCK | ROOM NO. | NO. OF STUDENTS |
     *    Calculated directly from actual MySQL allocations with Grand Total row.
     * C. Detailed Room-by-Room Section:
     *    Room Header: Building, Floor, Room Number, Capacity, Students Allocated.
     *    Student Roster Table: S.No | Roll Number | Student Name | Seat Number | Department | Section.
     * D. Print-ready A4 layout with repeated table headers, clean borders, and Page X of Y numbering.
     */
    public byte[] generateExamSeatingPdf(Long examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        List<SeatAllocation> allocations = seatAllocationRepository.findByExamId(examId);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No seating arrangement has been generated for this exam.");
        }

        // PDF Integrity Validation: verify all allocated records are valid and unique
        Set<Long> uniqueAllocatedStudentIds = new HashSet<>();
        Set<String> uniqueAllocatedSeats = new HashSet<>();
        for (SeatAllocation sa : allocations) {
            if (sa.getStudent() == null || sa.getStudent().getId() == null) {
                throw new IllegalStateException("Allocation record contains null student data.");
            }
            if (sa.getRoom() == null || sa.getRoom().getId() == null) {
                throw new IllegalStateException("Allocation record contains null room data.");
            }
            if (!uniqueAllocatedStudentIds.add(sa.getStudent().getId())) {
                throw new IllegalStateException("Duplicate student allocation found in database for student ID: " + sa.getStudent().getId());
            }
            String seatKey = sa.getRoom().getId() + "-" + sa.getRowNumber() + "-" + sa.getColumnNumber();
            if (!uniqueAllocatedSeats.add(seatKey)) {
                throw new IllegalStateException("Duplicate seat assignment found in database for room " + sa.getRoom().getRoomNumber() + " at " + seatKey);
            }
        }

        // Collect distinct rooms in ordered sequence (Building -> Floor -> Room Number -> Room ID)
        List<Room> distinctRooms = allocations.stream()
                .map(SeatAllocation::getRoom)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator
                                .comparing((Room r) -> getBuildingName(r))
                                .thenComparing(this::getFloorName)
                                .thenComparing(Room::getRoomNumber)
                                .thenComparing(Room::getId))),
                        ArrayList::new
                ));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 30, 30, 36, 42);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PageNumberHelper());
            document.open();

            // A. Header Section
            addHeaderBlock(document, "EXAM SEATING PLAN", "OFFICIAL MASTER SEATING ARRANGEMENT");

            // Exam Info Summary Card: exactly matching persisted allocations count
            addExamSummaryBlock(document, exam, allocations.size(), distinctRooms.size());

            document.add(new Paragraph(" "));

            // B. Primary College Seating Plan Summary Table
            Paragraph summaryTitle = new Paragraph("COLLEGE MASTER SEATING PLAN SUMMARY", SUBTITLE_FONT);
            summaryTitle.setSpacingAfter(6f);
            document.add(summaryTitle);

            PdfPTable summaryTable = new PdfPTable(6);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{1.4f, 2.0f, 2.0f, 2.0f, 1.4f, 1.6f});
            summaryTable.setHeaderRows(1);

            addTableHeader(summaryTable, new String[]{"PROGRAM", "FROM", "TO", "BLOCK", "ROOM NO.", "NO. OF STUDENTS"});

            int grandTotalStudents = 0;
            boolean alt = false;

            for (Room room : distinctRooms) {
                List<SeatAllocation> roomAllocs = allocations.stream()
                        .filter(a -> a.getRoom() != null && a.getRoom().getId().equals(room.getId()))
                        .collect(Collectors.toList());

                if (roomAllocs.isEmpty()) continue;

                // Group by department code and section name
                Map<String, List<SeatAllocation>> groupMapInRoom = new LinkedHashMap<>();
                for (SeatAllocation sa : roomAllocs) {
                    String deptCode = (sa.getStudent() != null && sa.getStudent().getDepartment() != null && sa.getStudent().getDepartment().getCode() != null)
                            ? sa.getStudent().getDepartment().getCode().trim()
                            : "GEN";
                    String secName = (sa.getStudent() != null && sa.getStudent().getSection() != null && sa.getStudent().getSection().getName() != null)
                            ? sa.getStudent().getSection().getName().trim()
                            : "";
                    String programKey = secName.isEmpty() ? deptCode : deptCode + "-" + secName;
                    groupMapInRoom.computeIfAbsent(programKey, k -> new ArrayList<>()).add(sa);
                }

                for (Map.Entry<String, List<SeatAllocation>> entry : groupMapInRoom.entrySet()) {
                    String program = entry.getKey();
                    List<SeatAllocation> groupAllocs = entry.getValue();
                    groupAllocs.sort(Comparator.comparing(a -> (a.getStudent() != null && a.getStudent().getRollNumber() != null) ? a.getStudent().getRollNumber() : ""));

                    String fromRoll = (groupAllocs.get(0).getStudent() != null && groupAllocs.get(0).getStudent().getRollNumber() != null)
                            ? groupAllocs.get(0).getStudent().getRollNumber() : "-";
                    String toRoll = (groupAllocs.get(groupAllocs.size() - 1).getStudent() != null && groupAllocs.get(groupAllocs.size() - 1).getStudent().getRollNumber() != null)
                            ? groupAllocs.get(groupAllocs.size() - 1).getStudent().getRollNumber() : "-";
                    String block = getBuildingName(room);
                    String roomNo = room.getRoomNumber();
                    int count = groupAllocs.size();
                    grandTotalStudents += count;

                    BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
                    alt = !alt;

                    addTableCell(summaryTable, program, TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(summaryTable, fromRoll, TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(summaryTable, toRoll, TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(summaryTable, block, TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(summaryTable, roomNo, TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(summaryTable, String.valueOf(count), TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                }
            }

            // Verify summary grand total equals allocations size
            if (grandTotalStudents != allocations.size()) {
                throw new IllegalStateException(String.format(
                        "PDF integrity error: Summary table total (%d) does not match total persisted allocations (%d).",
                        grandTotalStudents, allocations.size()));
            }

            // Total Row
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL STUDENTS ALLOCATED", TABLE_BOLD_FONT));
            totalLabelCell.setColspan(5);
            totalLabelCell.setBackgroundColor(LIGHT_BLUE);
            totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalLabelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalLabelCell.setPadding(6f);
            totalLabelCell.setBorderColor(BORDER_GRAY);
            summaryTable.addCell(totalLabelCell);

            PdfPCell totalValCell = new PdfPCell(new Phrase(String.valueOf(grandTotalStudents), TABLE_BOLD_FONT));
            totalValCell.setBackgroundColor(LIGHT_BLUE);
            totalValCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            totalValCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            totalValCell.setPadding(6f);
            totalValCell.setBorderColor(BORDER_GRAY);
            summaryTable.addCell(totalValCell);

            document.add(summaryTable);

            // C. Detailed Room-by-Room Seating Rosters
            Paragraph rosterTitle = new Paragraph("ROOM-WISE DETAILED SEATING ROSTERS", SUBTITLE_FONT);
            rosterTitle.setSpacingBefore(18f);
            rosterTitle.setSpacingAfter(8f);
            document.add(rosterTitle);

            int totalRosterStudents = 0;
            for (Room room : distinctRooms) {
                List<SeatAllocation> roomAllocs = allocations.stream()
                        .filter(a -> a.getRoom() != null && a.getRoom().getId().equals(room.getId()))
                        .sorted(Comparator.comparingInt(SeatAllocation::getSeatNumber))
                        .collect(Collectors.toList());

                if (roomAllocs.isEmpty()) continue;

                // Room Header Info Banner
                PdfPTable roomHeaderTable = new PdfPTable(5);
                roomHeaderTable.setWidthPercentage(100);
                roomHeaderTable.setWidths(new float[]{2.0f, 1.6f, 1.4f, 1.4f, 1.6f});
                roomHeaderTable.setSpacingBefore(12f);
                roomHeaderTable.setSpacingAfter(4f);

                int capacity = room.getCapacity() > 0 ? room.getCapacity() : (room.getRowsCount() * room.getColumnsCount());

                addRoomHeaderCell(roomHeaderTable, "Building / Block:", getBuildingName(room));
                addRoomHeaderCell(roomHeaderTable, "Floor / Level:", getFloorName(room));
                addRoomHeaderCell(roomHeaderTable, "Room Number:", "Room " + room.getRoomNumber());
                addRoomHeaderCell(roomHeaderTable, "Room Capacity:", String.valueOf(capacity));
                addRoomHeaderCell(roomHeaderTable, "Students Allocated:", String.valueOf(roomAllocs.size()));

                document.add(roomHeaderTable);

                // Student Roster Table
                PdfPTable rosterTable = new PdfPTable(6);
                rosterTable.setWidthPercentage(100);
                rosterTable.setWidths(new float[]{0.8f, 1.8f, 2.6f, 1.2f, 1.1f, 1.0f});
                rosterTable.setHeaderRows(1);

                addTableHeader(rosterTable, new String[]{"S.No", "Roll Number", "Student Name", "Seat Number", "Department", "Section"});

                int sNo = 1;
                boolean rAlt = false;
                for (SeatAllocation sa : roomAllocs) {
                    totalRosterStudents++;
                    BaseColor bg = rAlt ? ROW_ALT : BaseColor.WHITE;
                    rAlt = !rAlt;

                    String studentName = (sa.getStudent().getFullName() != null && !sa.getStudent().getFullName().isBlank())
                            ? sa.getStudent().getFullName()
                            : sa.getStudent().getRollNumber();

                    addTableCell(rosterTable, String.valueOf(sNo++), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(rosterTable, sa.getStudent().getRollNumber(), TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(rosterTable, studentName, TABLE_BODY_FONT, bg, Element.ALIGN_LEFT);
                    addTableCell(rosterTable, "Seat #" + sa.getSeatNumber(), TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(rosterTable, sa.getStudent().getDepartment().getCode(), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                    addTableCell(rosterTable, sa.getStudent().getSection().getName(), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                }

                document.add(rosterTable);
            }

            if (totalRosterStudents != allocations.size()) {
                throw new IllegalStateException(String.format(
                        "PDF integrity error: Detailed room rosters total (%d) does not match total persisted allocations (%d).",
                        totalRosterStudents, allocations.size()));
            }

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Exam Seating PDF: " + e.getMessage(), e);
        }
    }

    /**
     * 2. ROOM-WISE SEATING CHART PDF
     * Endpoint: GET /api/allocations/{examId}/room/{roomId}/pdf
     */
    public byte[] generateRoomSeatingPdf(Long examId, Long roomId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        List<SeatAllocation> allocations = seatAllocationRepository.findByExamIdAndRoomId(examId, roomId);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No seating arrangement found for Room " + room.getRoomNumber() + " in this exam.");
        }

        Map<String, SeatAllocation> gridMap = new HashMap<>();
        for (SeatAllocation sa : allocations) {
            gridMap.put(sa.getRowNumber() + "-" + sa.getColumnNumber(), sa);
        }

        int rows = room.getRowsCount();
        int cols = room.getColumnsCount();
        int capacity = room.getCapacity() > 0 ? room.getCapacity() : (rows * cols);
        int occupied = allocations.size();
        int empty = Math.max(0, capacity - occupied);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeaderBlock(document, "EXAMINATION HALL SEATING CHART", "ROOM " + room.getRoomNumber() + " (" + getBuildingName(room) + " • " + getFloorName(room) + ")");

            addExamInfoBlock(document, exam, occupied, room);

            document.add(new Paragraph(" "));

            // Stage Banner
            PdfPTable stageTable = new PdfPTable(1);
            stageTable.setWidthPercentage(100);
            PdfPCell stageCell = new PdfPCell(new Phrase("▲ FRONT / TEACHER DESK / BLACKBOARD ▲", GRID_STAGE_FONT));
            stageCell.setBackgroundColor(STAGE_DARK);
            stageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            stageCell.setPadding(6f);
            stageCell.setBorderColor(STAGE_DARK);
            stageTable.addCell(stageCell);
            document.add(stageTable);

            document.add(new Paragraph(" "));

            // Visual Seating Grid Table
            PdfPTable gridTable = new PdfPTable(cols);
            gridTable.setWidthPercentage(100);

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    String key = r + "-" + c;
                    SeatAllocation sa = gridMap.get(key);
                    int seatNum = (sa != null) ? sa.getSeatNumber() : (r * cols + c + 1);

                    PdfPCell cell = new PdfPCell();
                    cell.setPadding(6f);
                    cell.setMinimumHeight(48f);

                    Paragraph p = new Paragraph();
                    p.add(new Chunk("Seat #" + seatNum + " (R" + (r + 1) + "-C" + (c + 1) + ")\n", GRID_SEAT_NUM_FONT));

                    if (sa != null) {
                        cell.setBackgroundColor(LIGHT_BLUE);
                        cell.setBorderColor(BORDER_GRAY);
                        String name = (sa.getStudent().getFullName() != null && !sa.getStudent().getFullName().isBlank())
                                ? sa.getStudent().getFullName()
                                : sa.getStudent().getRollNumber();
                        p.add(new Chunk(name + "\n", GRID_NAME_FONT));
                        p.add(new Chunk(sa.getStudent().getRollNumber() + " • ", GRID_ROLL_FONT));
                        p.add(new Chunk(sa.getStudent().getDepartment().getCode() + "-" + sa.getStudent().getSection().getName(), GRID_ROLL_FONT));
                    } else {
                        cell.setBackgroundColor(ROW_ALT);
                        cell.setBorderColor(BORDER_GRAY);
                        p.add(new Chunk("\nEMPTY", GRID_EMPTY_FONT));
                    }

                    cell.addElement(p);
                    gridTable.addCell(cell);
                }
            }

            document.add(gridTable);

            Paragraph statsPara = new Paragraph(
                    String.format("Hall Statistics: Total Capacity: %d   |   Occupied Desks: %d   |   Empty Desks: %d   |   Room Occupancy: %.1f%%",
                            capacity, occupied, empty, (capacity > 0 ? (occupied * 100.0 / capacity) : 0)),
                    META_LABEL_FONT);
            statsPara.setAlignment(Element.ALIGN_CENTER);
            statsPara.setSpacingBefore(12f);
            document.add(statsPara);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Room Seating Chart PDF: " + e.getMessage(), e);
        }
    }

    /**
     * 3. ROOM-WISE ATTENDANCE SHEET PDF (Preserved in backend for data completeness)
     * Endpoint: GET /api/allocations/{examId}/room/{roomId}/attendance-pdf
     */
    public byte[] generateRoomAttendancePdf(Long examId, Long roomId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("Exam not found with ID: " + examId));

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found with ID: " + roomId));

        List<SeatAllocation> allocations = seatAllocationRepository.findByExamIdAndRoomId(examId, roomId);
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("No seating arrangement found for Room " + room.getRoomNumber() + " in this exam.");
        }

        allocations.sort(Comparator.comparingInt(SeatAllocation::getSeatNumber));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 30, 30, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            addHeaderBlock(document, "EXAMINATION ATTENDANCE SHEET", "INVIGILATOR VERIFICATION RECORD");
            addExamInfoBlock(document, exam, allocations.size(), room);

            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.8f, 1.0f, 1.5f, 2.5f, 1.0f, 0.9f, 2.3f});
            table.setHeaderRows(1);

            addTableHeader(table, new String[]{"S.No.", "Seat #", "Roll Number", "Student Name", "Dept", "Sec", "Student Signature"});

            int serial = 1;
            boolean alt = false;
            for (SeatAllocation sa : allocations) {
                BaseColor bg = alt ? ROW_ALT : BaseColor.WHITE;
                alt = !alt;

                String sName = (sa.getStudent().getFullName() != null && !sa.getStudent().getFullName().isBlank())
                        ? sa.getStudent().getFullName()
                        : sa.getStudent().getRollNumber();

                addTableCell(table, String.valueOf(serial++), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                addTableCell(table, "Seat #" + sa.getSeatNumber(), TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                addTableCell(table, sa.getStudent().getRollNumber(), TABLE_BOLD_FONT, bg, Element.ALIGN_CENTER);
                addTableCell(table, sName, TABLE_BODY_FONT, bg, Element.ALIGN_LEFT);
                addTableCell(table, sa.getStudent().getDepartment().getCode(), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                addTableCell(table, sa.getStudent().getSection().getName(), TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
                addTableCell(table, "____________________", TABLE_BODY_FONT, bg, Element.ALIGN_CENTER);
            }

            document.add(table);

            document.add(new Paragraph(" "));
            PdfPTable signTable = new PdfPTable(3);
            signTable.setWidthPercentage(100);
            signTable.setSpacingBefore(20f);

            PdfPCell c1 = new PdfPCell(new Phrase("Present Candidates: _________\nAbsent Candidates: _________", TABLE_BODY_FONT));
            PdfPCell c2 = new PdfPCell(new Phrase("Invigilator Name:\n___________________________", TABLE_BODY_FONT));
            PdfPCell c3 = new PdfPCell(new Phrase("Invigilator Signature:\n___________________________", TABLE_BODY_FONT));

            c1.setBorder(Rectangle.NO_BORDER);
            c2.setBorder(Rectangle.NO_BORDER);
            c3.setBorder(Rectangle.NO_BORDER);

            signTable.addCell(c1);
            signTable.addCell(c2);
            signTable.addCell(c3);
            document.add(signTable);

            addFooterBlock(document);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Attendance PDF: " + e.getMessage(), e);
        }
    }

    private void addHeaderBlock(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph inst = new Paragraph("COLLEGE OF ENGINEERING & TECHNOLOGY", INSTITUTION_FONT);
        inst.setAlignment(Element.ALIGN_CENTER);
        doc.add(inst);

        Paragraph subInst = new Paragraph("OFFICE OF THE CONTROLLER OF EXAMINATIONS • AUTONOMOUS", META_LABEL_FONT);
        subInst.setAlignment(Element.ALIGN_CENTER);
        doc.add(subInst);

        Paragraph mainTitle = new Paragraph(title, TITLE_FONT);
        mainTitle.setAlignment(Element.ALIGN_CENTER);
        mainTitle.setSpacingBefore(4f);
        doc.add(mainTitle);

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph sub = new Paragraph(subtitle, SUBTITLE_FONT);
            sub.setAlignment(Element.ALIGN_CENTER);
            doc.add(sub);
        }

        doc.add(new Paragraph(" "));
    }

    private void addExamSummaryBlock(Document doc, Exam exam, int totalAllocated, int totalRooms) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.2f, 2.0f, 1.2f, 1.6f});

        addInfoCell(infoTable, "Course / Subject:", exam.getSubjectName() + " (" + exam.getSubjectCode() + ")");
        addInfoCell(infoTable, "Exam Date & Session:", (exam.getExamDate() != null ? exam.getExamDate().toString() : "TBD") +
                " (" + (exam.getSession() != null ? exam.getSession() : "MORNING") + ")");

        String timeStr = (exam.getStartTime() != null ? exam.getStartTime().toString().substring(0, 5) : "09:30") + " - " +
                (exam.getEndTime() != null ? exam.getEndTime().toString().substring(0, 5) : "12:30");
        addInfoCell(infoTable, "Examination Timing:", timeStr);
        addInfoCell(infoTable, "Academic Term:", "Semester " + exam.getSemester() + " (" + exam.getAcademicYear() + ")");

        addInfoCell(infoTable, "Candidates Seated:", String.valueOf(totalAllocated));
        addInfoCell(infoTable, "Halls Allocated:", String.valueOf(totalRooms) + " Examination Rooms");

        doc.add(infoTable);
    }

    private void addExamInfoBlock(Document doc, Exam exam, int totalAllocated, Room room) throws DocumentException {
        PdfPTable infoTable = new PdfPTable(4);
        infoTable.setWidthPercentage(100);
        infoTable.setWidths(new float[]{1.2f, 2.0f, 1.2f, 1.6f});

        addInfoCell(infoTable, "Course / Subject:", exam.getSubjectName() + " (" + exam.getSubjectCode() + ")");
        addInfoCell(infoTable, "Exam Date & Session:", (exam.getExamDate() != null ? exam.getExamDate().toString() : "TBD") +
                " (" + (exam.getSession() != null ? exam.getSession() : "MORNING") + ")");

        String timeStr = (exam.getStartTime() != null ? exam.getStartTime().toString().substring(0, 5) : "09:30") + " - " +
                (exam.getEndTime() != null ? exam.getEndTime().toString().substring(0, 5) : "12:30");
        addInfoCell(infoTable, "Examination Timing:", timeStr);

        if (room != null) {
            addInfoCell(infoTable, "Hall / Room:", "Room " + room.getRoomNumber() + " (" + getBuildingName(room) + ", " + getFloorName(room) + ")");
        } else {
            addInfoCell(infoTable, "Academic Term:", "Semester " + exam.getSemester() + " (" + exam.getAcademicYear() + ")");
        }

        addInfoCell(infoTable, "Candidates Seated:", String.valueOf(totalAllocated));
        addInfoCell(infoTable, "Document Status:", "OFFICIAL CONFIRMED");

        doc.add(infoTable);
    }

    private void addRoomHeaderCell(PdfPTable table, String label, String val) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5f);
        cell.setBackgroundColor(STAGE_DARK);
        cell.setBorderColor(STAGE_DARK);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, new BaseColor(226, 232, 240));
        Font valFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(val, valFont));
        p.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p);

        table.addCell(cell);
    }

    private void addInfoCell(PdfPTable table, String label, String val) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(4f);
        cell.setBackgroundColor(ROW_ALT);
        cell.setBorderColor(BORDER_GRAY);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", META_LABEL_FONT));
        p.add(new Chunk(val, META_VAL_FONT));
        cell.addElement(p);

        table.addCell(cell);
    }

    private void addTableHeader(PdfPTable table, String[] headers) {
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(PRIMARY_NAVY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(6f);
            cell.setBorderColor(PRIMARY_NAVY);
            table.addCell(cell);
        }
    }

    private void addTableCell(PdfPTable table, String text, Font font, BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        cell.setBorderColor(BORDER_GRAY);
        table.addCell(cell);
    }

    private void addFooterBlock(Document doc) throws DocumentException {
        Paragraph footer = new Paragraph(
                "Generated on: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                        "  |  Exam Hall Seating Allocator System  |  Confidential Institutional Document",
                META_LABEL_FONT);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(16f);
        doc.add(footer);
    }
}
