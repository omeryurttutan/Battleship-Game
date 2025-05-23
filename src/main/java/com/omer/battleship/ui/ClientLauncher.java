package com.omer.battleship.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// Entry point of the JavaFX application
public class ClientLauncher extends Application {

    private static Stage primaryStage; // Reference to the main window
    private static Scene scene;        // Shared scene instance

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        scene = new Scene(loadFXML("secondary"), 600, 400); // Load the initial scene
        stage.setScene(scene);
        stage.setTitle("Battleship"); // Window title
        stage.show();                 // Show the window
    }

    // Can be used to change the root of the scene
    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    // Loads the FXML file and returns its root element
    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(
                ClientLauncher.class.getResource("/fxml/" + fxml + ".fxml")
        );
        return fxmlLoader.load();
    }

    // Returns the main stage (used for access in other classes)
    public static Stage getStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args); // Start the JavaFX application
    }
}
