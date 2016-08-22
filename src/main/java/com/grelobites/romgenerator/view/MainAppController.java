package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.GameRendererFactory;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerFactory;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.beans.binding.Bindings;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainAppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private ApplicationContext applicationContext;
    private GameRenderer gameRenderer;

    @FXML
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

    public MainAppController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    private RomSetHandler getRomSetHandler() {
        return getApplicationContext().getRomSetHandler();
    }

    private void addSnapshotFiles(List<File> files) {
        files.stream()
                .map(GameUtil::createGameFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(g -> getRomSetHandler().addGame(g));
    }

    private void updateRomSetHandler() {
        applicationContext.setRomSetHandler(
                RomSetHandlerFactory.getHandler(Configuration.getInstance().getMode()));
        createRomButton.disableProperty()
                .bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                        .or(applicationContext.getRomSetHandler().generationAllowedProperty().not()));
    }

    @FXML
    private void initialize() throws IOException {
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
            LOGGER.debug("onDragDropped");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                addSnapshotFiles(db.getFiles());
                success = true;
            }
                /* let the source know whether the files were successfully
                 * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

        createRomButton.setOnAction(c -> {
            DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveRomSet"));
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
                    addSnapshotFiles(snapshotFiles);
                } catch (Exception e) {
                    LOGGER.error("Opening snapshots from files " + snapshotFiles, e);
                }
            }
        });

        removeSelectedRomButton.setOnAction(c -> {
            Optional<Integer> selectedIndex = Optional.of(gameTable.getSelectionModel().getSelectedIndex());
            selectedIndex.ifPresent(index -> applicationContext.getGameList().remove(index.intValue()));
        });

        clearRomsetButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.orElse(ButtonType.CANCEL) == ButtonType.OK){
                applicationContext.getGameList().clear();
            }
        });

        Configuration.getInstance().modeProperty().addListener(
                (observable, oldValue, newValue) -> updateRomSetHandler());
    }



    private void onGameSelection(Game oldGame, Game newGame) {
        LOGGER.debug("onGameSelection oldGame=" + oldGame+ ", newGame=" + newGame);
        gameRenderer.previewGame(newGame);
        if (newGame == null) {
            removeSelectedRomButton.setDisable(true);
        } else {
            removeSelectedRomButton.setDisable(false);
        }
    }

}
