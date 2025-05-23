package com.omer.battleship.ui;

import com.omer.battleship.client.Client;
import com.omer.battleship.game.GameManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

// FXML controller class that manages the login screen
public class LoginScreenController {

    @FXML private TextField nameField;     // Field for entering the player's name
    @FXML private Label statusLabel;       // Displays connection and status messages
    @FXML private Button submitBtn;        // Button to submit the name
    @FXML private Button connectBtn;       // Button to initiate connection
    @FXML private TextField ipField;       // Field to input the server's IP address

    private Client client;
    private GameManager controller;

    // Runs when the page is first loaded
    @FXML
    public void initialize() {
        nameField.setDisable(true); // Initially disable name field until connection is made
    }

    // Runs when the IP is entered and connection to the server is attempted
    @FXML
    private void connect() {
        String ip = ipField.getText().trim();
        if (ip.isBlank()) {
            statusLabel.setText("IP cannot be empty!");
            return;
        }

        try {
            // Connect to server and start controller
            client = new Client(ip, 8000);
            controller = new GameManager(client);
            client.setController(controller);

            // Handle incoming messages from the server
            client.setMessageHandler(msg -> {
                if (msg.startsWith("START")) {
                    // If a game start message is received, switch the UI
                    Platform.runLater(() -> {
                        try {
                            new BattleshipClientApp(controller).start(ClientLauncher.getStage()); // Start new scene
                            controller.handleServerMessage(msg);          // Process START message
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    controller.handleServerMessage(msg); // Process other messages
                }
            });

            // Connection successful, allow user to enter name
            statusLabel.setText("Connected ✔ Now enter your name:");
            connectBtn.setDisable(true);
            ipField.setDisable(true);
            nameField.setDisable(false);

        } catch (IOException ex) {
            statusLabel.setText("Connection failed: " + ex.getMessage());
        }
    }

    // Runs when the player submits their name
    @FXML
    private void submitName() {
        if (client == null) {
            statusLabel.setText("Connect using IP first.");
            return;
        }

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("Name cannot be empty!");
            return;
        }

        // Send name and join command to the server
        client.sendMessage("NAME " + name);
        client.sendMessage("JOIN");

        statusLabel.setText("⏳ Waiting for another player...");
        submitBtn.setDisable(true);
        nameField.setDisable(true);
    }
}
