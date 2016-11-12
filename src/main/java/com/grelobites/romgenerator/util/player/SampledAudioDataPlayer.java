package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class SampledAudioDataPlayer extends AudioDataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampledAudioDataPlayer.class);
    private static final String SERVICE_THREAD_NAME = "AudioPlayerServiceThread";
    private static final int AUDIO_BUFFER_SIZE = 4096;
    private DoubleProperty progressProperty;
    private Runnable onFinalization;
    private File mediaFile;
    private Thread serviceThread;
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    private void init() {
        progressProperty = new SimpleDoubleProperty();
        serviceThread = new Thread(null, this::playAudioFile, SERVICE_THREAD_NAME);
    }

    public SampledAudioDataPlayer() throws IOException {
        init();
        mediaFile = getBootstrapAudioFile();
    }

    public SampledAudioDataPlayer(int block, byte[] data) throws IOException {
        init();
        mediaFile = getBlockAudioFile(block, data);
    }

    private Mixer getMixer() {
        String mixerName = PlayerConfiguration.getInstance()
                .getAudioMixerName();
        Mixer.Info[] mixerInfos =  AudioSystem.getMixerInfo();
        if (mixerName != null) {
            for (Mixer.Info mixerInfo : mixerInfos) {
                LOGGER.debug("Mixer " + mixerInfo);
                if (mixerInfo.getName().equals(mixerName)) {
                    return AudioSystem.getMixer(mixerInfo);
                }
            }
            LOGGER.warn("Unable to find configured mixer " + mixerName + ". Using default mixer " + mixerInfos[0]);
        }
        return AudioSystem.getMixer(mixerInfos[0]);
    }

    private void playAudioFile() {
        try {
            Mixer mixer = getMixer();
            LOGGER.debug("Playing audio on mixer " + mixer.getMixerInfo());
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(mediaFile);
            long length = mediaFile.length();
            int written = 0;
            AudioFormat audioFormat = audioInputStream.getFormat();
            LOGGER.debug("Audio format from media file is " + audioFormat);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine soundLine = (SourceDataLine) mixer.getLine(info);
            soundLine.open(audioFormat);
            soundLine.start();
            int nBytesRead = 0;
            byte[] sampledData = new byte[AUDIO_BUFFER_SIZE];
            while (nBytesRead != -1) {
                nBytesRead = audioInputStream.read(sampledData, 0, sampledData.length);

                if (nBytesRead > 0) {
                    written += soundLine.write(sampledData, 0, nBytesRead);
                }
                final double progress = 1.0 * written / length;
                Platform.runLater(() -> progressProperty.set(progress));
                if (state != State.RUNNING) {
                    LOGGER.debug("No longer in running state");
                    break;
                }
            }
            soundLine.drain();
            soundLine.stop();
            if (state == State.RUNNING && onFinalization != null) {
                //Only when we are not stopped programmatically (end of stream)
                Platform.runLater(onFinalization);
            }
        } catch (Exception e) {
            LOGGER.error("Playing audio", e);
        } finally {
            state = State.STOPPED;
            LOGGER.debug("State is now STOPPED");
        }
    }

    @Override
    public void send() {
        state = State.RUNNING;
        serviceThread.start();
    }

    @Override
    public void stop() {
        if (state == State.RUNNING) {
            state = State.STOPPING;
            LOGGER.debug("State changed to STOPPING");

            while (state != State.STOPPED) {
                try {
                    serviceThread.join();
                } catch (InterruptedException e) {
                    LOGGER.debug("Joining thread was interrupted", e);
                }
            }
            LOGGER.debug("Stop operation acknowledged");
        }
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
        return Optional.empty();
    }
}
