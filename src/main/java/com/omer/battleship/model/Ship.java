package com.omer.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Ship {
    public static final int CELL_SIZE = 30; // Visual size of each ship cell (for graphics)

    private final int size;                  // Length of the ship (number of cells)
    private boolean horizontal = true;       // Whether the ship is horizontal or vertical
    private final List<Cell> cells = new ArrayList<>(); // List of cells occupied by the ship

    public Ship(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    // Changes the ship's orientation (horizontal ↔ vertical) and clears its cells
    public void rotate() {
        horizontal = !horizontal;
        cells.clear();
    }

    public List<Cell> getCells() {
        return cells;
    }

    // Adds a cell to the ship (used during placement)
    public void addCell(Cell cell) {
        cells.add(cell);
    }

    // Returns true if all cells of the ship have been hit, meaning the ship is sunk
    public boolean isSunk() {
        return cells.stream().allMatch(Cell::isHit);
    }
}
