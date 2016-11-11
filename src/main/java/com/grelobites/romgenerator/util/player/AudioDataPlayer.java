package com.grelobites.romgenerator.util.player;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class AudioDataPlayer extends AudioDataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AudioDataPlayer.class);

    private MediaView mediaView;
    private DoubleProperty progressProperty;
    private Runnable onFinalization;


    protected void cleanup() {
        if (onFinalization != null) {
            onFinalization.run();
        }
        unsetBindings(mediaView.getMediaPlayer());
        super.cleanup();
    }


    private MediaPlayer getBootstrapMediaPlayer() throws IOException {

        MediaPlayer player = new MediaPlayer(new Media(getBootstrapAudioFile()
                .toURI().toURL().toExternalForm()));
        player.setOnError(() -> {
            LOGGER.error("Bootstrap Player error: " + player.getError());
            player.stop();
        });
        return player;
    }

    private MediaPlayer getBlockMediaPlayer(int block, byte[] data) throws IOException {
        MediaPlayer player = new MediaPlayer(new Media(
                getBlockAudioFile(block, data).toURI().toURL().toExternalForm()));

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

    public AudioDataPlayer(MediaView mediaView, int block, byte[] data) throws IOException {
        init(mediaView);
        mediaView.setMediaPlayer(getBlockMediaPlayer(block, data));
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
