package com.grelobites.dandanator.view;

import java.io.File;
import java.io.IOException;

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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public class DandanatorController {

	private WritableImage spectrum48kImage;
	private ZxScreen dandanatorPreviewImage;
	
	private ObservableList<Game> gameList = FXCollections.observableArrayList();
	
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
    
    private int getMaxSlotCount() {
    	//TODO: Control with the 256/512 buttons
    	return 10;
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
    
    private void recreatePreviewImage() {
    	int line = 10;
    	int index = 1;
    	int maxSlots = getMaxSlotCount();
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
    	while (index <= getMaxSlotCount()) {
    		dandanatorPreviewImage.deleteLine(line);
    		dandanatorPreviewImage.setPen(ZxColor.WHITE);
    		dandanatorPreviewImage.printLine(String
    				.format("%d.", index % Constants.MAX_SLOTS), line++, 0);
    		index++;
    	}
    	dandanatorPreviewImage.setPen(ZxColor.BRIGHTBLUE);
    	dandanatorPreviewImage.printLine("T. Toggle Pokes", 21, 0);
    	if (maxSlots == Constants.MAX_SLOTS) {
    		dandanatorPreviewImage.setPen(ZxColor.BRIGHTRED);
    		dandanatorPreviewImage.printLine("R. Test ROM", 23, 0);
    	}
    }
    
	@FXML
	private void initialize() throws IOException {
	
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
			recreatePreviewImage();
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
                System.out.println("onDragDropped");
                Dragboard db = event.getDragboard();
                boolean success = false;
                if (db.hasFiles()) {
                	for (File file: db.getFiles()) {
                		GameUtil.createGameFromFile(file).map(game -> {
                			return gameList.add(game);
                		});
                	}
                    success = true;
                }
                /* let the source know whether the string was successfully 
                 * transferred and used */
                event.setDropCompleted(success);
                event.consume();
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
