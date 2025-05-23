package com.omer.battleship.logic;

import com.omer.battleship.model.Board;
import com.omer.battleship.model.Ship;
import com.omer.battleship.model.ShotResult;

public class GameLogic {

    // Represents the current phase of the game
    public enum Phase { SETUP, IN_PROGRESS, FINISHED }

    private final Board[] boards = new Board[2];       // One game board for each player
    private final int[] aliveShips = new int[2];       // Remaining ship count for each player
    private final boolean[] ready = new boolean[2];    // Tracks if players are ready

    private Phase phase = Phase.SETUP; // Game starts in the setup phase
    private int currentTurn = 0;       // Whose turn it is (0 or 1 — player index)
    private int winner = -1;           // The number of the winning player (-1 = no winner yet)

    public GameLogic() {
        boards[0] = new Board(10, 10); // 10x10 board for player 1
        boards[1] = new Board(10, 10); // 10x10 board for player 2
    }

    // Attempts to place a ship on the board for the given player
    public boolean placeShip(int player, Ship ship, int row, int col) {
        ensurePhase(Phase.SETUP);         // Only allowed during setup phase
        validatePlayer(player);           // Check if player number is valid
        boolean ok = boards[player].placeShip(ship, row, col);
        if (ok) {
            aliveShips[player] = boards[player].getShips().size(); // Update if placement successful
        }
        return ok;
    }

    // Marks the player as ready after placing ships
    public void playerReady(int player) {
        ensurePhase(Phase.SETUP);
        validatePlayer(player);
        ready[player] = true;

        // If both players are ready, start the game
        if (ready[0] && ready[1]) {
            phase = Phase.IN_PROGRESS;
            currentTurn = 0; // Player 1 starts the game
        }
    }

    // Resets the game state for a new round
    public void resetForNewGame() {
        this.phase       = Phase.SETUP;
        this.currentTurn = 0;
        this.winner      = -1;

        ready[0] = false;
        ready[1] = false;
        aliveShips[0] = 0;
        aliveShips[1] = 0;

        boards[0].reset();
        boards[1].reset();
    }

    public Phase getPhase() {
        return phase;
    }

    // Returns the board of the specified player
    public Board getBoard(int player) {
        validatePlayer(player);
        return boards[player];
    }

    // Throws an error if the game is not in the expected phase
    private void ensurePhase(Phase expected) {
        if (phase != expected)
            throw new IllegalStateException("Current phase is " + phase + " – expected " + expected);
    }

    // Checks if the player index is 0 or 1, throws an error otherwise
    private void validatePlayer(int player) {
        if (player < 0 || player > 1)
            throw new IllegalArgumentException("Invalid player number: " + player);
    }
}
