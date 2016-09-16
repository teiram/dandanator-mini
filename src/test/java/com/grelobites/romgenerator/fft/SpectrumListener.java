package com.grelobites.romgenerator.fft;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class SpectrumListener  extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpectrumListener.class);
    private static final int WIDTH = 512;
    private static final int HEIGHT = 400;
    private ImageView spectrumImageView = new ImageView();
    private Canvas canvas = new Canvas(WIDTH, HEIGHT);
    private Label frequencyLabel = new Label("--");

    private void smoothSignal(ByteBuffer buffer, int samples, double smoothing) {
        buffer.rewind();
        short value = buffer.getShort(); // start with the first input
        for (int i = 0; i < samples; i += 2) {
            short currentValue = buffer.getShort();
            value += (currentValue - value) / smoothing;
            buffer.putShort(i, value);
        }
    }

    private void updateCanvas(Complex[] spectrum) {
        double[] energy = Arrays.stream(spectrum).mapToDouble(c -> c.abs()).toArray();
        final double max = Arrays.stream(energy).max().getAsDouble();

        double[] normalized = Arrays.stream(energy).map(d -> d / max).toArray();
        Platform.runLater(() -> {
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(Color.BLACK);
            gc.fillRect(0, 0, WIDTH, HEIGHT);
            gc.setLineWidth(1.0);
            for (int i = 0; i < spectrum.length / 2; i++) {
                gc.setStroke(Color.GREEN);
                gc.setLineDashes(null);
                gc.strokeLine(i, HEIGHT - 1,
                        i, HEIGHT - 1 - Double.valueOf(normalized[i] * (HEIGHT - 1)).intValue());
                if (i % 23 == 0) {
                    gc.setStroke(Color.WHITE);
                    gc.setLineDashes(2.0);
                    gc.strokeLine(i, HEIGHT - 1, i, 0);
                }
            }
        });
    }

    private void updateImageView(Complex[] spectrum) {
        WritableImage image = new WritableImage(WIDTH, HEIGHT);
        double[] energy = Arrays.stream(spectrum).mapToDouble(c -> c.abs()).toArray();
        final double max = Arrays.stream(energy).max().getAsDouble();

        double[] normalized = Arrays.stream(energy).map(d -> d / max).toArray();

        PixelWriter writer = image.getPixelWriter();
        for (int i = 0; i < spectrum.length / 2; i++) {
            writer.setArgb(i,
                    HEIGHT - 1
                            - Double.valueOf(normalized[i] * (HEIGHT - 1)).intValue(), 0xff000000);
        }
        Platform.runLater(() -> spectrumImageView.setImage(image));
    }

    private int getDominantFrequency(byte[] data) {
        int samples = data.length / 2;
        Complex[] source = new Complex[samples];
        ByteBuffer buffer = ByteBuffer.wrap(data)
                .order(ByteOrder.BIG_ENDIAN);
        //smoothSignal(buffer, samples, 100);
        buffer.rewind();
        for (int i = 0; i < samples; i++) {
            source[i] = new Complex(buffer.getShort(), 0);
        }
        Complex[] result = FFT.fft(source);
        double maxValue = 0.0;
        double minValue = Double.MAX_VALUE;
        int freqIndex = 0;
        for (int j = 0; j <= result.length / 2; j++) {
            double value = result[j].abs();
            if (value > maxValue) {
                maxValue = value;
                freqIndex = j;
            }
            if (value < minValue) {
                minValue = value;
            }
        }
        updateCanvas(result);

        return freqIndex;
    }

    private static boolean checkThreshold(ByteBuffer buffer, int samples) {
        int maximum = 0;
        for (int i = 0; i < samples; i++) {
            short sample = buffer.getShort();
            if (Math.abs(sample) > maximum) {
                maximum = Math.abs(sample);
            }
        }
        return maximum > 500;
    }

    private void updateFrequency(Float frequency) {
        final String text = frequency != null ? String.format("Freq: %f Hz", frequency) : "------";
        Platform.runLater(() -> {
            frequencyLabel.setText(text);
        });
    }

    public void updateSpectrum()  {
        try {
            float sampleRate = 44100.0f;
            int fftSize = 1024; // 1024 temporal samples mean 1024 / 44100 seconds = 23.21 ms
            TargetDataLine line;
            AudioFormat format = //new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    //44100.0f , 16, 1, 2, 44100.0f, false);
                    new AudioFormat(sampleRate, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            int keepSeconds = 2;
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            byte[] buffer = new byte[fftSize * 2];  //16 bits per sample (2 bytes)
            int numRead;
            int checkCount = 0;
            int lastFrequencyIndex = -1;
            while ((numRead = line.read(buffer, 0, buffer.length)) > 0) {
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN);
                if (checkThreshold(byteBuffer, numRead / 2)) {
                    int frequencyIndex = getDominantFrequency(buffer);
                    if (frequencyIndex == lastFrequencyIndex) {
                        checkCount++;
                        if (checkCount >= 40) {
                            updateFrequency((sampleRate * frequencyIndex) / fftSize);
                        }
                    }
                    lastFrequencyIndex = frequencyIndex;
                } else {
                    updateFrequency(null);
                }
            }
        } catch (Exception e) {
            LOGGER.error("In updateSpectrum", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Spectrum Listener");

        VBox root = new VBox();
        root.getChildren().addAll(canvas, frequencyLabel);
        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT + 30));
        primaryStage.show();
        new Thread(() -> updateSpectrum()).start();

    }

}
