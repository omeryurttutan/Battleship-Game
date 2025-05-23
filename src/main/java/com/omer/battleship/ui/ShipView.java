package com.omer.battleship.ui;

import com.omer.battleship.model.Ship;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * This class represents the visual component of a Ship.
 * It creates an ImageView based on the ship's size and orientation.
 */
public class ShipView {
    private final ImageView imageView;

    public ShipView(Ship model) {
        // Determine the image path based on ship size and orientation
        String prefix = "/images/ship" + model.getSize();
        String path = model.isHorizontal()
                ? prefix + "_horizontal.png"
                : prefix + "_vertical.png";

        // Load the image from resources
        Image img = new Image(getClass().getResourceAsStream(path));
        imageView = new ImageView(img);
        imageView.setPreserveRatio(true);

        // Set the size based on the ship's orientation
        if (model.isHorizontal()) {
            imageView.setFitWidth(model.getSize() * Ship.CELL_SIZE);
        } else {
            imageView.setFitHeight(model.getSize() * Ship.CELL_SIZE);
        }
    }

    // Return the ImageView node to be added to the scene
    public ImageView getNode() {
        return imageView;
    }
}
