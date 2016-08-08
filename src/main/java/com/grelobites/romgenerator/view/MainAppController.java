package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.PokeViewable;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.gamerenderer.GameRenderer;
import com.grelobites.romgenerator.util.gamerenderer.GameRendererFactory;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerFactory;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.PokeEntityTreeCell;
import com.grelobites.romgenerator.view.util.RecursiveTreeItem;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainAppController {
	private static final Logger LOGGER = LoggerFactory.getLogger(MainAppController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

    private ObservableList<Game> gameList;
    private RomSetHandler romSetHandler;
    private GameRenderer gameRenderer;

    @FXML
	private ImageView menuPreviewImage;
	
	@FXML
	private ImageView gamePreviewImage;
	
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
    private TreeView<PokeViewable> pokeView;

    @FXML
    private Button addPokeButton;

    @FXML
    private Button removeSelectedPokeButton;

    @FXML
    private Button removeAllGamePokesButton;

    @FXML
    private ProgressBar pokesCurrentSizeBar;

    @FXML
    private TabPane gameInfoTabPane;

    @FXML
    private Tab gameInfoTab;

    @FXML
    private Tab pokesTab;

    @FXML
    private AnchorPane gameInfoPane;

    @FXML
    private TextField gameName;

    @FXML
    private Label gameType;

    @FXML
    private CheckBox gameRomAttribute;

    @FXML
    private CheckBox gameHoldScreenAttribute;

    @FXML
    private Label compressedSize;

    public MainAppController() {
        this.gameList = FXCollections.observableArrayList(game -> game.getObservable());
    }

    public ImageView getMenuPreviewImage() {
        return menuPreviewImage;
    }

    public ImageView getGamePreviewImage() {
        return gamePreviewImage;
    }

    public ObservableList<Game> getGameList() {
        return gameList;
    }

    private void onGameListChange() {
        LOGGER.debug("onGameListChange");
    	createRomButton.setDisable(getGameList().isEmpty());
        clearRomsetButton.setDisable(getGameList().isEmpty());
    	//recreatePreviewImage();
    }

    private void addSnapshotFiles(List<File> files) {
        files.stream()
                .map(GameUtil::createGameFromFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(g -> romSetHandler.addGame(g));
    }

    private void updateRomSetHandler() {
        if (romSetHandler != null) {
            romSetHandler.unbind();
        }
        LOGGER.debug("Changing RomSetHandler to " + Configuration.getInstance().getMode());
        romSetHandler = RomSetHandlerFactory.getHandler(Configuration.getInstance().getMode());
        romSetHandler.bind(this);
    }

	@FXML
	private void initialize() throws IOException {

	    gameRenderer = GameRendererFactory.getDefaultRenderer();
        gameRenderer.setTarget(getGamePreviewImage());
        updateRomSetHandler();

		gameTable.setItems(getGameList());
		gameTable.setPlaceholder(new Label(LocaleUtil.i18n("dropGamesMessage")));

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

        pokeView.setEditable(true);
        pokeView.setCellFactory(p -> {
            TreeCell<PokeViewable> cell = new PokeEntityTreeCell();
            cell.setOnMouseClicked(e -> {
                if (cell.isEmpty()) {
                    pokeView.getSelectionModel().clearSelection();
                }
            });
            return cell;
        });

        pokeView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        removeSelectedPokeButton.setDisable(false);
                    } else {
                        removeSelectedPokeButton.setDisable(true);
                    }
                });

		getGameList().addListener((ListChangeListener.Change<? extends Game> cl) -> {
			onGameListChange();
		});

        gameTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onGameSelection(oldValue, newValue));
        
        gameTable.setOnDragOver(event -> {
        	if (event.getGestureSource() != gameTable &&
        			event.getDragboard().hasFiles()) {
        		event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        	}
        	event.consume();
        });

        gameTable.setOnDragEntered(event -> {
        	if (event.getGestureSource() != gameTable &&
        			event.getDragboard().hasFiles()) {
        		//TODO: Give feedback
        	}
        	event.consume();
        });
        
        gameTable.setOnDragExited(event -> {
        	//TODO: Remove feedback
        	event.consume();
        });
        
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
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("saveRomSet"));
            final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
            try {
                romSetHandler.exportRomSet(new FileOutputStream(saveFile));
            } catch (IOException e) {
                LOGGER.error("Creating ROM Set", e);
            }
        });

        addRomButton.setOnAction(c -> {
           FileChooser chooser = new FileChooser();
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
            selectedIndex.ifPresent(index -> getGameList().remove(index.intValue()));
        });

        clearRomsetButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.get() == ButtonType.OK){
                getGameList().clear();
            }
        });

        addPokeButton.setOnAction(c -> {
            if (pokeView.getSelectionModel().getSelectedItem() != null) {
                pokeView.getSelectionModel().getSelectedItem().getValue()
                        .addNewChild();
            } else {
                pokeView.getRoot().getValue().addNewChild();
            }
        });

        removeSelectedPokeButton.setOnAction(c -> {
           if (pokeView.getSelectionModel().getSelectedItem() != null) {
               TreeItem<PokeViewable> selected = pokeView.getSelectionModel().getSelectedItem();
               if (selected != null) {
                   int selectedIndex = pokeView.getSelectionModel().getSelectedIndex();
                   if (selectedIndex >= 0) {
                       pokeView.getSelectionModel().select(selectedIndex - 1);
                   } else {
                       pokeView.getSelectionModel().select(pokeView.getRoot());
                   }
                   selected.getValue().getParent().removeChild(selected.getValue());
               }
           }
        });

        removeAllGamePokesButton.setOnAction(c -> {
            Game game = gameTable.getSelectionModel().getSelectedItem();
            if (game != null) {
                Optional<ButtonType> result = DialogUtil
                        .buildAlert(LocaleUtil.i18n("pokeSetDeletionConfirmTitle"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmHeader"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmContent"))
                        .showAndWait();

                if (result.get() == ButtonType.OK) {
                    ((RamGame) game).getTrainerList().getChildren().clear();
                }
            }
        });

        pokeView.setDisable(true);
        pokeView.setOnDragOver(event -> {
            if (event.getGestureSource() != pokeView &&
                    event.getDragboard().hasFiles() &&
                    event.getDragboard().getFiles().size() == 1) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pokeView.setOnDragEntered(event -> {
            if (event.getGestureSource() != pokeView &&
                    event.getDragboard().hasFiles() &&
                    event.getDragboard().getFiles().size() == 1) {
                //TODO: Give feedback
            }
            event.consume();
        });

        pokeView.setOnDragExited(Event::consume);

        pokeView.setOnDragDropped(event -> {
            LOGGER.debug("onDragDropped");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && db.getFiles().size() == 1) {
                try {
                    Game game = gameTable.getSelectionModel().getSelectedItem();
                    ImportContext ctx = new ImportContext(db.getFiles().get(0));
                    GameUtil.importPokesFromFile((RamGame) game, ctx);
                    if (ctx.hasErrors()) {
                        LOGGER.debug("Detected errors in pokes import operation");
                        DialogUtil.buildWarningAlert(LocaleUtil.i18n("importPokesWarning"),
                                LocaleUtil.i18n("importPokesWarningHeader"),
                                ctx.getImportErrors().stream()
                                        .distinct()
                                        .collect(Collectors.joining("\n"))).showAndWait();
                    }
                    success = true;
                } catch (IOException ioe) {
                    LOGGER.warn("Adding poke files", ioe);
                }
            }
            /* let the source know whether the files were successfully
             * transferred and used */
            event.setDropCompleted(success);
            event.consume();
        });

         Configuration.getInstance().modeProperty().addListener(
                (observable, oldValue, newValue) -> updateRomSetHandler());

        //Update poke usage while adding or removing games from the list
        getGameList().addListener((ListChangeListener.Change<? extends Game> c) -> {
            boolean gamesAddedOrRemoved = false;
            boolean gamesUpdated = false;
            while (c.next()) {
                if (c.wasRemoved() || c.wasAdded()) {
                    gamesAddedOrRemoved = true;
                }
                if (c.wasUpdated()) {
                    gamesUpdated = true;
                }
                if (gamesAddedOrRemoved && gamesUpdated) {
                    //Don't search anymore. We know enough
                    break;
                }
            }
            if (gamesAddedOrRemoved) {
                pokesCurrentSizeBar.setProgress(GameUtil.getOverallPokeUsage(getGameList()));
            }
            if (gamesUpdated) {
                Game game = gameTable.getSelectionModel().getSelectedItem();
            }
        });
    }

    private void unbindInfoPropertiesFromGame(Game game) {
        if (game != null) {
            LOGGER.debug("Unbinding bidirectionally name property from game " + game);
            gameName.textProperty().unbindBidirectional(game.nameProperty());
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                gameHoldScreenAttribute.selectedProperty().unbindBidirectional(ramGame.holdScreenProperty());
                gameRomAttribute.selectedProperty().unbindBidirectional(ramGame.romProperty());
                pokeView.setRoot(null);
            }
        }
    }

    private void bindInfoPropertiesToGame(Game game) {
        if (game != null) {
            LOGGER.debug("Binding bidirectionally name property to game " + game);
            gameName.textProperty().bindBidirectional(game.nameProperty());
            gameType.textProperty().set(game.getType().name());
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                gameHoldScreenAttribute.selectedProperty().bindBidirectional(ramGame.holdScreenProperty());
                gameRomAttribute.selectedProperty().bindBidirectional(ramGame.romProperty());
                pokeView.setRoot(new RecursiveTreeItem<>(ramGame.getTrainerList(), PokeViewable::getChildren,
                        this::computePokeChange));
            }
        }
    }

	private void onGameSelection(Game oldGame, Game newGame) {
	    LOGGER.debug("onGameSelection oldGame=" + oldGame+ ", newGame=" + newGame);
	    unbindInfoPropertiesFromGame(oldGame);
	    gameRenderer.previewGame(newGame);
        bindInfoPropertiesToGame(newGame);
		if (newGame == null) {
            removeSelectedRomButton.setDisable(true);
            addPokeButton.setDisable(true);
            removeAllGamePokesButton.setDisable(true);
            removeSelectedPokeButton.setDisable(true);
            pokeView.setDisable(true);
            gameInfoTabPane.setDisable(true);
		} else {
            removeSelectedRomButton.setDisable(false);

            if (newGame instanceof RamGame) {
                RamGame ramGame = (RamGame) newGame;
                addPokeButton.setDisable(false);
                pokeView.setDisable(false);
                pokesTab.setDisable(false);
                if (ramGame.getTrainerList().getChildren().size() > 0) {
                    removeAllGamePokesButton.setDisable(false);
                } else {
                    removeAllGamePokesButton.setDisable(true);
                }
            } else {
                pokeView.setDisable(true);
                pokesTab.setDisable(true);
            }
            gameInfoTab.setDisable(false);
            gameInfoTabPane.setDisable(false);
		}
	}

    private void computePokeChange(PokeViewable f) {
        LOGGER.debug("New poke ocupation is " + GameUtil.getOverallPokeUsage(getGameList()));
        pokesCurrentSizeBar.setProgress(GameUtil.getOverallPokeUsage(getGameList()));
        if (gameTable.getSelectionModel().getSelectedItem() == f.getOwner()) {
            removeAllGamePokesButton.setDisable(!GameUtil.gameHasPokes(f.getOwner()));
        }
    }

    public void importRomSet(File romSetFile) throws IOException {
        if (getGameList().size() > 0) {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();
            if (result.get() == ButtonType.OK){
                getGameList().clear();
            }
        }
        InputStream is = new FileInputStream(romSetFile);
        romSetHandler.importRomSet(is);
    }

    public void exportCurrentGamePokes() {
        Game selectedGame = gameTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null && selectedGame instanceof RamGame) {
            if (GameUtil.gameHasPokes(selectedGame)) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle(LocaleUtil.i18n("exportCurrentGamePokes"));
                final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
                try {
                    GameUtil.exportPokesToFile((RamGame) selectedGame, saveFile);
                } catch (IOException e) {
                    LOGGER.error("Exporting Game Pokes", e);
                }
            } else {
                DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGamePokesErrorTitle"),
                        LocaleUtil.i18n("exportCurrentGamePokesErrorHeader"),
                        LocaleUtil.i18n("exportCurrentGamePokesErrorContentNoPokesInGame")).showAndWait();
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGamePokesErrorTitle"),
                    LocaleUtil.i18n("exportCurrentGamePokesErrorHeader"),
                    LocaleUtil.i18n("exportCurrentGamePokesErrorContentNoGameSelected")).showAndWait();
        }
    }

    public void exportCurrentGame() {
        Game selectedGame = gameTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentGame"));
            final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
            try {
                GameUtil.exportGameAsSNA(selectedGame, saveFile);
            } catch (IOException e) {
                LOGGER.error("Exporting Game", e);
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGameErrorTitle"),
                    LocaleUtil.i18n("exportCurrentGameErrorHeader"),
                    LocaleUtil.i18n("exportCurrentGameErrorContentNoGameSelected")).showAndWait();
        }
    }

}
