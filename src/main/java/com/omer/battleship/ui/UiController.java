// com/omer/battleship/ui/UiController.java
package com.omer.battleship.ui;

import com.omer.battleship.model.ShotResult;

/**
 * Interface to define UI-related actions.
 * The GameManager uses this to communicate with the user interface.
 */
public interface UiController {

    // Make the enemy's board visible
    void showEnemyBoard();

    // Enable or disable the ability to fire (click on enemy board)
    void enableFireMode(boolean active);

    // Update the status label/message on the screen
    void updateStatus(String text);

    // Mark the player's own board with the result of an enemy's attack
    void markMyBoard(int row, int col, ShotResult result);

    // Mark the enemy's board with the result of the player's attack
    void markEnemyBoard(int row, int col, ShotResult result);

    // Reset the UI when a rematch starts
    void resetForRematch();

    // Show game result (win or lose) and ask if the player wants to play again
    void showGameOver(boolean isWin, int hits, int shots);
}
