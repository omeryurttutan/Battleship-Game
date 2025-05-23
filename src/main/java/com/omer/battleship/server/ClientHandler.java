package com.omer.battleship.server;

import java.io.*;
import java.net.*;
import java.util.List;

import com.omer.battleship.model.Board;
import com.omer.battleship.model.ShotResult;
import com.omer.battleship.model.Ship;

public class ClientHandler implements Runnable {
    private final Socket socket;                // Socket connection with this client
    private final BufferedReader in;            // Reads data from the client
    private final PrintWriter out;              // Sends data to the client
    private boolean ready = false;              // Whether the player is ready
    private String playerName;                  // Name of the player
    private static int joinedPlayers = 0;       // Number of connected players (not used currently)
    private GameRoom room;                      // The game room the player belongs to

    private final Board board = new Board(10, 10); // The game board for the player

    public void setRoom(GameRoom r) { this.room = r; }
    public String getPlayerName()  { return playerName; }

    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.in     = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out    = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            out.println("NAME_REQUEST"); // Ask the player for their name
            String msg;
            while ((msg = in.readLine()) != null) {
                handleMessage(msg); // Process the received message
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Defines how the server handles messages from the client
    private void handleMessage(String msg) {
        msg = msg.trim();

        // Receive the player's name
        if (msg.startsWith("NAME ")) {
            playerName = msg.substring(5).trim();
            System.out.println("Player name: " + playerName);

        }
        // Add the player to the lobby
        else if (msg.equals("JOIN")) {
            BattleshipServer.joinLobby(this);

        }
        // The player indicates they are ready
        else if (msg.equals("READY")) {
            if (room != null) {
                room.playerReady(this);
            }

        }
        // The player is placing a ship
        else if (msg.startsWith("PLACE ")) {
            String[] p   = msg.split(" ");
            int size     = Integer.parseInt(p[1]);
            int row      = Integer.parseInt(p[2]);
            int col      = Integer.parseInt(p[3]);
            boolean hor  = p[4].equals("H");

            // Remove any existing ship of the same size
            board.getShips().removeIf(s -> s.getSize() == size);

            Ship ship = new Ship(size);
            if (!hor) ship.rotate(); // Rotate if vertical
            boolean ok = board.placeShip(ship, row, col);

            System.out.println(playerName + " PLACE "
                    + row + "," + col + " size=" + size + (hor ? " H" : " V")
                    + "  ==> " + (ok ? "OK" : "FAIL"));
        }

        // Fire command
        else if (msg.startsWith("FIRE ")) {
            String[] parts = msg.trim().split("\\s+");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);

            ClientHandler opponent = room.getOpponent(this);
            if (opponent == null) return;

            // If the cell was already hit, return MISS again
            if (opponent.board.getGrid()[row][col].isHit()) {
                sendMessage(String.format("RESULT MISS %d %d", row, col));
            } else {
                ShotResult result = opponent.board.shootAt(row, col);

                // Notify the firing player about the result
                sendMessage(String.format("RESULT %s %d %d SELF",
                        result.name(), row, col));
                // Notify the opponent too
                opponent.sendMessage(String.format("RESULT %s %d %d OPPONENT",
                        result.name(), row, col));

                // Check if all opponent ships are sunk
                List<Ship> oppShips = opponent.board.getShips();
                boolean opponentAllSunk = !oppShips.isEmpty()
                        && oppShips.stream().allMatch(Ship::isSunk);

                // Check if game is over
                if (opponentAllSunk) {
                    int winner = room.getPlayers().indexOf(this) + 1;
                    for (ClientHandler p : room.getPlayers()) {
                        p.sendMessage("GAMEOVER " + winner);
                    }
                } else {
                    // If the game continues, give turn to the opponent
                    int nextTurn = room.getPlayers().indexOf(opponent) + 1;
                    for (ClientHandler p : room.getPlayers()) {
                        p.sendMessage("TURN " + nextTurn);
                    }
                }
            }
        }

        // Forward the result message to the opponent
        else if (msg.startsWith("RESULT ")) {
            ClientHandler opponent = room.getOpponent(this);
            if (opponent != null) {
                opponent.sendMessage(msg);
            }
        }

        // Rematch request
        else if (msg.equals("REMATCH")) {
            if (room != null) {
                room.requestRematch(this);
            }
        }

        // Unknown messages
        else {
            System.out.println("Unknown message: " + msg);
        }
    }

    public Board getBoard() {
        return board;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void sendMessage(String msg) {
        out.println(msg); // Send message to the client
    }
}
