package com.omer.battleship.game;

import com.omer.battleship.client.Client;
import com.omer.battleship.logic.GameLogic;
import com.omer.battleship.model.Board;
import com.omer.battleship.model.Ship;
import com.omer.battleship.model.ShotResult;
import com.omer.battleship.ui.BattleshipClientApp;
import com.omer.battleship.ui.UiController;
import javafx.application.Platform;

import java.util.HashSet;
import java.util.Set;

public class GameManager {

    // To avoid counting the same hit multiple times
    private final Set<String> countedHits = new HashSet<>();
    private int shotsFired = 0;
    private int hits       = 0;

    private final Client client;                      // Client object to communicate with the server
    private final GameLogic engine = new GameLogic(); // Engine that manages game logic
    private UiController uiController;                // UI controller

    private String playerName = "Player";             // Player name
    private int thisPlayerNumber;                     // Player number (1 or 2)

    public GameManager(Client client) {
        this.client = client;
        this.client.setMessageHandler(this::handleServerMessage); // Set handler to process incoming messages
    }

    public void setUiController(UiController ui)  { this.uiController = ui; }
    public void setPlayerName(String name)       { this.playerName   = name; }

    // Place the ship on the specified location and notify the server
    public boolean placeShip(Ship ship, int row, int col) {
        System.out.printf("[DEBUG-CLIENT] try PLACE size=%d at row=%d, col=%d%n",
                ship.getSize(), row, col);
        boolean ok = engine.placeShip(thisPlayerNumber - 1, ship, row, col);
        if (ok) {
            String dir = ship.isHorizontal() ? "H" : "V";
            String msg = String.format("PLACE %d %d %d %s",
                    ship.getSize(), row, col, dir);
            System.out.println("[DEBUG-CLIENT] sent → " + msg);
            client.sendMessage(msg);
        } else {
            System.out.println("[DEBUG-CLIENT] placement FAILED");
        }
        return ok;
    }

    // Reset stats and send rematch request to the server
    public void sendRematch() {
        shotsFired = 0;
        hits       = 0;
        countedHits.clear();
        client.sendMessage("REMATCH");
    }

    // Send READY message if all ships are placed
    public void sendReady() {
        if (engine.getBoard(thisPlayerNumber - 1).getShips().size() < 4) {
            uiController.updateStatus("⚠ You must place all your ships!");
            return;
        }
        engine.playerReady(thisPlayerNumber - 1);
        client.sendMessage("READY");
    }

    // Fire at a cell and notify the server
    public void sendFire(int row, int col) {
        shotsFired++;
        client.sendMessage(String.format("FIRE %d %d", row, col));
        uiController.enableFireMode(false); // Disable firing again until next turn
    }

    // Handle messages received from the server
    public void handleServerMessage(String msg) {
        String[] p = msg.trim().split("\\s+");
        if (p.length == 0) return;

        switch (p[0]) {
            case "NAME_REQUEST" ->
                    client.sendMessage("NAME " + playerName); // Send player name to the server

            case "START" -> {
                if (p.length < 2) return;
                thisPlayerNumber = Integer.parseInt(p[1]);
                Platform.runLater(() ->
                        uiController.updateStatus("✅ Game started, waiting for turn info…")
                );
            }

            case "TURN" -> {
                if (p.length < 2) return;
                int turn = Integer.parseInt(p[1]);
                boolean mine = (turn == thisPlayerNumber);

                // During setup, mark opponent as ready
                if (engine.getPhase() == GameLogic.Phase.SETUP) {
                    int otherIdx = 1 - (thisPlayerNumber - 1);
                    try { engine.playerReady(otherIdx); }
                    catch (IllegalStateException ignore) {}
                }

                Platform.runLater(() -> {
                    uiController.showEnemyBoard();
                    uiController.enableFireMode(mine);
                    uiController.updateStatus(
                            mine ? "🎯 YOUR TURN!" : "⏳ OPPONENT'S TURN…"
                    );
                });
            }

            case "FIRE" -> {
                if (p.length < 3) return;
                int r = Integer.parseInt(p[1]);
                int c = Integer.parseInt(p[2]);

                Board myBoard  = engine.getBoard(thisPlayerNumber - 1);
                ShotResult res = myBoard.shootAt(r, c);

                // Prevent counting the same hit multiple times
                if ((res == ShotResult.HIT || res == ShotResult.SUNK)
                        && countedHits.add(r + "-" + c)) {
                    hits++;
                }

                Platform.runLater(() ->
                        uiController.markMyBoard(r, c, res)
                );
            }

            case "RESULT" -> {
                if (p.length < 5) return;
                ShotResult res;
                try { res = ShotResult.valueOf(p[1]); }
                catch (IllegalArgumentException e) {
                    System.err.println("⚠ Invalid RESULT: " + p[1]);
                    return;
                }
                int r = Integer.parseInt(p[2]);
                int c = Integer.parseInt(p[3]);
                String target = p[4];

                // Show result of your own shot (on self or opponent)
                Platform.runLater(() -> {
                    if ("SELF".equals(target)) {
                        uiController.markEnemyBoard(r, c, res);
                    } else {
                        uiController.markMyBoard(r, c, res);
                    }
                });
            }

            case "GAMEOVER" -> {
                if (p.length < 2) return;
                int winner = Integer.parseInt(p[1]);
                boolean isWin = (winner == thisPlayerNumber);
                Platform.runLater(() ->
                        uiController.showGameOver(isWin, hits, shotsFired)
                );
            }

            case "REMATCH_START" -> {
                engine.resetForNewGame();
                Platform.runLater(() -> {
                    BattleshipClientApp.getInstance().resetForRematch();
                    uiController.resetForRematch();
                });
                break;
            }

            default -> System.out.println("⚠ Unknown message: " + msg); // Print unknown messages
        }
    }

    public Board getBoard() {
        return engine.getBoard(thisPlayerNumber - 1); // Return the player’s board
    }
}
