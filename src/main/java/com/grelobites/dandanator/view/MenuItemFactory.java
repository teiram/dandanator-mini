package com.grelobites.dandanator.view;

import com.grelobites.dandanator.model.Game;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableView;

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
}
