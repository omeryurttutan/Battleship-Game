package com.omer.battleship.ui;

import com.omer.battleship.game.GameManager;
import com.omer.battleship.model.Ship;
import com.omer.battleship.model.ShotResult;
import com.omer.battleship.model.Cell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Optional;

public class BattleshipClientApp extends Application implements UiController {
    private static BattleshipClientApp instance; // Singleton reference
    private static final int CELL_SIZE = Ship.CELL_SIZE; // Cell size constant

    private Label statusLabel;              // Label to show game status
    private GameManager gameManager;        // Game controller (game logic)
    private GridPane boardGrid;             // Player's own board
    private GridPane enemyGrid;             // Opponent's board
    private boolean placementLocked = false; // Lock after ships are placed
    private VBox shipBox;                   // Box containing draggable ships
    private Ship selectedShip;              // Currently selected (dragged) ship
    private final Pane[][] cells = new Pane[10][10];         // Cells on player board
    private final Pane[][] enemyCells = new Pane[10][10];     // Cells on enemy board
    private StackPane boardWrapper;         // Player board with background image
    private StackPane enemyWrapper;         // Enemy board with background image

    // Constructor
    public BattleshipClientApp() {
        instance = this;
    }

    public BattleshipClientApp(GameManager controller) {
        this();
        this.gameManager = controller;
        controller.setUiController(this); // Connects game logic with UI
    }

    public static BattleshipClientApp getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        final int BOARD_PX = 10 * CELL_SIZE; // Board size in pixels

        Label title = new Label("⚓ BATTLESHIP"); // Title label
        title.setStyle("-fx-font-size:22px;-fx-font-weight:bold;");

        statusLabel = new Label("⏳ WAITING FOR OTHER PLAYERS..."); // Initial game status

        // Create the player's board with background image
        ImageView boardBg = loadBoardImage(BOARD_PX);
        boardGrid = createGrid(cells, true); // Grid for player
        boardWrapper = new StackPane(boardBg, boardGrid);
        boardWrapper.setPrefSize(BOARD_PX, BOARD_PX);
        installDragHandlers(); // Setup drag-and-drop

        // Create the enemy board (initially hidden)
        ImageView enemyBg = loadBoardImage(BOARD_PX);
        enemyGrid = createGrid(enemyCells, false);
        enemyGrid.setVisible(false);
        enemyWrapper = new StackPane(enemyBg, enemyGrid);
        enemyWrapper.setPrefSize(BOARD_PX, BOARD_PX);

        VBox enemyBox = new VBox(10, new Label("ENEMY GRID"), enemyWrapper);
        enemyBox.setAlignment(Pos.TOP_CENTER);
        enemyBox.setVisible(false);

        // Create ship box with draggable ships
        shipBox = new VBox(20);
        shipBox.setAlignment(Pos.TOP_CENTER);
        shipBox.setStyle("-fx-padding:20;");
        shipBox.setPrefWidth(5 * CELL_SIZE + 40);
        createShip(2);
        createShip(3);
        createShip(4);
        createShip(5);

        // "READY" button – Sends message to server if all ships are placed
        Button readyBtn = new Button("READY");
        readyBtn.setOnAction(e -> {
            final int REQUIRED = 4;
            int placed = gameManager.getBoard().getShips().size();
            if (placed < REQUIRED) {
                statusLabel.setText(String.format(
                        "⚠ PLACE ALL OF THE SHIPS! (%d/%d)", placed, REQUIRED));
                return;
            }
            gameManager.sendReady(); // Notify server
            readyBtn.setDisable(true);
            placementLocked = true;
            statusLabel.setText("⏳ WAITING FOR OPPONENT...");
        });

        // Layout setup
        VBox left = new VBox(20, boardWrapper, new Label("YOUR GRID"));
        left.setAlignment(Pos.CENTER);
        HBox main = new HBox(50, left, shipBox);
        main.setAlignment(Pos.TOP_CENTER);
        HBox enemyHolder = new HBox(enemyBox);
        enemyHolder.setAlignment(Pos.CENTER);
        enemyHolder.setPadding(new Insets(20, 0, 0, 0));

        VBox root = new VBox(20, title, main, readyBtn, statusLabel, enemyHolder);
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-padding:20;");

