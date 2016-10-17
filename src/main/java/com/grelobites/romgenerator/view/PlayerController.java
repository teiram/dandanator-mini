package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.AudioDataPlayer;
import com.grelobites.romgenerator.util.player.DataPlayer;
import com.grelobites.romgenerator.util.player.FrequencyDetector;
import com.grelobites.romgenerator.util.player.SerialDataPlayer;
import javafx.animation.FadeTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class PlayerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerController.class);

    private static final int ROMSET_SIZE = Constants.SLOT_SIZE * 32;

    private static final int LOADER_BLOCK = -1;
    private static final int PAUSE_AFTER_LOADER = 2000;
    private static final int DETECTION_TIMEOUT = 3000;

    private static final String LOADER_STRING = "Loader";
    private static PlayerConfiguration configuration = PlayerConfiguration.getInstance();

    private static final String PLAY_BUTTON_STYLE = "button-play";
    private static final String STOP_BUTTON_STYLE = "button-stop";

    @FXML
    private Button playButton;

    @FXML
    private Button rewindButton;

    @FXML
    private Button forwardButton;

    @FXML
    private ProgressBar blockProgress;

    @FXML
    private ProgressBar overallProgress;

    @FXML
    private MediaView mediaView;

    @FXML
    private Label currentBlockLabel;

    @FXML
    private Circle playingLed;

    @FXML
    private Circle recordingLed;

    @FXML
    private ImageView playerImage;

    @FXML
    private ImageView beeImage;

    @FXML
    private Label failuresCount;

    private IntegerProperty consecutiveFailures;

    private ApplicationContext applicationContext;

    private BooleanProperty playing;

    private byte[] romsetByteArray;

    private IntegerProperty currentBlock;

    private BooleanProperty nextBlockRequested;

    private IntegerProperty startingBlockProperty;

    private ObjectProperty<DataPlayer> currentPlayer;

    private static void doAfterDelay(int delay, Runnable r) {
        Task<Void> sleeper = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    LOGGER.warn("Delay thread was interrupted", e);
                }
                return null;
            }
        };
        sleeper.setOnSucceeded(event -> r.run());
        new Thread(sleeper).start();
    }

    private void playBlinkingTransition(int duration) {
        playingLed.setVisible(true);
        FadeTransition ft = new FadeTransition(Duration.millis(1000), playingLed);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(duration / 1000);
        ft.setAutoReverse(true);
        ft.play();
    }

    public PlayerController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        playing = new SimpleBooleanProperty(false);
        startingBlockProperty = new SimpleIntegerProperty(configuration.getSendLoader() ?
                LOADER_BLOCK : 0);
        currentBlock = new SimpleIntegerProperty(startingBlockProperty.get());
        nextBlockRequested = new SimpleBooleanProperty();
        currentPlayer = new SimpleObjectProperty<>();
        consecutiveFailures = new SimpleIntegerProperty(0);
    }


    private byte[] getRomsetByteArray() throws IOException {
        if (romsetByteArray == null) {
            if (configuration.getCustomRomSetPath() != null) {
                try (FileInputStream fis = new FileInputStream(configuration.getCustomRomSetPath())) {
                    romsetByteArray = Util.fromInputStream(fis);
                }
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                applicationContext.getRomSetHandler().exportRomSet(bos);
                romsetByteArray = bos.toByteArray();
            }
        }
        return romsetByteArray;
    }

    private DataPlayer getBootstrapMediaPlayer() throws IOException {
        return new AudioDataPlayer(mediaView);
    }

    private DataPlayer getBlockMediaPlayer(int block) throws IOException {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize];
        System.arraycopy(getRomsetByteArray(), block * blockSize, buffer, 0, blockSize);

        return configuration.isUseSerialPort() ?
                new SerialDataPlayer(block, buffer) :
                new AudioDataPlayer(mediaView, block,  buffer);
    }

    private void playCurrentBlock() {
        LOGGER.debug("playBlock with block " + currentBlock + " requested");
        nextBlockRequested.set(nextBlockRequested.not().get());
    }

    private void calculateNextBlock() throws IOException {
        if (configuration.isUseTargetFeedback()) {

            FrequencyDetector detector = FrequencyDetector.builder()
                    .withTimeoutMillis(DETECTION_TIMEOUT)
                    .withFrequencyConsumer(f -> f.map(frequency -> {
                        if (frequency == FrequencyDetector.SUCCESS_FREQUENCY) {
                            LOGGER.debug("Detected success tone");
                            consecutiveFailures.set(0);
                            if (currentBlock.get() != LOADER_BLOCK) {
                                playBlinkingTransition(configuration.getRecordingPause());
                            }
                            doAfterDelay(currentBlock.get() == LOADER_BLOCK ?
                                    PAUSE_AFTER_LOADER : configuration.getRecordingPause(), () -> {
                                currentBlock.set(currentBlock.get() + 1);
                                playCurrentBlock();
                            });
                        } else {
                            LOGGER.debug("Detected something else");
                            playCurrentBlock();
                            consecutiveFailures.set(consecutiveFailures.get() + 1);
                        }
                        recordingLed.setVisible(false);
                        return 0;
                    }).orElseGet(() -> {
                        LOGGER.debug("Fallback to repeat current block");
                        recordingLed.setVisible(false);
                        consecutiveFailures.set(consecutiveFailures.get() + 1);
                        playCurrentBlock();
                        return null;
                    })).build();

            recordingLed.setVisible(true);
            LOGGER.debug("Started detection");
            detector.start();
        } else {
            LOGGER.debug("Playing next block on skipped detection");
            if (currentBlock.get() != LOADER_BLOCK) {
                playBlinkingTransition(configuration.getRecordingPause());
            }
            doAfterDelay(currentBlock.get() == LOADER_BLOCK ?
                    PAUSE_AFTER_LOADER : configuration.getRecordingPause(), () -> {
                currentBlock.set(currentBlock.get() + 1);
                playCurrentBlock();
            });
        }
    }

    private void onEndOfMedia() {
        try {
            playingLed.setVisible(false);
            calculateNextBlock();
        } catch (Exception e) {
            LOGGER.error("Setting next player", e);
        }
    }

    private void unbindPlayer(DataPlayer player) {
        LOGGER.debug("Unbinding player " + player);
        blockProgress.progressProperty().unbind();
    }

    private void bindPlayer(DataPlayer player) {
        LOGGER.debug("Binding player " + player);
        blockProgress.progressProperty().bind(player.progressProperty());
    }

    private void initMediaPlayer(DataPlayer player) {
        this.currentPlayer.set(player);
        player.onFinalization(this::onEndOfMedia);
        play();
    }

    private void play() {
        playingLed.setVisible(true);
        playButton.getStyleClass().removeAll(PLAY_BUTTON_STYLE);
        playButton.getStyleClass().add(STOP_BUTTON_STYLE);
        currentPlayer.get().send();
        playing.set(true);
    }

    private void stop() {
        if (playing.get()) {
            playingLed.setVisible(false);
            playButton.getStyleClass().removeAll(STOP_BUTTON_STYLE);
            playButton.getStyleClass().add(PLAY_BUTTON_STYLE);
            LOGGER.debug("Stopping player");
            currentPlayer.get().stop();
            consecutiveFailures.set(0);
            playing.set(false);
        }
    }

    private String getCurrentBlockString() {
        int blockNumber = currentBlock.get();
        int totalBlocks = ROMSET_SIZE / configuration.getBlockSize();
        if (blockNumber >= 0) {
            if (blockNumber < totalBlocks) {
                return String.format("%d/%d", blockNumber + 1, totalBlocks);
            } else {
                return "";
            }
        } else {
            return LOADER_STRING;
        }
    }

    @FXML
    void initialize() throws IOException {
        playingLed.setVisible(false);
        recordingLed.setVisible(false);

        //React to changes in the game list
        applicationContext.getGameList().addListener((InvalidationListener) e -> {
            if (configuration.getCustomRomSetPath() == null) {
                stop();
                romsetByteArray = null;
            }
        });

        configuration.customRomSetPathProperty().addListener(e -> {
            stop();
            romsetByteArray = null;
        });

        configuration.loaderPathProperty().addListener(e -> {
            stop();
            romsetByteArray = null;
        });

        playerImage.setImage(configuration.isUseSerialPort() ? configuration.getKempstonImage() :
            configuration.getCassetteImage());

        beeImage.visibleProperty().bind(consecutiveFailures.greaterThan(0));
        failuresCount.visibleProperty().bind(consecutiveFailures.greaterThan(0));

        configuration.useSerialPortProperty().addListener(
                (observable, oldValue, newValue) ->
                        playerImage.setImage(newValue ? configuration.getKempstonImage() :
                                configuration.getCassetteImage()));

        configuration.sendLoaderProperty().addListener(
                (observable, oldValue, newValue) -> {
                    stop();
                    if (!newValue && currentBlock.get() == LOADER_BLOCK) {
                        currentBlock.set(0);
                    }
                    if (newValue && currentBlock.get() == 0) {
                        currentBlock.set(LOADER_BLOCK);
                    }
        });
        startingBlockProperty.bind(Bindings.createIntegerBinding(() ->
                configuration.getSendLoader() ? LOADER_BLOCK : 0, configuration.sendLoaderProperty()));

        failuresCount.textProperty().bind(consecutiveFailures.asString());

        rewindButton.disableProperty().bind(playing
                .or(currentBlock.isEqualTo(startingBlockProperty)));
        forwardButton.disableProperty().bind(playing
                .or(currentBlock.isEqualTo(ROMSET_SIZE / configuration.getBlockSize() - 1)));
        playButton.disableProperty().bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                .or(applicationContext.getRomSetHandler().generationAllowedProperty().not())
                .and(configuration.customRomSetPathProperty().isNull()));

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                Math.max(0, currentBlock.doubleValue() / (ROMSET_SIZE /
                        configuration.getBlockSize())), currentBlock));

        currentBlockLabel.textProperty().bind(Bindings
                .createStringBinding(this::getCurrentBlockString, currentBlock));

        nextBlockRequested.addListener(observable -> {
            LOGGER.debug("nextBlockRequested listener triggered with currentBlock " + currentBlock);
            if (playing.get()) {
                try {
                    if (currentBlock.get() == LOADER_BLOCK) {
                        initMediaPlayer(getBootstrapMediaPlayer());
                    } else if (currentBlock.get() * configuration.getBlockSize() < ROMSET_SIZE) {
                        initMediaPlayer(getBlockMediaPlayer(currentBlock.get()));
                    } else {
                        stop();
                        currentBlock.set(startingBlockProperty.get());
                    }
                } catch (Exception e) {
                    LOGGER.error("Setting up player", e);
                }
            }
        });

        playButton.setOnAction(c -> {
            try {
                if (!playing.get()) {
                    playing.set(true);
                    playCurrentBlock();
                } else {
                    stop();
                }
            } catch (Exception e) {
                LOGGER.error("Getting ROMSet block", e);
            }
        });

        rewindButton.setOnAction(c -> {
            if (currentBlock.get() > LOADER_BLOCK) {
                currentBlock.set(currentBlock.get() - 1);
            }
        });

        forwardButton.setOnAction(c -> {
            if ((currentBlock.get() + 1) * configuration.getBlockSize() < ROMSET_SIZE) {
                currentBlock.set(currentBlock.get() + 1);
            }
        });

        currentPlayer.addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                unbindPlayer(oldValue);
            }
            if (newValue != null) {
                bindPlayer(newValue);
            }
        });
    }

}
