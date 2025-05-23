package com.omer.battleship.client;

import com.omer.battleship.game.GameManager;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class Client {
    private final Socket socket;                  // Socket used to connect to the server
    private final BufferedReader in;              // To read messages from the server
    private final PrintWriter out;                // To send messages to the server
    private Consumer<String> messageHandler;      // Functional interface to handle incoming messages
    private GameManager controller;               // Class that controls game logic (currently just set)

    public Client(String serverIp, int port) throws IOException {
        socket = new Socket(serverIp, port);                             // Connect to the server
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Create input stream
        out = new PrintWriter(socket.getOutputStream(), true);          // Create output stream (autoFlush enabled)
        new Thread(this::listen).start();                               // Start a separate thread to listen for messages
    }

    private void listen() {
        try {
            String line;
            // Continuously read messages from the server
            while ((line = in.readLine()) != null) {
                if (messageHandler != null) {
                    messageHandler.accept(line); // If there's a message handler, pass the message to it
                }
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace on error
        }
    }

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler; // Set how to handle incoming messages
    }

    public void sendMessage(String msg) {
        out.println(msg); // Send a message to the server
    }

    public void close() throws IOException {
        socket.close(); // Close the connection
    }

    public void setController(GameManager controller) {
        this.controller = controller; // Assign controller (not used currently)
    }
}
