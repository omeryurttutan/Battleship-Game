package com.omer.battleship.model;

public class Cell {
    private final int row;             // Row index of the cell
    private final int col;             // Column index of the cell
    private boolean hasShip;           // Whether the cell contains a ship
    private boolean isHit;             // Whether the cell has been shot at

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.hasShip = false;
        this.isHit = false;
    }

    // Resets the hit status
    public void resetHit() {
        this.isHit = false;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    public boolean hasShip() { return hasShip; }

    // Called when a ship is placed on the cell
    public void placeShip() { this.hasShip = true; }

    // Sets whether the cell has a ship or not
    public void setShip(boolean b) { this.hasShip = b; }

    public boolean isHit() { return isHit; }

    // Called when the cell is shot at
    public void hit() { this.isHit = true; }

    // Returns true if the cell is both hit and contains a ship
    public boolean isHitAndShip() {
        return isHit && hasShip;
    }
}
