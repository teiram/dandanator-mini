package com.grelobites.dandanator.view;

import java.io.IOException;

import javafx.beans.Observable;
import javafx.scene.control.*;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.Poke;
import com.grelobites.dandanator.util.GameUtil;
import com.grelobites.dandanator.util.ImageUtil;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

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
    private ToggleButton romSize256;
    
    @FXML
    private ToggleButton romSize512;
    
    @FXML
    private ToggleGroup romsize;
    
    @FXML
    private Button removeButton;
   
    @FXML
    private TreeView<Poke> pokeView;
    
    private int getAvailableSlotCount() {
    	return romSize256.isSelected() ? Constants.SLOTS_256K_ROM : 
    		Constants.SLOTS_512K_ROM;
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
    	recreatePreviewImage();
    }
    
    private void recreatePreviewImage() {
        LOGGER.info("recreatePreviewImage");
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
	                    LOGGER.info("Dragging content of row " + index);
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
	                LOGGER.info("onDragOver: " + db);
	                if (db.hasContent(SERIALIZED_MIME_TYPE)) {
	                    if (row.getIndex() != ((Integer)db.getContent(SERIALIZED_MIME_TYPE)).intValue()) {
	                        event.acceptTransferModes(TransferMode.MOVE);
	                        event.consume();
	                    }
	                }
	            });

	            row.setOnDragDropped(event -> {
	                Dragboard db = event.getDragboard();
	            	LOGGER.info("row.setOnDragDropped: " + db);
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
	                	LOGGER.info("Dragboard content is not of the required type");
	                }
	            });
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
        
		previewImage.setImage(dandanatorPreviewImage);
	
		currentScreenshot.setImage(spectrum48kImage);
	
		gameList.addListener((ListChangeListener.Change<? extends Game> cl) -> {
			onGameListChange();
		});

        gameTable.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showGameDetails(newValue));
        
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
                    db.getFiles().stream()
                            .map(GameUtil::createGameFromFile)
                            .forEach(gameOptional -> {
                                gameOptional.map(gameList::add);
                            });
                    success = true;
                }
                /* let the source know whether the files were successfully
                 * transferred and used */
                event.setDropCompleted(success);
                event.consume();
            });    
        
        romsize.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
        	int maxAllowedSlots = getAvailableSlotCount();
        	while (gameList.size() > maxAllowedSlots) {
        		gameList.remove(gameList.size() - 1);
        	}
        	onGameListChange();
        });
        
        removeButton.setOnAction(c -> {
        	for (Integer index : gameTable.getSelectionModel().getSelectedIndices()) {
        		gameList.remove(index.intValue());
        	}
        });
	}
	
	private void showGameDetails(Game game) {
		if (game == null) {
			currentScreenshot.setImage(spectrum48kImage);
		} else {
			currentScreenshot.setImage(game.getScreenshot());
		}
	}

}
