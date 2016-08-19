package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);

    private final ObservableList<Game> gameList;
    private TableView<Game> gameTable;
    private final BooleanProperty gameSelected;
    private final ImageView menuPreviewImage;
    private StringProperty romUsageDetail;
    private DoubleProperty romUsage;
    private IntegerProperty backgroundTaskCount;
    private RomSetHandler romSetHandler;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("RomGenerator executor service");
        return t ;
    });

    public ApplicationContext(ImageView menuPreviewImage, TableView<Game> gameTable) {
        this.gameList = FXCollections.observableArrayList(Game::getObservable);
        this.gameTable = gameTable;
        this.gameSelected = new SimpleBooleanProperty(false);
        this.menuPreviewImage = menuPreviewImage;
        this.romUsage = new SimpleDoubleProperty();
        this.romUsageDetail = new SimpleStringProperty();
        this.backgroundTaskCount = new SimpleIntegerProperty();
    }

    public ObservableList<Game> getGameList() {
        return gameList;
    }

    public boolean getGameSelected() {
        return gameSelected.get();
    }

    public void setGameSelected(boolean gameSelected) {
        this.gameSelected.set(gameSelected);
    }

    public BooleanProperty gameSelectedProperty() {
        return gameSelected;
    }

    public void setRomUsage(double romUsage) {
        this.romUsage.set(romUsage);
    }

    public double getRomUsage() {
        return romUsage.get();
    }

    public DoubleProperty romUsageProperty() {
        return romUsage;
    }

    public void setRomUsageDetail(String romUsageDetail) {
        this.romUsageDetail.set(romUsageDetail);
    }

    public String getRomUsageDetail() {
        return romUsageDetail.get();
    }

    public StringProperty romUsageDetailProperty() {
        return romUsageDetail;
    }

    public IntegerProperty backgroundTaskCountProperty() {
        return backgroundTaskCount;
    }

    public ImageView getMenuPreviewImage() {
        return menuPreviewImage;
    }

    public void addBackgroundTask(Callable<OperationResult> task) {
        backgroundTaskCount.set(backgroundTaskCount.get() + 1);
        executorService.submit(new BackgroundTask(task, backgroundTaskCount));
    }

    public RomSetHandler getRomSetHandler() {
        return romSetHandler;
    }

    public void setRomSetHandler(RomSetHandler romSetHandler) {
        LOGGER.debug("Changing RomSetHandler to " + Configuration.getInstance().getMode());
        if (this.romSetHandler != null) {
            this.romSetHandler.unbind();
        }
        romSetHandler.bind(this);
        this.romSetHandler = romSetHandler;
    }

    public void exportCurrentGame() {
        Game selectedGame = gameTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentGame"));
            final File saveFile = chooser.showSaveDialog(gameTable.getScene().getWindow());
            if (saveFile != null) {
                try {
                    GameUtil.exportGameAsSNA(selectedGame, saveFile);
                } catch (IOException e) {
                    LOGGER.error("Exporting Game", e);
                }
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGameErrorTitle"),
                    LocaleUtil.i18n("exportCurrentGameErrorHeader"),
                    LocaleUtil.i18n("exportCurrentGameErrorContentNoGameSelected")).showAndWait();
        }
    }

    public void exportCurrentGamePokes() {
        Game selectedGame = gameTable.getSelectionModel().getSelectedItem();
        if (selectedGame != null && selectedGame instanceof RamGame) {
            if (GameUtil.gameHasPokes(selectedGame)) {
                FileChooser chooser = new FileChooser();
                chooser.setTitle(LocaleUtil.i18n("exportCurrentGamePokes"));
                final File saveFile = chooser.showSaveDialog(gameTable.getScene().getWindow());
                if (saveFile != null) {
                    try {
                        GameUtil.exportPokesToFile((RamGame) selectedGame, saveFile);
                    } catch (IOException e) {
                        LOGGER.error("Exporting Game Pokes", e);
                    }
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

    public void importRomSet(File romSetFile) throws IOException {
        if (getGameList().size() > 0) {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();
            if (result.orElse(ButtonType.CLOSE) == ButtonType.OK){
                getGameList().clear();
            }
        }
        InputStream is = new FileInputStream(romSetFile);
        romSetHandler.importRomSet(is);
    }

    class BackgroundTask extends FutureTask<OperationResult> {
        private IntegerProperty backgroundTaskCount;

        public BackgroundTask(Callable<OperationResult> callable, IntegerProperty backgroundTaskCount) {
            super(callable);
            this.backgroundTaskCount = backgroundTaskCount;
        }

        protected void done() {
            Platform.runLater(() -> {
                backgroundTaskCount.set(backgroundTaskCount.get() - 1);
                try {
                    OperationResult result = get();
                    if (result.isError()) {
                        DialogUtil.buildErrorAlert(result.getContext(),
                                result.getMessage(),
                                result.getDetail())
                                .showAndWait();
                    }
                } catch (Exception e) {
                    LOGGER.error("On background task completion", e);
                }

            });
        }
    }
}


