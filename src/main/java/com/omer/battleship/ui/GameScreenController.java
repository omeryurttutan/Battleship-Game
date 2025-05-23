package com.omer.battleship.ui;

import com.omer.battleship.game.GameManager;
import com.omer.battleship.model.ShotResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.util.Optional;
import javafx.application.Platform;

// Class that controls the FXML interface
public class GameScreenController implements UiController {

    @FXML private GridPane myBoardGrid;     // Player's own board
    @FXML private GridPane enemyBoardGrid;  // Opponent's board
    @FXML private Label statusLabel;        // Status display label

    private GameManager controller;

    // Sets the controller and connects it to the UI
    public void setController(GameManager controller) {
        this.controller = controller;
        controller.setUiController(this);
    }

    // Runs when the READY button is clicked
    @FXML
    private void onReadyClick() {
        int placed = controller.getBoard().getShips().size();
        System.out.println("[DEBUG] placed ships = " + placed);

        final int REQUIRED = 4;
        if (placed < REQUIRED) {
            updateStatus("⚠ Place all ships! (" + placed + "/" + REQUIRED + ")");
            return;
        }

        controller.sendReady(); // Sends ready status to the server
        System.out.println("[DEBUG] sent READY");

        updateStatus("⏳ Waiting for the other player...");
        myBoardGrid.setDisable(true); // Locks ship placement
    }

    // Displays a popup when the game ends
    @Override
    public void showGameOver(boolean isWin, int hits, int shots) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(isWin ? "🎉 You Win!" : "💥 You Lose!");
        alert.setContentText(String.format("Ships hit: %d%nTotal shots: %d%n\nWould you like to play again?", hits, shots));

        ButtonType rematch = new ButtonType("Play Again");
        ButtonType exit    = new ButtonType("Exit", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(rematch, exit);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == rematch) {
            controller.sendRematch();  // Sends rematch request
            resetForRematch();         // Resets the UI
        } else {
            Platform.exit();           // Exits the game
        }
    }

    // UI reset for rematch (optional)
    @Override
    public void resetForRematch() {
        // Optionally reset board and status label here
    }

    // Enables or disables firing turn
    @Override
    public void enableFireMode(boolean active) {
        enemyBoardGrid.setDisable(!active);
        System.out.println("Grid now " + (active ? "ENABLED" : "DISABLED"));
    }

    // Makes the enemy board visible
    @Override
    public void showEnemyBoard() {
        enemyBoardGrid.setVisible(true);
    }

    // Updates the status message
    @Override
    public void updateStatus(String text) {
        statusLabel.setText(text);
    }

    // Marks hits on your own board
    @Override
    public void markMyBoard(int row, int col, ShotResult result) {
        Pane cell = findCell(myBoardGrid, row, col);
        if (result == ShotResult.HIT || result == ShotResult.SUNK) {
            cell.setStyle("-fx-background-color: red;");
        } else {
            cell.setStyle("-fx-background-color: white;");
        }
    }

    // Marks hits on the enemy board
    @Override
    public void markEnemyBoard(int row, int col, ShotResult result) {
        Pane cell = findCell(enemyBoardGrid, row, col);
        if (result == ShotResult.HIT || result == ShotResult.SUNK) {
            cell.setStyle("-fx-background-color: red;");
        } else {
            cell.setStyle("-fx-background-color: white;");
        }
    }

    // Finds the cell at a specific row and column in a GridPane
    private Pane findCell(GridPane grid, int row, int col) {
        for (var node : grid.getChildren()) {
            Integer r = GridPane.getRowIndex(node);
            Integer c = GridPane.getColumnIndex(node);
            if (r != null && c != null && r == row && c == col && node instanceof Pane) {
                return (Pane) node;
            }
        }
        throw new IllegalStateException("Cell not found at " + row + "," + col);
    }
}
