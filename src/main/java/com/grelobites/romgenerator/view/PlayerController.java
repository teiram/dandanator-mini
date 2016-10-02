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
    private static final int LOADER_BLOCK = -1;

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

    private IntegerProperty currentBlock;

    private BooleanProperty nextBlockRequested;

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
        currentBlock = new SimpleIntegerProperty(LOADER_BLOCK);
        nextBlockRequested = new SimpleBooleanProperty();
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

    private static int getBlockCrc(byte[] data, int blockSize) {
        int sum = 0;
        for (byte value : data) {
            sum += Byte.toUnsignedInt(value);
        }
        return sum & 0xffff;
    }

    private MediaPlayer getBlockMediaPlayer(int block) throws IOException {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize + 3];
        System.arraycopy(getRomsetByteArray(), block * blockSize, buffer, 0, blockSize);

        buffer[blockSize] = Integer.valueOf(block + 1).byteValue();

        Util.writeAsLittleEndian(buffer, blockSize + 1, getBlockCrc(buffer, blockSize));

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

    private void playCurrentBlock() {
        LOGGER.debug("playBlock with block " + currentBlock + " requested");
        recordingLed.setVisible(false);
        nextBlockRequested.set(nextBlockRequested.not().get());
    }

    private void calculateNextBlock() throws IOException {
        if (configuration.isUseTargetFeedback()) {

            FrequencyDetector detector = FrequencyDetector.builder()
                    .withTimeoutMillis(3000)
                    .withFrequencyConsumer(f -> f.map(frequency -> {
                        if (Math.abs(frequency - OK_TONE) < 100.0) {
                            LOGGER.debug("Detected success tone");
                            doAfterDelay(configuration.getPauseBetweenBlocks(), () -> {
                                currentBlock.set(currentBlock.get() + 1);
                                playCurrentBlock();
                            });
                        } else {
                            LOGGER.debug("Detected something else");
                            playCurrentBlock();
                        }
                        return 0;
                    }).orElseGet(() -> {
                        LOGGER.debug("Fallback to repeat current block");
                        playCurrentBlock();
                        return null;
                    })).build();

            recordingLed.setVisible(true);
            LOGGER.debug("Started detection");
            detector.start();
        } else {
            LOGGER.debug("Playing next block on skipped detection");
            doAfterDelay(configuration.getPauseBetweenBlocks(), () -> {
                currentBlock.set(currentBlock.get() + 1);
                playCurrentBlock();
            });
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

        rewindButton.disableProperty().bind(playing
                .or(currentBlock.isEqualTo(LOADER_BLOCK)));
        forwardButton.disableProperty().bind(playing
                .or(currentBlock.isEqualTo(ROMSET_SIZE / configuration.getBlockSize() - 1)));
        playButton.disableProperty().bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                .or(applicationContext.getRomSetHandler().generationAllowedProperty().not()));

        volumeSlider.setValue(1.0);
        volumeSlider.disableProperty().bind(playing.not());

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() ->
                Math.max(0, currentBlock.doubleValue() / (ROMSET_SIZE /
                        configuration.getBlockSize())), currentBlock));

        currentBlockLabel.textProperty().bind(Bindings.createStringBinding(() ->
                currentBlock.get() >= 0 ? String.format("%d/%d", currentBlock.get() + 1,
                        ROMSET_SIZE / configuration.getBlockSize()) : "Loader", currentBlock));

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
                    playCurrentBlock();
                } else {
                    //Stop
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
