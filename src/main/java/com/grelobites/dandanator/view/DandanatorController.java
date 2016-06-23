package com.grelobites.dandanator.view;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.PokeEntity;
import com.grelobites.dandanator.util.GameUtil;
import com.grelobites.dandanator.util.ImageUtil;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;
import com.grelobites.dandanator.view.util.PokeEntityTreeCell;
import com.grelobites.dandanator.view.util.RecursiveTreeItem;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.FileChooser;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class DandanatorController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorController.class);
    private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

	private WritableImage spectrum48kImage;
	private ZxScreen dandanatorPreviewImage;
	
	private ObservableList<Game> gameList;
	
	@FXML
	private ImageView previewImage;
	
	@FXML
	private ImageView currentScreenshot;
	
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
    private Button exportGameButton;

    @FXML
    private Label pokesViewLabel;

    @FXML
    private TreeView<PokeEntity> pokeView;

    @FXML
    private Button addPokeButton;

    @FXML
    private Button removeSelectedPokeButton;

    @FXML
    private Button removeAllGamePokesButton;

    @FXML
    private ProgressBar pokesCurrentSizeBar;


    private int getAvailableSlotCount() {
    	return Constants.SLOTS_512K_ROM;
    }
    
    private void initializeImages() throws IOException {
    	
    	dandanatorPreviewImage = ImageUtil.scrLoader(
    			new ZxScreen(),
				DandanatorController.class.getClassLoader()
				.getResourceAsStream("dandanator.scr"));
    	//Decorate the preview for the first time
    	recreatePreviewImage();
    	
    	spectrum48kImage = ImageUtil.scrLoader(
    			ImageUtil.newScreenshot(),
    			DandanatorController.class.getClassLoader()
				.getResourceAsStream("sinclair-1982.scr"));	
    }
    
    private boolean isEmptySlotAvailable() {
    	return gameList.size() < getAvailableSlotCount();
    }
    
    private void onGameListChange() {
    	createRomButton.setDisable(
    			gameList.size() == getAvailableSlotCount() ? 
    					false : true);
        addRomButton.setDisable(
                gameList.size() == getAvailableSlotCount() ? true: false);
        clearRomsetButton.setDisable(
                gameList.size() == 0 ? true : false);

    	recreatePreviewImage();
    }
    
    private void recreatePreviewImage() {
        LOGGER.debug("recreatePreviewImage");
    	int line = 10;
    	int index = 1;
    	int maxSlots = getAvailableSlotCount();
    	dandanatorPreviewImage.setInk(ZxColor.BLACK);
    	for (Game game : gameList) {
    		dandanatorPreviewImage.setPen(
    				game.getScreen() ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
    		dandanatorPreviewImage.deleteLine(line);
    		dandanatorPreviewImage.printLine(
    				String.format("%d%c %s", index % Constants.MAX_SLOTS, 
    						game.getRom() ? 'r' : '.',
    						game.getName()), 
    				line++, 0);
    		index++;
    	}
    	while (index <= maxSlots) {
    		dandanatorPreviewImage.deleteLine(line);
    		dandanatorPreviewImage.setPen(ZxColor.WHITE);
    		dandanatorPreviewImage.printLine(String
    				.format("%d.", index % Constants.MAX_SLOTS), line++, 0);
    		index++;
    	}
    	while (index++ <= Constants.MAX_SLOTS) {
    		dandanatorPreviewImage.deleteLine(line++);
    	}
    	dandanatorPreviewImage.setPen(ZxColor.BRIGHTBLUE);
    	dandanatorPreviewImage.printLine("T. Toggle Pokes", 21, 0);
    	if (maxSlots == Constants.MAX_SLOTS) {
    		dandanatorPreviewImage.setPen(ZxColor.BRIGHTRED);
    		dandanatorPreviewImage.printLine("R. Test ROM", 23, 0);
    	} else {
    		dandanatorPreviewImage.deleteLine(23);
    	}
    }


    private void addSnapshotFiles(List<File> files) {
        files.stream()
                .filter(f -> isEmptySlotAvailable())
                .map(GameUtil::createGameFromFile)
                .forEach(gameOptional -> gameOptional.map(g -> {
                    g.getPokes().getChildren().addListener((ListChangeListener.Change<?> c) -> {
                        LOGGER.debug("Pokes on game " + g + " changed");
                    });
                    return gameList.add(g);
                }));
    }

	@FXML
	private void initialize() throws IOException {

        gameList = FXCollections.observableArrayList(new Callback<Game, Observable[]>() {
            @Override
            public Observable[] call(Game game) {
                return new Observable[] {game.romProperty(), game.screenProperty()};
            }
        });

		gameTable.setItems(gameList);
		gameTable.setPlaceholder(new Label("Drop games here!"));
		gameTable.setRowFactory(rf -> {
			TableRow<Game> row = new TableRow<Game>();
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
	                    if (row.getIndex() != ((Integer)db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
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
			row.setContextMenu(MenuItemFactory.getGameContextMenu(gameTable, gameList));
			return row;
		});
		
        nameColumn.setCellValueFactory(
                cellData -> cellData.getValue().nameProperty());
        
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
            TreeCell<PokeEntity> cell = new PokeEntityTreeCell();
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

        pokeView.setContextMenu(MenuItemFactory.getPokeContextMenu(gameTable));

		previewImage.setImage(dandanatorPreviewImage);
	
		currentScreenshot.setImage(spectrum48kImage);

		gameList.addListener((ListChangeListener.Change<? extends Game> cl) -> {
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
                System.out.println("onDragDropped");
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
            chooser.setTitle("Save ROM Set");
            final File saveFile = chooser.showSaveDialog(createRomButton.getScene().getWindow());
            try {
                GameUtil.createRomSet(saveFile, gameList);
            } catch (IOException e) {
                LOGGER.error("Creating ROM Set", e);
            }
        });

        addRomButton.setOnAction(c -> {
           FileChooser chooser = new FileChooser();
            chooser.setTitle("Open snapshots");
            final List<File> snapshotFiles = chooser.showOpenMultipleDialog(addRomButton.getScene().getWindow());
            try {
                addSnapshotFiles(snapshotFiles);
            } catch (Exception e) {
                LOGGER.error("Opening snapshots from files " +  snapshotFiles, e);
            }
        });

        removeSelectedRomButton.setOnAction(c -> {
            Optional<Integer> selectedIndex = Optional.of(gameTable.getSelectionModel().getSelectedIndex());
            selectedIndex.ifPresent(index -> gameList.remove(index.intValue()));
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
               TreeItem<PokeEntity> selected = pokeView.getSelectionModel().getSelectedItem();
               if (selected != null) {
                   pokeView.getSelectionModel().select(pokeView.getRoot());
                   selected.getValue().getParent().removeChild(selected.getValue());
               }
           }
        });

        removeAllGamePokesButton.setOnAction(c -> {
            Game game = gameTable.getSelectionModel().getSelectedItem();
            if (game != null) {
                game.getPokes().getChildren().clear();
            }
        });
	}
	
	private void onGameSelection(Game game) {
		if (game == null) {
			currentScreenshot.setImage(spectrum48kImage);
            removeSelectedRomButton.setDisable(true);
            addPokeButton.setDisable(true);
            removeAllGamePokesButton.setDisable(true);
            removeSelectedPokeButton.setDisable(true);
            pokesViewLabel.setText("No game selected");
            pokeView.setDisable(true);
            pokeView.setRoot(null);

		} else {
			currentScreenshot.setImage(game.getScreenshot());
            removeSelectedRomButton.setDisable(false);
            addPokeButton.setDisable(false);
            pokesViewLabel.setText(String.format("Trainers / Pokes for %s", game.getName()));
            pokeView.setRoot(new RecursiveTreeItem<>(game.getPokes(), PokeEntity::getChildren,
                    this::computePokeChange));
            pokeView.setDisable(false);
            if (game.getPokes().getChildren().size() > 0) {
                removeAllGamePokesButton.setDisable(false);
            } else {
                removeAllGamePokesButton.setDisable(true);
            }
		}
	}

    private void computePokeChange(PokeEntity f) {
        LOGGER.debug("New poke ocupation is " + GameUtil.getOverallPokeUsage(gameList));
        pokesCurrentSizeBar.setProgress(GameUtil.getOverallPokeUsage(gameList));
        if (gameTable.getSelectionModel().getSelectedItem() == f.getOwnerGame()) {
            removeAllGamePokesButton.setDisable(f.getOwnerGame().hasPokes() ? false : true);
        }
    }

}
