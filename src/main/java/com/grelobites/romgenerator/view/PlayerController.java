package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.ChannelType;
import com.grelobites.romgenerator.util.player.CompressedWavOutputStream;
import com.grelobites.romgenerator.util.player.FrequencyDetector;
import com.grelobites.romgenerator.util.player.WavOutputFormat;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Circle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PlayerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerController.class);

    private static final int ROMSET_SIZE = Constants.SLOT_SIZE * 32;

    private static final double OK_TONE = 4000.0;

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
    private Slider volumeSlider;

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

    private File temporaryFile;

    private ApplicationContext applicationContext;

    private BooleanProperty playing;

    private byte[] romsetByteArray;

    private int currentBlock;


    private IntegerProperty nextBlockRequested;

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

    public PlayerController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        playing = new SimpleBooleanProperty(false);
        currentBlock = 0;
        nextBlockRequested = new SimpleIntegerProperty(-1);
    }

    private File getTemporaryFile() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("romgenerator", ".wav");
        }
        return temporaryFile;
    }

    private void cleanup() {
        if (temporaryFile != null) {
            temporaryFile.delete();
            temporaryFile = null;
        }
    }

    private byte[] getRomsetByteArray() throws IOException {
        if (romsetByteArray == null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            applicationContext.getRomSetHandler().exportRomSet(bos);
            romsetByteArray = bos.toByteArray();
        }
        return romsetByteArray;
    }

    private MediaPlayer getBootstrapMediaPlayer() throws IOException {
        byte[] wavData = Util.fromInputStream(configuration.getLoaderStream());
        File tempFile = getTemporaryFile();
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(wavData);
        fos.close();
        MediaPlayer player = new MediaPlayer(new Media(tempFile.toURI().toURL().toExternalForm()));
        player.setOnError(() -> {
            LOGGER.error("Bootstrap Player error: " + player.getError());
            player.stop();
        });
        return player;
    }

    private MediaPlayer getBlockMediaPlayer(int block) throws IOException {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize + 1];
        System.arraycopy(getRomsetByteArray(), block * blockSize, buffer, 0, blockSize);
        buffer[blockSize] = Integer.valueOf(block + 1).byteValue();
        File tempFile = getTemporaryFile();
        LOGGER.debug("Creating new MediaPlayer for block " + block + " on file " + tempFile);
        FileOutputStream fos = new FileOutputStream(tempFile);
        CompressedWavOutputStream wos = new CompressedWavOutputStream(fos,
                WavOutputFormat.builder()
                    .withSampleRate(WavOutputFormat.SRATE_44100)
                    .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                    .withSpeed(configuration.getEncodingSpeed())
                    .withFlagByte(WavOutputFormat.DATA_FLAG_BYTE)
                    .withOffset(WavOutputFormat.DEFAULT_OFFSET)
                    .withPilotDurationMillis(configuration.getPilotLength())
                    .withFinalPauseDurationMillis(configuration.getTrailLength())
                    .build());
        wos.write(buffer);
        wos.close();
        fos.close();
        MediaPlayer player = new MediaPlayer(new Media(tempFile.toURI().toURL().toExternalForm()));
        player.setOnError(() -> LOGGER.error("Player error: " + player.getError()));
        return player;
    }

    private void playBlock(int block) {
        LOGGER.debug("playBlock with block " + block + " requested");
        recordingLed.setVisible(false);
        currentBlock = block;
        nextBlockRequested.set(nextBlockRequested.get() + 1);
    }

    private void calculateNextBlock() throws IOException {
        if (configuration.isUseTargetFeedback()) {
            FrequencyDetector detector = new FrequencyDetector(3000, (f) -> {
                f.map(frequency -> {
                    if (Math.abs(frequency - OK_TONE) < 100.0) {
                        LOGGER.debug("Detected success tone");
                        doAfterDelay(configuration.getPauseBetweenBlocks(), () -> playBlock(currentBlock + 1));
                    } else {
                        LOGGER.debug("Detected something else");
                        playBlock(currentBlock);
                    }
                    return 0;
                }).orElseGet(() -> {
                    LOGGER.debug("Fallback to repeat current block");
                    playBlock(currentBlock);
                    return null;
                });
            });
            recordingLed.setVisible(true);
            LOGGER.debug("Started detection");
            detector.start();
        } else {
            LOGGER.debug("Playing next block on skipped detection");
            doAfterDelay(configuration.getPauseBetweenBlocks(), () -> playBlock(currentBlock + 1));
        }
    }

    private void onEndOfMedia() {
        try {
            playingLed.setVisible(false);
            cleanup();
            calculateNextBlock();
        } catch (Exception e) {
            LOGGER.error("Setting next player", e);
        }
    }

    private void unbindPlayer(MediaPlayer player) {
        player.volumeProperty().unbindBidirectional(volumeSlider.valueProperty());
        blockProgress.progressProperty().unbind();
    }

    private void bindPlayer(MediaPlayer player) {
        player.volumeProperty().bindBidirectional(volumeSlider.valueProperty());
        blockProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            if (player.getCurrentTime() != null && player.getTotalDuration() != null) {
                return player.getCurrentTime().toSeconds() / player.getTotalDuration().toSeconds();
            } else {
                return 0.0;
            }
        }, player.currentTimeProperty(), player.totalDurationProperty()));
    }

    private void initMediaPlayer(MediaPlayer player) {
        player.setOnEndOfMedia(() -> onEndOfMedia());
        mediaView.setMediaPlayer(player);
        play();
    }

    private void play() {
        playingLed.setVisible(true);
        playButton.getStyleClass().removeAll(PLAY_BUTTON_STYLE);
        playButton.getStyleClass().add(STOP_BUTTON_STYLE);
        mediaView.getMediaPlayer().play();
        playing.set(true);
    }

    private void stop() {
        playingLed.setVisible(false);
        playButton.getStyleClass().removeAll(STOP_BUTTON_STYLE);
        playButton.getStyleClass().add(PLAY_BUTTON_STYLE);
        mediaView.getMediaPlayer().stop();
        playing.set(false);
    }

    @FXML
    void initialize() throws IOException {
        playingLed.setVisible(false);
        recordingLed.setVisible(false);

        playButton.disableProperty().bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                .or(applicationContext.getRomSetHandler().generationAllowedProperty().not()));

        volumeSlider.setValue(1.0);
        volumeSlider.disableProperty().bind(playing.not());

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            return Math.max(0, Integer.valueOf(currentBlock).doubleValue() / (ROMSET_SIZE /
                    configuration.getBlockSize()));
        }, nextBlockRequested));

        nextBlockRequested.addListener(observable -> {
            LOGGER.debug("nextBlockRequested listener triggered with currentBlock " + currentBlock);
            if (playing.get()) {
                try {
                    if (currentBlock < 0) {
                        initMediaPlayer(getBootstrapMediaPlayer());
                        currentBlockLabel.setText("Loader");
                    } else if (currentBlock * configuration.getBlockSize() < ROMSET_SIZE) {
                        initMediaPlayer(getBlockMediaPlayer(currentBlock));
                        currentBlockLabel.setText(String.format("%d/%d", currentBlock + 1,
                                ROMSET_SIZE / configuration.getBlockSize()));
                    } else {
                        stop();
                    }
                } catch (Exception e) {
                    LOGGER.error("Setting up player", e);
                }
            }
        });

        playButton.setOnAction(c -> {
            try {
                if (!playing.get()) {
                    //Start playing
                    playing.set(true);
                    playBlock(-1);
                } else {
                    //Stop
                    stop();
                }
            } catch (Exception e) {
                LOGGER.error("Getting ROMSet block", e);
            }
        });

        rewindButton.setOnAction(c -> {
            try {
                if (playing.get() && currentBlock > 0) {
                    mediaView.getMediaPlayer().stop();
                    playBlock(currentBlock - 1);
                }
            } catch (Exception e) {
                LOGGER.error("Trying to rewind", e);
            }
        });

        forwardButton.setOnAction(c -> {
            try {
                if (playing.get() && (currentBlock + 1) * configuration.getBlockSize() < ROMSET_SIZE) {
                    mediaView.getMediaPlayer().stop();
                    playBlock(currentBlock + 1);
                }
            } catch (Exception e) {
                LOGGER.error("Trying to fast forward", e);
            }
        });

        mediaView.mediaPlayerProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != null) {
                unbindPlayer(oldValue);
            }
            if (newValue != null) {
                bindPlayer(newValue);
            }
        });
    }

}