        stage.setScene(new Scene(root, 1000, 900)); // Create scene
        stage.setTitle("Battleship");
        stage.centerOnScreen();
        stage.show();
    }

    // Creates the grid (for player or opponent)
    private GridPane createGrid(Pane[][] store, boolean forPlayer) {
        GridPane g = new GridPane();
        g.setGridLinesVisible(false);
        for (int i = 0; i < 10; i++) {
            g.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
            g.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 10; c++) {
                Pane p = new Pane();
                p.setMinSize(CELL_SIZE, CELL_SIZE);
                p.setMaxSize(CELL_SIZE, CELL_SIZE);
                if (!forPlayer) {
                    // Fire at opponent's cell when clicked
                    final int rr = r, cc = c;
                    p.setOnMouseClicked(e -> {
                        if (!enemyGrid.isDisabled()) {
                            gameManager.sendFire(rr, cc);
                            enemyGrid.setDisable(true);
                            p.setDisable(true);
                        }
                    });
                }
                store[r][c] = p;
                g.add(p, c, r);
            }
        }
        return g;
    }

    // Clears the ship image from the board
    private void clearShipVisual(Ship ship) {
        String wrapperId = "ship_wrapper_" + ship.getSize();
        boardGrid.getChildren().removeIf(node ->
                wrapperId.equals(node.getId())
        );
    }

    // Loads the background image
    private ImageView loadBoardImage(double px) {
        var url = getClass().getResource("/images/board.png");
        if (url == null) throw new IllegalStateException("/images/board.png not found");
        ImageView iv = new ImageView(url.toExternalForm());
        iv.setFitWidth(px);
        iv.setFitHeight(px);
        iv.setPreserveRatio(false);
        return iv;
    }

    // Defines drag-and-drop behavior for placing ships on the player board
    private void installDragHandlers() {
        // Listen for drag events on all cells
        for (Node node : boardGrid.getChildren()) {
            if (!(node instanceof Pane)) continue;
            Pane cell = (Pane) node;
            Integer r = GridPane.getRowIndex(cell);
            Integer c = GridPane.getColumnIndex(cell);
            if (r == null || c == null) continue;

            // When a ship is dragged over a cell
            cell.setOnDragOver(e -> {
                // If the game hasn't started and a ship is selected
                if (!placementLocked && selectedShip != null && e.getDragboard().hasString()) {
                    clearHighlight(); // Clear previous highlights
                    int sr = r, sc = c;
                    boolean valid = true;

                    // Check if the ship fits and doesn't overlap other ships
                    for (int i = 0; i < selectedShip.getSize(); i++) {
                        int rr = sr + (selectedShip.isHorizontal() ? 0 : i);
                        int cc = sc + (selectedShip.isHorizontal() ? i : 0);
                        if (rr < 0 || rr >= 10 || cc < 0 || cc >= 10 || gameManager.getBoard().getCells()[rr][cc].hasShip()) {
                            valid = false;
                            break;
                        }
                    }

                    // If placement is valid, highlight and allow drop
                    if (valid) {
                        highlightCells(sr, sc, selectedShip);
                        e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    }
                }
                e.consume();
            });
        }

        // When a ship is dropped onto the board
        boardWrapper.setOnDragDropped(e -> {
            if (selectedShip != null) {
                Node n = e.getPickResult().getIntersectedNode();
                while (n != null && !(n instanceof Pane && GridPane.getRowIndex(n) != null)) {
                    n = n.getParent();
                }

                if (n instanceof Pane) {
                    int row = GridPane.getRowIndex(n);
                    int col = GridPane.getColumnIndex(n);
                    boolean ok = gameManager.placeShip(selectedShip, row, col);
                    if (ok) refreshBoardUI();
                    else System.out.println("⚠ INVALID POSITION.");
                    e.setDropCompleted(ok);
                } else {
                    e.setDropCompleted(false);
                }
                selectedShip = null; // Reset selection
            }
            e.consume();
        });

        // When drag leaves the board area, clear highlights
        boardWrapper.setOnDragExited(e -> {
            clearHighlight();
            e.consume();
        });
    }


    // Temporarily highlights the cells where the ship will be placed in blue
    private void highlightCells(int sr, int sc, Ship ship) {
        for (int i = 0; i < ship.getSize(); i++) {
            int r = sr + (ship.isHorizontal() ? 0 : i);
            int c = sc + (ship.isHorizontal() ? i : 0);
            if (r >= 0 && r < 10 && c >= 0 && c < 10)
                cells[r][c].setStyle("-fx-background-color: rgba(100,100,255,0.4);");
        }
    }

    // Clears the highlights (resets all cells to their original state)
    private void clearHighlight() {
        for (Pane[] row : cells)
            for (Pane p : row)
                p.setStyle("");
    }

    // Creates a ship of the given size and adds it to the shipBox (sidebar)
    private void createShip(int size) {
        Ship ship = new Ship(size);
        ShipView shipView = new ShipView(ship);
        ImageView iv = shipView.getNode();
        iv.setId("ship_" + size);

        // When clicked, rotates the ship
        iv.setOnMouseClicked(e -> {
            if (!placementLocked && shipBox.getChildren().contains(iv)) {
                ship.rotate();
                Image img = new Image(getClass().getResourceAsStream(
                        "/images/ship" + ship.getSize() +
                                (ship.isHorizontal() ? "_horizontal.png" : "_vertical.png")
                ));
                iv.setImage(img);
                iv.setFitWidth(ship.isHorizontal() ? ship.getSize() * CELL_SIZE : CELL_SIZE);
                iv.setFitHeight(ship.isHorizontal() ? CELL_SIZE : ship.getSize() * CELL_SIZE);
            }
        });

        // When drag is started, puts data on the dragboard
        iv.setOnDragDetected(e -> {
            if (!placementLocked) {
                Dragboard db = iv.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent cc = new ClipboardContent();
                cc.putString("ship");
                db.setContent(cc);
                selectedShip = ship;
                e.consume();
            }
        });

        shipBox.getChildren().add(iv);
    }

    // Redraws all ships on the board
    private void refreshBoardUI() {
        for (Pane[] row : cells)
            for (Pane cell : row)
                cell.getChildren().removeIf(n -> n instanceof StackPane);

        for (Ship sh : gameManager.getBoard().getShips()) {
            if (!sh.getCells().isEmpty()) {
                Cell start = sh.getCells().get(0);
                placeShipUI(start.getRow(), start.getCol(), sh);
            }
        }
    }

    // Resets the UI when a rematch is started
    public void resetForRematch() {
        statusLabel.setText("waiting for other player to accept play again");
        shipBox.setVisible(true);
        shipBox.setManaged(true);
        placementLocked = false;
        boardGrid.setDisable(false);
        enemyGrid.setVisible(false);

        // Clear all cells
        for (Pane[] row : cells) for (Pane cell : row) {
            cell.getChildren().removeIf(n -> n instanceof StackPane);
            cell.setStyle("");
        }
        refreshBoardUI();
    }

    // Visually places a ship on the board
    private void placeShipUI(int row, int col, Ship ship) {
        if (placementLocked) return;
        clearShipVisual(ship);

        ShipView view = new ShipView(ship);
        ImageView iv = view.getNode();
        iv.setId("ship_" + ship.getSize());

        StackPane wrap = new StackPane(iv);
        wrap.setId("ship_wrapper_" + ship.getSize());
        wrap.setPrefSize(
                ship.isHorizontal() ? ship.getSize() * CELL_SIZE : CELL_SIZE,
                ship.isHorizontal() ? CELL_SIZE : ship.getSize() * CELL_SIZE
        );

        // When the ship is dragged back, move it to the sidebar
        wrap.setOnDragDetected(ev -> {
            if (placementLocked) return;
            Dragboard db = wrap.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent cc = new ClipboardContent();
            cc.putString("ship");
            db.setContent(cc);
            selectedShip = ship;
            gameManager.getBoard().removeShip(ship);
            boardGrid.getChildren().remove(wrap);
            addShipToBox(ship);
            ev.consume();
        });

        GridPane.setColumnIndex(wrap, col);
        GridPane.setRowIndex(wrap, row);
        GridPane.setColumnSpan(wrap, ship.isHorizontal() ? ship.getSize() : 1);
        GridPane.setRowSpan(wrap, ship.isHorizontal() ? 1 : ship.getSize());
        boardGrid.getChildren().add(wrap);

        // Remove the related ship from the sidebar
        shipBox.getChildren().removeIf(node -> node instanceof ImageView &&
                ("ship_" + ship.getSize()).equals(node.getId()));

        selectedShip = null;
    }

    /** Helper method that adds the ship to shipBox with the same event handlers as in placeShipUI */
    private void addShipToBox(Ship ship) {
        ShipView view = new ShipView(ship);
        ImageView iv2 = view.getNode();
        iv2.setId("ship_" + ship.getSize());

        // When clicked, rotate and update the view
        iv2.setOnMouseClicked(e -> {
            if (placementLocked) return;
            ship.rotate();
            updateShipBoxView(iv2, ship);
        });

        // Start drag operation, handled by placeShipUI
        iv2.setOnDragDetected(e -> {
            if (placementLocked) return;
            Dragboard db2 = iv2.startDragAndDrop(TransferMode.COPY_OR_MOVE);
            ClipboardContent cc2 = new ClipboardContent();
            cc2.putString("ship");
            db2.setContent(cc2);
            selectedShip = ship;
            e.consume();
        });

        shipBox.getChildren().add(iv2);
    }

    /** Refreshes the visual of a ship in the ship box when it is rotated */
    private void updateShipBoxView(ImageView iv, Ship ship) {
        ShipView view = new ShipView(ship);
        ImageView fresh = view.getNode();
        iv.setImage(fresh.getImage());
        iv.setFitWidth(fresh.getFitWidth());
        iv.setFitHeight(fresh.getFitHeight());
    }

    // Visually marks the cell on the player's board with an icon (X or empty)
    @Override
    public void markMyBoard(int row, int col, ShotResult result) {
        boardGrid.getChildren().removeIf(node ->
                node instanceof ImageView &&
                        "markMy".equals(node.getId()) &&
                        Integer.valueOf(row).equals(GridPane.getRowIndex(node)) &&
                        Integer.valueOf(col).equals(GridPane.getColumnIndex(node))
        );

        String path = result == ShotResult.MISS ? "/images/empty.png" : "/images/cross.png";
        ImageView icon = new ImageView(getClass().getResource(path).toExternalForm());
        icon.setFitWidth(CELL_SIZE);
        icon.setFitHeight(CELL_SIZE);
        icon.setPreserveRatio(false);
        icon.setId("markMy");

        GridPane.setColumnIndex(icon, col);
        GridPane.setRowIndex(icon, row);
        boardGrid.getChildren().add(icon);
        icon.toFront();
    }

    // Marks the result of an attack on the enemy's board
    @Override
    public void markEnemyBoard(int row, int col, ShotResult result) {
        enemyGrid.getChildren().removeIf(node ->
                node instanceof ImageView &&
                        "markEn".equals(node.getId()) &&
                        Integer.valueOf(row).equals(GridPane.getRowIndex(node)) &&
                        Integer.valueOf(col).equals(GridPane.getColumnIndex(node))
        );

        String path = result == ShotResult.MISS ? "/images/empty.png" : "/images/cross.png";
        ImageView icon = new ImageView(getClass().getResource(path).toExternalForm());
        icon.setFitWidth(CELL_SIZE);
        icon.setFitHeight(CELL_SIZE);
        icon.setPreserveRatio(false);
        icon.setId("markEn");

        GridPane.setColumnIndex(icon, col);
        GridPane.setRowIndex(icon, row);
        enemyGrid.getChildren().add(icon);
        icon.toFront();
    }

    @Override public void enableFireMode(boolean active) { enemyGrid.setDisable(!active); }
    @Override public void updateStatus(String text) { statusLabel.setText(text); }
    @Override public void showEnemyBoard() {
        ((VBox) enemyWrapper.getParent()).setVisible(true);
        enemyGrid.setVisible(true);
        shipBox.setVisible(false);
        shipBox.setManaged(false);
    }

    // Displays the result popup when the game ends and prompts for a rematch
    @Override
    public void showGameOver(boolean isWin, int hits, int shots) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("GAME OVER");
        alert.setHeaderText(isWin ? "🎉 YOU WIN!" : "💥 YOU LOSE!");
        alert.setContentText(String.format("Would you like to play again?", hits, shots));

        ButtonType rematch = new ButtonType("PLAY AGAIN");
        ButtonType exit    = new ButtonType("EXIT", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(rematch, exit);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == rematch) {
            gameManager.sendRematch();
            resetForRematch();
        } else {
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

