package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.RomSetUtil;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.GameRendererFactory;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerFactory;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Pane;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Optional;

public class MainAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private ApplicationContext applicationContext;
    private GameRenderer gameRenderer;

    @FXML
    private Pane applicationPane;

    @FXML
    private Pane gamePreviewPane;

    @FXML
    private Pane menuPreviewPane;

    @FXML
    private Pagination menuPagination;

    private ImageView menuPreview;

    @FXML
    private ImageView gamePreview;

    @FXML
    private TableView<Game> gameTable;

    @FXML
    private TableColumn<Game, String> nameColumn;

    @FXML
    private Button createRomButton;

    @FXML
    private Button addRomButton;

    @FXML
    private Button removeSelectedRomButton;

    @FXML
    private Button clearRomsetButton;

    @FXML
    private ProgressIndicator operationInProgressIndicator;

    @FXML
    private Pane romSetHandlerInfoPane;

    private Pane playerPane;
    private PlayerController playerController;
    private boolean playerVisible = false;

    private boolean iannaMode = false;

    public MainAppController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private RomSetHandler getRomSetHandler() {
        return getApplicationContext().getRomSetHandler();
    }

    private boolean interceptSpecialRomSet(List<File> files) {
        LOGGER.debug("interceptSpecialRomSet " + files);
        Optional<InputStream> screenResource;
        if (files.size() == 1 &&
                (screenResource = RomSetUtil.getRomScreenResource(files.get(0))).isPresent()) {
            PlayerConfiguration.getInstance()
                    .setCustomRomSetPath(files.get(0).getPath());
            applicationContext.getGameList().clear();
            menuPagination.setCurrentPageIndex(1);
            try {
                gameRenderer.loadImage(screenResource.get());
                iannaMode = true;
                return true;
            } catch (Exception e) {
                LOGGER.error("Loading known rom screen", e);
                return false;
            }
        } else {
            if (iannaMode == true) {
                gameRenderer.previewGame(null);
                menuPagination.setCurrentPageIndex(0);
                PlayerConfiguration.getInstance()
                        .setCustomRomSetPath(null);
                iannaMode = false;
            }
            return false;
        }
    }

    private void addGamesFromFiles(List<File> files) {
        if (!interceptSpecialRomSet(files)) {
            files.forEach(file ->
                    applicationContext.addBackgroundTask(() -> {
                        Optional<Game> gameOptional = GameUtil.createGameFromFile(file);
                        if (gameOptional.isPresent()) {
                            Platform.runLater(() -> getRomSetHandler().addGame(gameOptional.get()));
                        } else {
                            Platform.runLater(() -> {
                                try (FileInputStream fis = new FileInputStream(file)) {
                                    if (getApplicationContext().getGameList().isEmpty()) {
                                        getRomSetHandler().importRomSet(fis);
                                    } else {
                                        getRomSetHandler().mergeRomSet(fis);
                                    }
                                } catch (Exception e) {
                                    LOGGER.error("Importing ROMSet", e);
                                    DialogUtil.buildErrorAlert(
                                            LocaleUtil.i18n("fileImportError"),
                                            LocaleUtil.i18n("fileImportErrorHeader"),
                                            LocaleUtil.i18n("fileImportErrorContent"))
                                            .showAndWait();
                                }
                            });
                        }
                        return OperationResult.successResult();
                    }));
        }
    }

    private void updateRomSetHandler() {
        applicationContext.setRomSetHandler(
                RomSetHandlerFactory.getHandler(Configuration.getInstance().getMode()));
        createRomButton.disableProperty()
                .bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                        .or(applicationContext.getRomSetHandler().generationAllowedProperty().not()));
    }

    private boolean acceptsDndSelectedGame() {
        if (applicationContext.getGameSelected()) {
            Game selectedGame = applicationContext.selectedGameProperty().get();
            return selectedGame.getType() != GameType.ROM;
        } else {
            return false;
        }
    }

    private void updateGameScreen(File screenFile) throws IOException {
        if (ImageUtil.isValidScreenFile(screenFile)) {
            Game selectedGame = applicationContext.selectedGameProperty().get();
            if (selectedGame instanceof SnapshotGame) {
                ((SnapshotGame) selectedGame).updateScreen(Util.fromInputStream(new FileInputStream(screenFile)));
                gameRenderer.previewGame(selectedGame);
            }
        } else {
            LOGGER.warn("Ignoring invalid provided screen file");
        }
    }

    @FXML
    private void initialize() throws IOException {
        menuPreview = new ImageView();
        
        menuPreview.fitWidthProperty().bind(menuPreviewPane.widthProperty());
        menuPreview.fitHeightProperty().bind(menuPreviewPane.heightProperty());

        gamePreview.fitWidthProperty().bind(gamePreviewPane.widthProperty());
        gamePreview.fitHeightProperty().bind(gamePreviewPane.heightProperty());
        menuPagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        menuPagination.setPageFactory((index) -> {
            switch (index) {
                case 0:
                    return menuPreview;
                case 1:
                    return getPlayerPane();
                default:
                    return null;
            }
        });

        applicationContext.setRomSetHandlerInfoPane(romSetHandlerInfoPane);
        applicationContext.setMenuPreview(menuPreview);
        applicationContext.setSelectedGameProperty(gameTable.getSelectionModel().selectedItemProperty());

        gameRenderer = GameRendererFactory.getDefaultRenderer();
        gameRenderer.setTarget(gamePreview);
        updateRomSetHandler();

        clearRomsetButton.disableProperty()
                .bind(Bindings.size(applicationContext.getGameList())
                        .isEqualTo(0));

        gameTable.setItems(applicationContext.getGameList());
        gameTable.setPlaceholder(new Label(LocaleUtil.i18n("dropGamesMessage")));

        operationInProgressIndicator.visibleProperty().bind(
                applicationContext.backgroundTaskCountProperty().greaterThan(0));

        onGameSelection(null, null);

        gameTable.setRowFactory(rf -> {
            TableRow<Game> row = new TableRow<>();
            row.setOnDragDetected(event -> {
                if (!row.isEmpty()) {
                    Integer index = row.getIndex();
                    LOGGER.debug("Dragging content of row " + index);
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    db.setDragView(row.snapshot(null, null));
                    ClipboardContent cc = new ClipboardContent();
                    cc.put(SERIALIZED_MIME_TYPE, index);
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    if (row.getIndex() != (Integer) db.getContent(SERIALIZED_MIME_TYPE)) {
                        event.acceptTransferModes(TransferMode.MOVE);
                        event.consume();
                    }
                }
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                LOGGER.debug("row.setOnDragDropped: " + db);
                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                    int draggedIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                    Game draggedGame = gameTable.getItems().remove(draggedIndex);

                    int dropIndex ;

                    if (row.isEmpty()) {
                        dropIndex = gameTable.getItems().size();
                    } else {
                        dropIndex = row.getIndex();
                    }

                    gameTable.getItems().add(dropIndex, draggedGame);

                    event.setDropCompleted(true);
                    gameTable.getSelectionModel().select(dropIndex);
                    event.consume();
                } else {
                    LOGGER.debug("Dragboard content is not of the required type");
                }
            });

            row.setOnMouseClicked(e -> {
                if (row.isEmpty()) {
                    gameTable.getSelectionModel().clearSelection();
                }
            });
            return row;
        });

        nameColumn.setCellValueFactory(
                cellData -> cellData.getValue().nameProperty());
        nameColumn.setCellFactory(TextFieldTableCell.forTableColumn(
                new StringConverter<String>() {
                    @Override
                    public String toString(String value) {
                        return value;
                    }

                    @Override
                    public String fromString(String value) {
                        return GameUtil.filterGameName(value);
                    }
                }));


        gameTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onGameSelection(oldValue, newValue));


        gameTable.setOnDragOver(event -> {
            if (event.getGestureSource() != gameTable &&
                    event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        gameTable.setOnDragEntered(Event::consume);

        gameTable.setOnDragExited(Event::consume);

        gameTable.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            LOGGER.debug("onDragDropped. Transfer modes are " + db.getTransferModes());
            boolean success = false;
            if (db.hasFiles()) {
                addGamesFromFiles(db.getFiles());
                success = true;
            }
            /* let the source know whether the files were successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

        gamePreview.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles() && acceptsDndSelectedGame()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        gamePreview.setOnDragEntered(Event::consume);
        gamePreview.setOnDragExited(Event::consume);
        gamePreview.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                if (db.getFiles().size() == 1) {
                    try {
                        updateGameScreen(db.getFiles().get(0));
                    } catch (IOException e) {
                        LOGGER.error("Updating Game screen", e);
                    }
                }
                LOGGER.debug("And we had got some files: " + db.getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        createRomButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveRomSet"));
            chooser.setInitialFileName("dandanator_" + Constants.currentVersion() + ".rom");
            final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
            if (saveFile != null) {
                try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                    getApplicationContext().getRomSetHandler().exportRomSet(fos);
                } catch (IOException e) {
                    LOGGER.error("Creating ROM Set", e);
                }
            }
        });

        addRomButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("openSnapshot"));
            final List<File> snapshotFiles = chooser.showOpenMultipleDialog(addRomButton.getScene().getWindow());
            if (snapshotFiles != null) {
                try {
                    addGamesFromFiles(snapshotFiles);
                } catch (Exception e) {
                    LOGGER.error("Opening snapshots from files " + snapshotFiles, e);
                }
            }
        });

        removeSelectedRomButton.setOnAction(c -> {
            Optional<Game> selectedGame = Optional.of(gameTable.getSelectionModel().getSelectedItem());
            selectedGame.ifPresent(index -> applicationContext.getRomSetHandler().removeGame(selectedGame.get()));
        });

        clearRomsetButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK){
                applicationContext.getGameList().clear();
                applicationContext.setEepromLoaderIncluded(true);
            }
        });

        Configuration.getInstance().modeProperty().addListener(
                (observable, oldValue, newValue) -> updateRomSetHandler());
    }


    private void updateExportGameMenuEntryMessage(Game selectedGame) {
        applicationContext.setExportGameMenuEntryMessage(
                selectedGame == null ? LocaleUtil.i18n("exportGameMenuEntry") :
                selectedGame.getType() == GameType.ROM ?
                    LocaleUtil.i18n("exportGameAsRomMenuEntry") :
                        selectedGame.getType() == GameType.DAN_TAP ?
                                LocaleUtil.i18n("exportGameAsTapMenuEntry") :
                            GameType.isMLD(selectedGame.getType()) ?
                                LocaleUtil.i18n("exportGameAsMLDMenuEntry") :
                                LocaleUtil.i18n("exportGameAsZ80MenuEntry"));
    }

    private void onGameSelection(Game oldGame, Game newGame) {
        LOGGER.debug("onGameSelection oldGame=" + oldGame+ ", newGame=" + newGame);
        gameRenderer.previewGame(newGame);
        updateExportGameMenuEntryMessage(newGame);
        if (newGame == null) {
            removeSelectedRomButton.setDisable(true);
        } else {
            removeSelectedRomButton.setDisable(false);
        }
    }

    private PlayerController getPlayerController(ApplicationContext applicationContext) {
        if (playerController == null) {
            playerController = new PlayerController(applicationContext);
        }
        return playerController;
    }

    private Pane getPlayerPane() {
        try {
            if (playerPane == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(MainAppController.class.getResource("player.fxml"));
                loader.setController(getPlayerController(applicationContext));
                loader.setResources(LocaleUtil.getBundle());
                playerPane = loader.load();
            }
            return playerPane;
        } catch (Exception e) {
            LOGGER.error("Creating Player", e);
            throw new RuntimeException(e);
        }
    }

}
