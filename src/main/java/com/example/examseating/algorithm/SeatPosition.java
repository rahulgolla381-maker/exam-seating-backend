package com.example.examseating.algorithm;
import com.example.examseating.entity.Room;

public class SeatPosition {
    private Room room;
    private int row;
    private int col;
    private int seatNumber;

    public SeatPosition(Room room, int row, int col, int seatNumber) {
        this.room = room;
        this.row = row;
        this.col = col;
        this.seatNumber = seatNumber;
    }

    public Room getRoom() { return room; }
    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getSeatNumber() { return seatNumber; }
}
