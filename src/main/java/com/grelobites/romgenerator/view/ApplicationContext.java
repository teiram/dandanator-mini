package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ApplicationContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationContext.class);

    private final ObservableList<Game> gameList;
    private final BooleanProperty gameSelected;
    private final ImageView menuPreviewImage;
    private DoubleProperty romUsage;
    private IntegerProperty backgroundTaskCount;

    private final ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("RomGenerator executor service");
        return t ;
    });

    public ApplicationContext(ImageView menuPreviewImage) {
        this.gameList = FXCollections.observableArrayList(Game::getObservable);
        this.gameSelected = new SimpleBooleanProperty(false);
        this.menuPreviewImage = menuPreviewImage;
        this.romUsage = new SimpleDoubleProperty();
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


