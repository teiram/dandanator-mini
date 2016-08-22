package com.grelobites.romgenerator;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
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
import java.util.concurrent.Future;

public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);
    private static final String APPLICATION_TITLE = "ROM Generator";
    private Pane romSetHandlerInfoPane;
    private final ObservableList<Game> gameList;
    private ReadOnlyObjectProperty<Game> selectedGame;
    private BooleanProperty gameSelected;
    private ImageView menuPreview;
    private StringProperty romUsageDetail;
    private DoubleProperty romUsage;
    private IntegerProperty backgroundTaskCount;
    private RomSetHandler romSetHandler;
    private Menu extraMenu;
    private StringProperty applicationTitle;
    private DirectoryAwareFileChooser fileChooser;

    private void updateApplicationTitle() {
        StringBuilder title = new StringBuilder(APPLICATION_TITLE);
        if (romSetHandler != null) {
            title.append(" - " );
            title.append(romSetHandler.type().displayName());
        }
        applicationTitle.set(title.toString());
    }

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("RomGenerator executor service");
        return t;
    });

    public ApplicationContext() {
        this.gameList = FXCollections.observableArrayList(Game::getObservable);
        this.gameSelected = new SimpleBooleanProperty(false);
        this.romUsage = new SimpleDoubleProperty();
        this.romUsageDetail = new SimpleStringProperty();
        this.backgroundTaskCount = new SimpleIntegerProperty();
        this.applicationTitle = new SimpleStringProperty();
        updateApplicationTitle();
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

    public StringProperty applicationTitleProperty() {
        return applicationTitle;
    }

    public DirectoryAwareFileChooser getFileChooser() {
        if (this.fileChooser == null) {
            this.fileChooser = new DirectoryAwareFileChooser();
        }
        return fileChooser;
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

    public ImageView getMenuPreview() {
        return menuPreview;
    }

    public void setMenuPreview(ImageView menuPreview) {
        this.menuPreview = menuPreview;
    }

    public Pane getRomSetHandlerInfoPane() {
        return romSetHandlerInfoPane;
    }

    public void setRomSetHandlerInfoPane(Pane romSetHandlerInfoPane) {
        LOGGER.debug("setRomSetHandlerInfoPane " + romSetHandlerInfoPane);
        this.romSetHandlerInfoPane = romSetHandlerInfoPane;
    }

    public Future<OperationResult> addBackgroundTask(Callable<OperationResult> task) {
        Platform.runLater(() -> backgroundTaskCount.set(backgroundTaskCount.get() + 1));
        return executorService.submit(new BackgroundTask(task, backgroundTaskCount));
    }

    public ReadOnlyObjectProperty<Game> selectedGameProperty() {
        return selectedGame;
    }

    public void setSelectedGameProperty(ReadOnlyObjectProperty<Game> selectedGameProperty) {
        selectedGame = selectedGameProperty;
        gameSelected.bind(selectedGame.isNotNull());
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
        updateApplicationTitle();
    }

    public void exportCurrentGame() {
        Game game = selectedGame.get();
        if (game != null) {
            DirectoryAwareFileChooser chooser = getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("exportCurrentGame"));
            final File saveFile = chooser.showSaveDialog(menuPreview.getScene().getWindow());
            if (saveFile != null) {
                try {
                    GameUtil.exportGameAsSNA(game, saveFile);
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

    public Menu getExtraMenu() {
        return extraMenu;
    }

    public void setExtraMenu(Menu extraMenu) {
        this.extraMenu = extraMenu;
    }


    public void importRomSet(File romSetFile) throws IOException {
        if (getGameList().size() > 0) {
            Optional<ButtonType> result = DialogUtil
                    .buildAlert(LocaleUtil.i18n("gameDeletionConfirmTitle"),
                            LocaleUtil.i18n("gameDeletionConfirmHeader"),
                            LocaleUtil.i18n("gameDeletionConfirmContent"))
                    .showAndWait();
            if (result.orElse(ButtonType.CLOSE) == ButtonType.OK) {
                getGameList().clear();
            }
        }
        InputStream is = new FileInputStream(romSetFile);
        romSetHandler.importRomSet(is);
    }

    public void mergeRomSet(File romSetFile) throws IOException {
        romSetHandler.mergeRomSet(new FileInputStream(romSetFile));
    }

    class BackgroundTask implements Callable<OperationResult> {
        private IntegerProperty backgroundTaskCount;
        private Callable<OperationResult> task;

        public BackgroundTask(Callable<OperationResult> task, IntegerProperty backgroundTaskCount) {
            this.backgroundTaskCount = backgroundTaskCount;
            this.task = task;
        }

        @Override
        public OperationResult call() throws Exception {
            final OperationResult result = task.call();
            Platform.runLater(() -> {
                backgroundTaskCount.set(backgroundTaskCount.get() - 1);
                if (result.isError()) {
                    DialogUtil.buildErrorAlert(result.getContext(),
                            result.getMessage(),
                            result.getDetail())
                            .showAndWait();
                }
            });
            return result;
        }
    }
}


