package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Optional;

public class AudioDataPlayer extends DataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioDataPlayer.class);
    private MediaView mediaView;
    private DoubleProperty progressProperty;
    private File temporaryFile;
    private Runnable onFinalization;

    private File getTemporaryFile() throws IOException {
        if (temporaryFile == null) {
            temporaryFile = File.createTempFile("romgenerator", ".wav");
        }
        return temporaryFile;
    }

    private void cleanup() {
        if (onFinalization != null) {
            onFinalization.run();
        }
        unsetBindings(mediaView.getMediaPlayer());
        if (temporaryFile != null) {
            if (!temporaryFile.delete()) {
                LOGGER.warn("Unable to delete temporary file " + temporaryFile);
            }
            temporaryFile = null;
        }
    }

    private MediaPlayer getBootstrapMediaPlayer() throws IOException {
        FileOutputStream fos = new FileOutputStream(getTemporaryFile());
        byte[] loaderTap = TapUtil.generateLoaderTap(configuration.getLoaderStream(),
                configuration.isUseTargetFeedback());

        TapUtil.tap2wav(StandardWavOutputFormat.builder()
                        .withSampleRate(CompressedWavOutputFormat.SRATE_44100)
                        .withChannelType(ChannelType.valueOf(configuration.getAudioMode()))
                        .withPilotDurationMillis(5000)
                        .build(),
                new ByteArrayInputStream(loaderTap),
                fos);
        fos.close();
        MediaPlayer player = new MediaPlayer(new Media(getTemporaryFile().toURI().toURL().toExternalForm()));
        player.setOnError(() -> {
            LOGGER.error("Bootstrap Player error: " + player.getError());
            player.stop();
        });
        return player;
    }

    private MediaPlayer getBlockMediaPlayer(int block, byte[] data,
                                            EncodingSpeedPolicy encodingSpeedPolicy) throws IOException {
        int blockSize = configuration.getBlockSize();
        byte[] buffer = new byte[blockSize + 3];
        System.arraycopy(data, 0, buffer, 0, blockSize);

        buffer[blockSize] = Integer.valueOf(block + 1).byteValue();

        Util.writeAsLittleEndian(buffer, blockSize + 1, getBlockCrc(buffer, blockSize + 1));

        File tempFile = getTemporaryFile();
        LOGGER.debug("Creating new MediaPlayer for block " + block + " on file " + tempFile);
        FileOutputStream fos = new FileOutputStream(tempFile);
        encodeBuffer(buffer, encodingSpeedPolicy, fos);
        fos.close();
        MediaPlayer player = new MediaPlayer(new Media(tempFile.toURI().toURL().toExternalForm()));
        player.setOnError(() -> LOGGER.error("Player error: " + player.getError()));
        return player;
    }

    private void setBindings(MediaPlayer player) {
        progressProperty.bind(Bindings.createDoubleBinding(() -> {
            if (player.getCurrentTime() != null && player.getTotalDuration() != null) {
                return player.getCurrentTime().toSeconds() / player.getTotalDuration().toSeconds();
            } else {
                return 0.0;
            }
        }, player.currentTimeProperty(), player.totalDurationProperty()));
        player.setOnEndOfMedia(() -> cleanup());
    }

    private void unsetBindings(MediaPlayer player) {
        progressProperty.unbind();
    }

    private void init(MediaView mediaView) {
        progressProperty = new SimpleDoubleProperty();
        this.mediaView = mediaView;
    }

    public AudioDataPlayer(MediaView mediaView) throws IOException {
        init(mediaView);
        mediaView.setMediaPlayer(getBootstrapMediaPlayer());
        setBindings(mediaView.getMediaPlayer());
    }

    public AudioDataPlayer(MediaView mediaView, int block, byte[] data,
                           EncodingSpeedPolicy encodingSpeedPolicy) throws IOException {
        init(mediaView);
        mediaView.setMediaPlayer(getBlockMediaPlayer(block, data, encodingSpeedPolicy));
        setBindings(mediaView.getMediaPlayer());
    }

    @Override
    public void send() {
        mediaView.getMediaPlayer().play();
    }

    @Override
    public void stop() {
        mediaView.getMediaPlayer().stop();
    }

    @Override
    public void onFinalization(Runnable onFinalization) {
        this.onFinalization = onFinalization;
    }

    @Override
    public DoubleProperty progressProperty() {
        return progressProperty;
    }

    @Override
    public Optional<DoubleProperty> volumeProperty() {
        return Optional.of(mediaView.getMediaPlayer().volumeProperty());
    }
}
