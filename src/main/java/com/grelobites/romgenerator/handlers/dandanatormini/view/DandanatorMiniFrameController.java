package com.grelobites.romgenerator.handlers.dandanatormini.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.PokeEntityTreeCell;
import com.grelobites.romgenerator.view.util.RecursiveTreeItem;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

public class DandanatorMiniFrameController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniFrameController.class);

    private static final String BLUE_BAR_STYLE = "blue-bar";
    private static final String RED_BAR_STYLE = "red-bar";

    private static final String HW_MODE_SUPPORTED = "white-text";
    private static final String HW_MODE_UNSUPPORTED = "red-text";

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


    private Tooltip pokeUsageDetail;

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
    private Label romActiveAttributeLabel;

    @FXML
    private ComboBox<Game> gameRomAttribute;

    @FXML
    private CheckBox gameHoldScreenAttribute;

    @FXML
    private CheckBox gameCompressedAttribute;

    @FXML
    private CheckBox gameForced48kModeAttribute;

    @FXML
    private Label compressedSize;

    @FXML
    private ProgressBar romUsage;

    @FXML
    private Label hardwareMode;

    @FXML
    private HBox danSnapSizeSelector;

    @FXML
    private RadioButton danSnap48KSize;

    @FXML
    private RadioButton danSnap128KSize;

    private ApplicationContext applicationContext;

    private InvalidationListener currentGameCompressedChangeListener;

    private InvalidationListener getCurrentGameCompressedChangeListener() {
        return currentGameCompressedChangeListener;
    }

    private void setCurrentGameCompressedChangeListener(InvalidationListener currentGameCompressedChangeListener) {
        this.currentGameCompressedChangeListener = currentGameCompressedChangeListener;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @FXML
    private void initialize() {
        LOGGER.debug("Initializing DandanatorMiniFrameController");
        romUsage.progressProperty().bind(applicationContext.romUsageProperty());
        Tooltip romUsageDetail = new Tooltip();
        romUsage.setTooltip(romUsageDetail);
        romUsageDetail.textProperty().bind(applicationContext.romUsageDetailProperty());
        romUsage.progressProperty().addListener(
                (observable, oldValue, newValue) -> {
                    LOGGER.debug("Changing bar style on romUsage change to " + newValue.doubleValue());
                    romUsage.getStyleClass().removeAll(BLUE_BAR_STYLE, RED_BAR_STYLE);
                    romUsage.getStyleClass().add(
                            (newValue.doubleValue() > 1.0 ||
                                    applicationContext.getGameList().size() > DandanatorMiniConstants.MAX_GAMES) ?
                                    RED_BAR_STYLE : BLUE_BAR_STYLE);

                });

        gameInfoTabPane.setVisible(false);

        pokeView.setDisable(false);
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
            Game game = applicationContext.selectedGameProperty().get();
            if (game != null) {
                Optional<ButtonType> result = DialogUtil
                        .buildAlert(LocaleUtil.i18n("pokeSetDeletionConfirmTitle"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmHeader"),
                                LocaleUtil.i18n("pokeSetDeletionConfirmContent"))
                        .showAndWait();

                if (result.orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    ((SnapshotGame) game).getTrainerList().getChildren().clear();
                }
            }
        });

        pokeView.setOnDragOver(event -> {
            if (event.getGestureSource() != pokeView &&
                    event.getDragboard().hasFiles() &&
                    event.getDragboard().getFiles().size() == 1) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        pokeView.setOnDragEntered(Event::consume);

        pokeView.setOnDragExited(Event::consume);

        pokeView.setOnDragDropped(event -> {
            LOGGER.debug("onDragDropped");
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles() && db.getFiles().size() == 1) {
                try {
                    Game game = applicationContext.selectedGameProperty().get();
                    ImportContext ctx = new ImportContext(db.getFiles().get(0));
                    GameUtil.importPokesFromFile((SnapshotGame) game, ctx);
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

        pokeUsageDetail = new Tooltip();
        pokesCurrentSizeBar.setTooltip(pokeUsageDetail);
        applicationContext.getGameList().addListener((InvalidationListener) c -> {
            double pokeUsage = GameUtil.getOverallPokeUsage(applicationContext.getGameList());
            pokesCurrentSizeBar.setProgress(pokeUsage);
            String pokeUsageDetailString = String.format(LocaleUtil.i18n("pokeUsageDetail"),
                    pokeUsage * 100,
                    DandanatorMiniConstants.POKE_ZONE_SIZE);
            pokeUsageDetail.setText(pokeUsageDetailString);
        });

        applicationContext.selectedGameProperty().addListener(
                (observable, oldValue, newValue) -> onGameSelection(oldValue, newValue));
        onGameSelection(applicationContext.selectedGameProperty().get(),
                applicationContext.selectedGameProperty().get());


        gameRomAttribute.setCellFactory(new Callback<ListView<Game>, ListCell<Game>>() {
            @Override
            public ListCell<Game> call(ListView<Game> arg0) {
                return new ListCell<Game>() {
                    @Override
                    protected void updateItem(Game item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item == null || empty) {
                            setGraphic(null);
                        } else {
                            setText(item.getName());
                        }
                    }

                    @Override
                    protected boolean isItemChanged(Game oldItem, Game newItem) {
                        return true; //Force rerendering of items (handle name changes)
                    }
                };
            }
        });

        gameRomAttribute.setButtonCell(new TextFieldListCell<>(new StringConverter<Game>() {
            @Override
            public String toString(Game object) {
                return object.getName();
            }

            @Override
            public Game fromString(String string) {
                return null;
            }
        }));

        updateActiveRomComboItems();

        applicationContext.getGameList().addListener((InvalidationListener) e ->
                updateActiveRomComboItems());

        /*
        applicationContext.getGameList().addListener((ListChangeListener<Game>) c -> {
            while (c.next()) {
                gameRomAttribute.getItems().removeAll(c.getRemoved());
                c.getAddedSubList().forEach(g -> {
                    if (g instanceof RomGame) {
                        gameRomAttribute.getItems().add(g);
                    }
                });
            }
            LOGGER.debug("After computing changes. ComboBox list is " +
                gameRomAttribute.getItems());
        });
        */

    }

    private void updateActiveRomComboItems() {
        LOGGER.debug("updateActiveRomComboItems");

        ObservableList<Game> items = FXCollections.observableArrayList(
                DandanatorMiniConstants.INTERNAL_ROM_GAME,
                DandanatorMiniConstants.EXTRA_ROM_GAME);
        items.addAll(applicationContext.getGameList().filtered(e -> e.getType() == GameType.ROM));
        LOGGER.debug("Items in ROM combo list " + items);
        gameRomAttribute.setItems(items);
    }

    private void unbindInfoPropertiesFromGame(Game game) {
        if (game != null) {
            LOGGER.debug("Unbinding bidirectionally name property from game " + game);
            gameName.textProperty().unbindBidirectional(game.nameProperty());
            compressedSize.textProperty().unbind();

            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                gameHoldScreenAttribute.selectedProperty().unbindBidirectional(snapshotGame.holdScreenProperty());
                gameRomAttribute.valueProperty().unbindBidirectional(snapshotGame.romProperty());
                gameCompressedAttribute.selectedProperty().unbindBidirectional(snapshotGame.compressedProperty());
                gameForced48kModeAttribute.selectedProperty().unbindBidirectional(snapshotGame.force48kModeProperty());
                pokeView.setRoot(null);
                gameCompressedAttribute.selectedProperty().removeListener(getCurrentGameCompressedChangeListener());
            }
            if (game instanceof DanSnapGame) {
                DanSnapGame danGame = (DanSnapGame) game;
                danSnap48KSize.selectedProperty().unbindBidirectional(danGame.reservationSize48KProperty());
            }
        }
    }

    private void bindInfoPropertiesToGame(Game game) {
        if (game != null) {
            gameName.textProperty().bindBidirectional(game.nameProperty());
            gameType.textProperty().set(game.getType().screenName());
            compressedSize.textProperty().bind(getGameSizeProperty(game).asString());
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                LOGGER.debug("Binding hardware mode to " + ramGame.getHardwareMode().displayName());
                hardwareMode.textProperty().set(ramGame.getHardwareMode().displayName());
            }
            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                gameHoldScreenAttribute.selectedProperty().bindBidirectional(snapshotGame.holdScreenProperty());
                gameRomAttribute.valueProperty().bindBidirectional(snapshotGame.romProperty());
                LOGGER.debug("gameRomAttribute list is " + gameRomAttribute.getItems());
                //updateActiveRomComboItems();

                pokeView.setRoot(new RecursiveTreeItem<>(snapshotGame.getTrainerList(), PokeViewable::getChildren,
                        this::computePokeChange));
                gameCompressedAttribute.selectedProperty().bindBidirectional(snapshotGame.compressedProperty());
                gameForced48kModeAttribute.selectedProperty().bindBidirectional((snapshotGame.force48kModeProperty()));
                setCurrentGameCompressedChangeListener((c) -> {
                    compressedSize.textProperty().unbind();
                    compressedSize.textProperty().bind(getGameSizeProperty(game).asString());
                });
                snapshotGame.compressedProperty().addListener(getCurrentGameCompressedChangeListener());
            }
            if (game instanceof DanSnapGame) {
                LOGGER.debug("Binding as DanSnapGame");
                DanSnapGame danGame = (DanSnapGame) game;
                danSnap48KSize.selectedProperty().bindBidirectional(danGame.reservationSize48KProperty());
            }
        }
    }

    private void computePokeChange(PokeViewable f) {
        LOGGER.debug("New poke ocupation is " + GameUtil.getOverallPokeUsage(applicationContext.getGameList()));
        double pokeUsage = GameUtil.getOverallPokeUsage(applicationContext.getGameList());
        pokesCurrentSizeBar.setProgress(pokeUsage);
        String pokeUsageDetailString = String.format(LocaleUtil.i18n("pokeUsageDetail"),
                pokeUsage * 100,
                DandanatorMiniConstants.POKE_ZONE_SIZE);
        pokeUsageDetail.setText(pokeUsageDetailString);
        if (applicationContext.selectedGameProperty().get() == f.getOwner()) {
            removeAllGamePokesButton.setDisable(!GameUtil.gameHasPokes(f.getOwner()));
        }
    }

    private void styleHardwareMode(RamGame game) {
        hardwareMode.getStyleClass().removeAll(
                game.getHardwareMode().supported() ? HW_MODE_UNSUPPORTED : HW_MODE_SUPPORTED);
        hardwareMode.getStyleClass().add(game.getHardwareMode().supported() ?
            HW_MODE_SUPPORTED : HW_MODE_UNSUPPORTED);
    }

    private void onGameSelection(Game oldGame, Game newGame) {
        LOGGER.debug("onGameSelection oldGame=" + oldGame + ", newGame=" + newGame);
        unbindInfoPropertiesFromGame(oldGame);
        bindInfoPropertiesToGame(newGame);
        if (newGame == null) {
            addPokeButton.setDisable(true);
            removeAllGamePokesButton.setDisable(true);
            removeSelectedPokeButton.setDisable(true);
            gameInfoTabPane.setVisible(false);
        } else {
            if (newGame instanceof RamGame) {
                RamGame ramGame = (RamGame) newGame;
                hardwareMode.setVisible(true);
                styleHardwareMode(ramGame);

                if (newGame instanceof SnapshotGame) {
                    SnapshotGame snapshotGame = (SnapshotGame) newGame;
                    addPokeButton.setDisable(false);
                    pokesTab.setDisable(false);
                    gameRomAttribute.setVisible(true);
                    gameHoldScreenAttribute.setVisible(true);
                    gameCompressedAttribute.setVisible(true);
                    romActiveAttributeLabel.setVisible(true);
                    if (snapshotGame.getTrainerList().getChildren().size() > 0) {
                        removeAllGamePokesButton.setDisable(false);
                    } else {
                        removeAllGamePokesButton.setDisable(true);
                    }
                    gameForced48kModeAttribute.setVisible(snapshotGame.getType() == GameType.RAM48);
                } else {
                    pokesTab.setDisable(true);
                    gameRomAttribute.setVisible(false);
                    gameHoldScreenAttribute.setVisible(false);
                    gameCompressedAttribute.setVisible(false);
                    gameForced48kModeAttribute.setVisible(false);
                    romActiveAttributeLabel.setVisible(false);
                }
                danSnapSizeSelector.setVisible(newGame instanceof DanSnapGame);
            } else {
                pokesTab.setDisable(true);
                gameRomAttribute.setVisible(false);
                gameHoldScreenAttribute.setVisible(false);
                gameCompressedAttribute.setVisible(false);
                gameForced48kModeAttribute.setVisible(false);
                hardwareMode.setVisible(false);
                romActiveAttributeLabel.setVisible(false);
            }
            gameInfoTabPane.setVisible(true);
        }
    }


    private IntegerProperty getGameSizeProperty(Game game) {
        try {
            if (game instanceof SnapshotGame) {
                SnapshotGame snapshotGame = (SnapshotGame) game;
                if (snapshotGame.getCompressed()) {
                    return snapshotGame.compressedSizeProperty();
                }
            } else if (game instanceof DanSnapGame) {
                DanSnapGame danGame = (DanSnapGame) game;
                return danGame.sizeProperty();
            }
            return new SimpleIntegerProperty(game.getSize());
        } catch (Exception e) {
            LOGGER.error("Calculating game compressed size", e);
        }
        return null;
    }

}
