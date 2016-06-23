package com.grelobites.dandanator.view;

import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.util.GameUtil;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * Created by mteira on 17/6/16.
 */
public class MenuItemFactory {
    private MenuItemFactory() {}

    public static ContextMenu getGameContextMenu(final TableView<Game> gameTable,
                                                 final ObservableList<Game> gameList) {
        return new ContextMenu(deleteItemMenu(gameTable, gameList),
                deleteAllGames(gameList));
    }

    public static MenuItem deleteItemMenu(final TableView<Game> gameTable, final ObservableList<Game> gameList) {
        MenuItem menuItem = new MenuItem("Delete Game");
        menuItem.setOnAction(event ->
            gameList.remove(gameTable.getSelectionModel().getSelectedIndex()));
        return menuItem;
    }

    public static MenuItem deleteAllGames(final ObservableList<Game> gameList) {
        Menu menu = new Menu("Delete all games");
        MenuItem menuItem = new MenuItem("Confirm deletion");
        menu.getItems().add(menuItem);
        menuItem.setOnAction(event -> gameList.clear());
        return menu;
    }

    public static ContextMenu getPokeContextMenu(final TableView<Game> gameTable) {

        return new ContextMenu(importPokes(gameTable));
    }

    public static MenuItem importPokes(final TableView<Game> gameTable) {
        MenuItem menuItem = new MenuItem("Import Pokes from file");
        menuItem.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import Poke file");
            final File pokeFile = chooser.showOpenDialog(gameTable.getScene().getWindow());
            try {
                Game game = gameTable.getSelectionModel().getSelectedItem();
                GameUtil.importPokesFromFile(game, pokeFile);
            } catch (Exception e) {
                //TODO: Give error feedback (alert)
            }
        });
        return menuItem;
    }

}
