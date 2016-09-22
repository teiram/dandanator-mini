package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.CompressedWavOutputStream;
import com.grelobites.romgenerator.util.player.FrequencyDetector;
import com.grelobites.romgenerator.util.player.WavOutputFormat;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
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
    private static final int BLOCK_SIZE = 4096;

    private static final double OK_TONE = 4000.0;

    private boolean useTargetFeedback = true;

    @FXML
    private Button playButton;

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

    public PlayerController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        playing = new SimpleBooleanProperty(false);
        currentBlock = -1;
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
        String resource = PlayerController.class.getResource("/loader.wav").toExternalForm();
        LOGGER.debug("getBootstrapMediaPlayer from resource " + resource);
        byte[] wavData = Util.fromInputStream(PlayerController.class.getResourceAsStream("/loader.wav"));
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
        byte[] buffer = new byte[BLOCK_SIZE + 1];
        System.arraycopy(getRomsetByteArray(), block * BLOCK_SIZE, buffer, 0, BLOCK_SIZE);
        buffer[BLOCK_SIZE] = Integer.valueOf(block + 1).byteValue();
        File tempFile = getTemporaryFile();
        LOGGER.debug("Creating new MediaPlayer for block " + block + " on file " + tempFile);
        FileOutputStream fos = new FileOutputStream(tempFile);
        CompressedWavOutputStream wos = new CompressedWavOutputStream(fos, WavOutputFormat.defaultDataFormat());
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
        if (useTargetFeedback) {
            FrequencyDetector detector = new FrequencyDetector(3000, (f) -> {
                f.map(frequency -> {
                    if (Math.abs(frequency - OK_TONE) < 100.0) {
                        LOGGER.debug("Detected success tone");
                        playBlock(currentBlock + 1);
                    } else {
                        LOGGER.debug("Detected something else");
                        playBlock(currentBlock);
                    }
                    return null;
                }).orElseGet(() -> {
                    playBlock(currentBlock);
                    return null;
                });
            });
            recordingLed.setVisible(true);
            LOGGER.debug("Started detection");
            detector.start();
        } else {
            playBlock(currentBlock + 1);
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

    @FXML
    void initialize() throws IOException {
        playingLed.setVisible(false);
        recordingLed.setVisible(false);

        playButton.disableProperty().bind(applicationContext.backgroundTaskCountProperty().greaterThan(0)
                .or(applicationContext.getRomSetHandler().generationAllowedProperty().not()));

        volumeSlider.setValue(1.0);
        volumeSlider.disableProperty().bind(playing.not());

        overallProgress.progressProperty().bind(Bindings.createDoubleBinding(() -> {
            return Integer.valueOf(currentBlock).doubleValue() / (ROMSET_SIZE / BLOCK_SIZE);
        }, nextBlockRequested));

        nextBlockRequested.addListener(observable -> {
            LOGGER.debug("nextBlockRequested listener triggered with currentBlock " + currentBlock);
            if (playing.get()) {
                try {
                    if (currentBlock < 0) {
                        MediaPlayer player = getBootstrapMediaPlayer();
                        player.setOnEndOfMedia(() -> onEndOfMedia());
                        mediaView.setMediaPlayer(player);
                        player.play();
                        playingLed.setVisible(true);
                        currentBlockLabel.setText("Loader");
                        playButton.setText("||");
                    } else if (currentBlock * BLOCK_SIZE < ROMSET_SIZE) {
                        MediaPlayer player = getBlockMediaPlayer(currentBlock);
                        player.setOnEndOfMedia(() -> onEndOfMedia());
                        mediaView.setMediaPlayer(player);
                        player.play();
                        playingLed.setVisible(true);
                        currentBlockLabel.setText(String.format("%d/%d", currentBlock + 1, ROMSET_SIZE / BLOCK_SIZE));
                        playButton.setText("||");
                    } else {
                        playing.set(false);
                        playButton.setText(">");
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
                    playButton.setText("||");
                    playing.set(true);
                } else {
                    //Stop
                    mediaView.getMediaPlayer().pause();
                    playingLed.setVisible(false);
                    playButton.setText(">");
                    playing.set(false);

                }
            } catch (Exception e) {
                LOGGER.error("Getting ROMSet block", e);
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
