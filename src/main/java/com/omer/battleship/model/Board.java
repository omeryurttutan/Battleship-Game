package com.omer.battleship.model;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private final int rows;             // Number of rows on the board
    private final int cols;             // Number of columns on the board
    private final Cell[][] grid;        // 2D array of cells on the board
    private final List<Ship> ships;     // List of ships placed on the board

    public Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        this.grid = new Cell[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c); // Create a new cell for each position
            }
        }

        this.ships = new ArrayList<>();
    }

    // Creates a default 10x10 board
    public Board() {
        this(10, 10);
    }

    // Resets the board: clears all hits and ship placements
    public void reset() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c].resetHit();
                grid[r][c].setShip(false);
            }
        }
        ships.clear();
    }

    // Attempts to place a ship on the board
    public boolean placeShip(Ship ship, int row, int col) {
        // Remove existing instance of the same ship if already placed
        if (ships.contains(ship)) {
            removeShip(ship);
        }

        ship.getCells().clear();
        for (int i = 0; i < ship.getSize(); i++) {
            int r = row + (ship.isHorizontal() ? 0 : i);
            int c = col + (ship.isHorizontal() ? i : 0);
            grid[r][c].placeShip();       // Mark the cell as having a ship
            ship.addCell(grid[r][c]);     // Add cell to ship's list
        }
        ships.add(ship);
        return true;
    }

    // Removes a given ship from the board
    public void removeShip(Ship ship) {
        for (Cell cell : ship.getCells()) {
            cell.setShip(false);          // Clear the ship from the cell
        }
        ship.getCells().clear();
        ships.remove(ship);
    }

    // Shoots at a specific cell and returns the result
    public ShotResult shootAt(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols)
            return ShotResult.MISS; // Outside the board is always a miss

        Cell cell = grid[row][col];
        cell.hit();
        if (cell.hasShip()) {
            for (Ship ship : ships) {
                if (ship.getCells().contains(cell)) {
                    return ship.isSunk() ? ShotResult.SUNK : ShotResult.HIT;
                }
            }
        }
        return ShotResult.MISS;
    }

    // Returns all cells on the board
    public Cell[][] getCells() {
        return grid;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public Cell[][] getGrid() { return grid; }
    public List<Ship> getShips() { return ships; }
}
