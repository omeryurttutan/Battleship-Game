package com.omer.battleship.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BattleshipServer {

    private static final int PORT = 8000; // The port that the server will listen on

    // Players who connected to the lobby but are not yet matched
    private static final List<ClientHandler> lobby = new CopyOnWriteArrayList<>();

    // List of active game rooms
    private static final List<GameRoom> rooms = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Server starting, port: " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket sock = serverSocket.accept();           // Accept a new connection
                ClientHandler handler = new ClientHandler(sock); // Create a handler to manage this client
                new Thread(handler).start();                    // Run each client in a separate thread
            }
        }
    }

    // Adds a player to the lobby and starts the game if there's another waiting player
    public static synchronized void joinLobby(ClientHandler h) {
        lobby.add(h);
        System.out.println(h.getPlayerName() + " joined the lobby. Total: " + lobby.size());

        // If there are at least 2 players in the lobby, create a game room
        if (lobby.size() >= 2) {
            ClientHandler p1 = lobby.remove(0);
            ClientHandler p2 = lobby.remove(0);
            GameRoom room = new GameRoom(p1, p2);
            rooms.add(room);

            p1.setRoom(room);
            p2.setRoom(room);

            room.start(); // Start the game
        }
    }
}
