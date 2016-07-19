package com.grelobites.dandanator.view;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.Context;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.PokeViewable;
import com.grelobites.dandanator.util.*;
import com.grelobites.dandanator.util.emulator.zxspectrum.EmulatorInstance;
import com.grelobites.dandanator.util.gameloader.GameImageLoader;
import com.grelobites.dandanator.util.gameloader.GameImageLoaderFactory;
import com.grelobites.dandanator.util.pokeimporter.ImportContext;
import com.grelobites.dandanator.util.pokeimporter.PokeImporter;
import com.grelobites.dandanator.util.pokeimporter.PokeImporterFactory;
import com.grelobites.dandanator.view.util.DialogUtil;
import com.grelobites.dandanator.view.util.PokeEntityTreeCell;
import com.grelobites.dandanator.view.util.RecursiveTreeItem;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.Border;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DandanatorController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

	private EmulatorInstance spectrum48kInstance;
	private ZxScreen dandanatorPreviewImage;
	
	private Context context;
	
	@FXML
	private ImageView previewImage;
	
	@FXML
	private Pane currentScreenshot;
	
	@FXML
	private TableView<Game> gameTable;
	
	@FXML
	private TableColumn<Game, String> nameColumn;
	
    @FXML
    private TableColumn<Game, Boolean> screenColumn;
    
    @FXML
    private TableColumn<Game, Boolean> romColumn;
   
    @FXML
    private Button createRomButton;

    @FXML
    private Button addRomButton;

    @FXML
    private Button removeSelectedRomButton;

    @FXML
    private Button clearRomsetButton;

    @FXML
    private Label pokesViewLabel;

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

    private boolean emulatorStarted = false;

    private int getAvailableSlotCount() {
    	return Constants.SLOT_COUNT;
    }
    
    private void initializeImages() throws IOException {
        dandanatorPreviewImage = ImageUtil.scrLoader(
                new ZxScreen(),
                new ByteArrayInputStream(context.getConfiguration()
                    .getBackgroundImage()));

    	//Decorate the preview for the first time
    	recreatePreviewImage();

        try {
            spectrum48kInstance = new EmulatorInstance();
            //spectrum48kInstance.start(currentScreenshot);
        } catch (Exception e) {
            throw new RuntimeException("Initializing emulator", e);
        }
    }
    
    private boolean isEmptySlotAvailable() {
    	return context.getGameList().size() < getAvailableSlotCount();
    }
    
    private void onGameListChange() {
        LOGGER.debug("onGameListChange");
    	createRomButton.setDisable(context.getGameList().size() != getAvailableSlotCount());
        addRomButton.setDisable(context.getGameList().size() == getAvailableSlotCount());
        clearRomsetButton.setDisable(context.getGameList().isEmpty());

    	recreatePreviewImage();
    }
    
    private void recreatePreviewImage() {
        LOGGER.debug("recreatePreviewImage");
        context.getRomSetHandler()
                .updateScreen(context, dandanatorPreviewImage);
    }

    private void updatePreviewImage() {
        LOGGER.debug("updatePreviewImage");
        try {
            ImageUtil.scrLoader(dandanatorPreviewImage,
                    new ByteArrayInputStream(context.getConfiguration()
                            .getBackgroundImage()));
            recreatePreviewImage();
        } catch (Exception e) {
            LOGGER.error("Updating preview image", e);
        }
    }

    private void addSnapshotFiles(List<File> files) {
        files.stream()
                .filter(f -> isEmptySlotAvailable())
                .map(GameUtil::createGameFromFile)
                .forEach(gameOptional -> gameOptional.map(g -> context.getGameList().add(g)));
    }

    private void setupContext() {
        context = new Context();
        context.setConfiguration(Configuration.getInstance());
        context.setGameList(FXCollections.observableArrayList(game ->
                new Observable[] {game.nameProperty(),
                        game.romProperty(),
                        game.screenProperty()}));
    }
	@FXML
	private void initialize() throws IOException {

        setupContext();

		gameTable.setItems(context.getGameList());
		gameTable.setPlaceholder(new Label(LocaleUtil.i18n("dropGamesMessage")));

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
	                LOGGER.debug("onDragOver: " + db);
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

        screenColumn.setCellValueFactory(
                        cellData -> cellData.getValue().screenProperty());
        screenColumn.setCellFactory(CheckBoxTableCell
        		.forTableColumn(screenColumn));
        
        romColumn.setCellValueFactory(
        		cellData -> cellData.getValue().romProperty());
        romColumn.setCellFactory(CheckBoxTableCell
        		.forTableColumn(romColumn));
        
        initializeImages();

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

		previewImage.setImage(dandanatorPreviewImage);

		context.getGameList().addListener((ListChangeListener.Change<? extends Game> cl) -> {
			onGameListChange();
		});

        gameTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> onGameSelection(newValue));
        
        gameTable.setOnDragOver(event -> {
        	if (event.getGestureSource() != gameTable &&
        			event.getDragboard().hasFiles()) {
        		event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        	}
        	event.consume();
        });

        gameTable.setOnDragEntered(event -> {
        	if (event.getGestureSource() != gameTable &&
        			event.getDragboard().hasFiles() &&
        			isEmptySlotAvailable()) {
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
                if (db.hasFiles() && isEmptySlotAvailable()) {
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
                context.getRomSetHandler()
                        .createRomSet(context, new FileOutputStream(saveFile));
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
            selectedIndex.ifPresent(index -> context.getGameList().remove(index.intValue()));
        });

        clearRomsetButton.setOnAction(c -> {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();

            if (result.get() == ButtonType.OK){
                context.getGameList().clear();
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


        currentScreenshot.setOnMouseClicked(e -> {
            if (!emulatorStarted) {
                emulatorStarted = true;
                spectrum48kInstance.start(currentScreenshot);
                currentScreenshot.getParent().setStyle("-fx-background-color: green; -fx-background-radius: 5;");
            }
            if (spectrum48kInstance.isRunning()) {
                spectrum48kInstance.pause();
                currentScreenshot.getParent().setStyle("-fx-background-color: #a0a0a0; -fx-background-radius: 5;");
            } else {
                spectrum48kInstance.resume();
                currentScreenshot.getParent().setStyle("-fx-background-color: green; -fx-background-radius: 5;");
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

                if (result.get() == ButtonType.OK){
                    game.getTrainerList().getChildren().clear();
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
                    GameUtil.importPokesFromFile(game, ctx);
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

        context.getConfiguration().backgroundImagePathProperty().addListener(
                (observable, oldValue, newValue) -> updatePreviewImage());
        context.getConfiguration().charSetPathProperty().addListener(
                (observable, oldValue, newValue) -> updatePreviewImage());
        context.getConfiguration().togglePokesMessageProperty().addListener(
                (observable, oldValue, newValue) -> updatePreviewImage());
        context.getConfiguration().extraRomMessageProperty().addListener(
                (observable, oldValue, newValue) -> updatePreviewImage());

        //Update poke usage while adding or removing games from the list
        context.getGameList().addListener((ListChangeListener.Change<? extends Game> c) -> {
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
                pokesCurrentSizeBar.setProgress(GameUtil.getOverallPokeUsage(context.getGameList()));
            }
            if (gamesUpdated) {
                Game game = gameTable.getSelectionModel().getSelectedItem();

                pokesViewLabel.setText(game != null ? String.format(LocaleUtil.i18n("trainersHeadingMessage"),
                        gameTable.getSelectionModel().getSelectedItem().getName()) :
                        LocaleUtil.i18n("noGameSelectedMessage"));
            }
        });
    }

	private void onGameSelection(Game game) {
		if (game == null) {
		    spectrum48kInstance.reset();
            removeSelectedRomButton.setDisable(true);
            addPokeButton.setDisable(true);
            removeAllGamePokesButton.setDisable(true);
            removeSelectedPokeButton.setDisable(true);
            pokesViewLabel.setText(LocaleUtil.i18n("noGameSelectedMessage"));
            pokeView.setDisable(true);
            pokeView.setRoot(null);

		} else {
		    spectrum48kInstance.loadGame(game);
            removeSelectedRomButton.setDisable(false);
            addPokeButton.setDisable(false);
            pokesViewLabel.setText(String.format(LocaleUtil.i18n("trainersHeadingMessage"), game.getName()));
            pokeView.setRoot(new RecursiveTreeItem<>(game.getTrainerList(), PokeViewable::getChildren,
                    this::computePokeChange));
            pokeView.setDisable(false);
            if (game.getTrainerList().getChildren().size() > 0) {
                removeAllGamePokesButton.setDisable(false);
            } else {
                removeAllGamePokesButton.setDisable(true);
            }
		}
	}

    private void computePokeChange(PokeViewable f) {
        LOGGER.debug("New poke ocupation is " + GameUtil.getOverallPokeUsage(context.getGameList()));
        pokesCurrentSizeBar.setProgress(GameUtil.getOverallPokeUsage(context.getGameList()));
        if (gameTable.getSelectionModel().getSelectedItem() == f.getOwner()) {
            removeAllGamePokesButton.setDisable(!f.getOwner().hasPokes());
        }
    }

    public void importRomSet(File romSetFile) throws IOException {
        if (context.getGameList().size() > 0) {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();
            if (result.get() == ButtonType.OK){
                context.getGameList().clear();
            }
        }
        InputStream is = new FileInputStream(romSetFile);
        context.getRomSetHandler()
                .importRomSet(context, is);
    }

    public void exportCurrentGamePokes() {
        Game selectedGame = gameTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null) {
            if (selectedGame.hasPokes()) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle(LocaleUtil.i18n("exportCurrentGamePokes"));
                final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
                try {
                    GameUtil.exportPokesToFile(selectedGame, saveFile);
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
