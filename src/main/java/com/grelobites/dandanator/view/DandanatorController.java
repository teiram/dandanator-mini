package com.grelobites.dandanator.view;

import java.io.IOException;

import javafx.beans.Observable;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.GameUtil;
import com.grelobites.dandanator.util.ImageUtil;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DandanatorController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorController.class);
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
    
    private int getAvailableSlotCount() {
    	return romSize256.isSelected() ? Constants.SLOTS_256K_ROM : 
    		Constants.SLOTS_512K_ROM;
    }
    
    private void initializeImages() throws IOException {
    	
    	dandanatorPreviewImage = ImageUtil.scrLoader(
    			new ZxScreen(),
				DandanatorController.class.getClassLoader()
				.getResourceAsStream("dandanator.scr"));
    	//Decorate the image once
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
