package com.omer.battleship.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private final List<ClientHandler> players = new CopyOnWriteArrayList<>(); // Players in the game room
    private final Set<ClientHandler> rematchRequests = new HashSet<>();       // Players who requested a rematch

    // A game room is created with 2 players
    public GameRoom(ClientHandler p1, ClientHandler p2) {
        players.add(p1);
        players.add(p2);
    }

    // Starts the game: sends a start message to both players
    public void start() {
        players.get(0).sendMessage("START 1");
        players.get(1).sendMessage("START 2");
    }

    // Called when a player indicates they are ready
    public void playerReady(ClientHandler who) {
        who.setReady(true);
        System.out.println(who.getPlayerName() + " is ready (room).");

        // If both players are ready, start the game and send the first turn
        if (players.get(0).isReady() && players.get(1).isReady()) {
            players.get(0).sendMessage("TURN 1");
            players.get(1).sendMessage("TURN 1");
            players.forEach(p -> p.setReady(false)); // Reset readiness flags
        }
    }

    // Called when a rematch request is received
    public synchronized void requestRematch(ClientHandler who) {
        rematchRequests.add(who);
        System.out.println(who.getPlayerName() + " requested rematch (" + rematchRequests.size() + "/2)");

        // If both players want a rematch, reset the game and start again
        if (rematchRequests.size() == 2) {
            for (ClientHandler p : players) {
                p.getBoard().reset();     // Reset the board
                p.setReady(false);        // Set the player as not ready
            }
            rematchRequests.clear();      // Clear rematch requests

            for (ClientHandler p : players) {
                p.sendMessage("REMATCH_START"); // Notify clients to start rematch
            }

            start(); // Start the game again
        }
    }

    // Returns the list of players as read-only
    public List<ClientHandler> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    // Returns the opponent of the given player
    public ClientHandler getOpponent(ClientHandler you) {
        return players.get(0) == you ? players.get(1) : players.get(0);
    }
}
