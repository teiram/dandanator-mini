package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Optional;

public class SerialDataPlayer extends DataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDataPlayer.class);
    private static final String SERVICE_THREAD_NAME = "SerialPortServiceThread";
    private static PlayerConfiguration configuration = PlayerConfiguration.getInstance();
    private static SerialPort sharedSerialPort = new SerialPort(configuration.getSerialPort());
    private static final int SEND_BUFFER_SIZE = 1024;

    private Thread serviceThread;
    private Runnable onFinalization;
    private DoubleProperty progressProperty;
    private byte[] data;
    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }
    private State state = State.STOPPED;

    private void init() {
        progressProperty = new SimpleDoubleProperty(0.0);
        serviceThread = new Thread(null, this::serialSendData, SERVICE_THREAD_NAME);
    }

    public SerialDataPlayer(int block, byte[] data) {
        init();
        setupBlockData(block, data);
    }

    private void setupBlockData(int block, byte[] buffer) {
        int blockSize = configuration.getBlockSize();
        data = new byte[blockSize + 3];
        System.arraycopy(buffer, 0, data, 0, blockSize);

        data[blockSize] = Integer.valueOf(block + 1).byteValue();

        Util.writeAsLittleEndian(data, blockSize + 1, Util.getBlockCrc16(data, blockSize + 1));
    }

    private void serialSendData() {
        try {
            if (!sharedSerialPort.isOpened()) {
                sharedSerialPort.openPort();
                sharedSerialPort.setParams(configuration.getSerialSpeed(), SerialPort.DATABITS_8, SerialPort.STOPBITS_2,
                        SerialPort.PARITY_NONE);
                //Give time to some crappy serial ports to stabilize
                Thread.sleep(2000);
            }

            int sent = 0;
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            byte[] sendBuffer = new byte[SEND_BUFFER_SIZE];
            while (sent < data.length) {
                int count = bis.read(sendBuffer);
                LOGGER.debug("Sending block of " + count + " bytes");
                if (count < SEND_BUFFER_SIZE) {
                    sharedSerialPort.writeBytes(Arrays.copyOfRange(sendBuffer, 0, count));
                } else {
                    sharedSerialPort.writeBytes(sendBuffer);
                }
                sent += count;
                final double progress = 1.0 * sent / data.length;
                Platform.runLater(() -> progressProperty.set(progress));
                if (state != State.RUNNING) {
                    LOGGER.debug("No more in running state");
                    break;
                }
            }

            if (state == State.RUNNING && onFinalization != null) {
                Platform.runLater(onFinalization);
            }
            state = State.STOPPED;
            LOGGER.debug("State is now STOPPED");
        } catch (Exception e) {
            LOGGER.error("Exception during send process", e);
            state = State.STOPPED;
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
                    LOGGER.debug("Serial thread was interrupted", e);
                }
            }
        }
        //Try to close the port anyway
        try {
            if (sharedSerialPort.isOpened()) {
                LOGGER.debug("Closing serial port");
                sharedSerialPort.closePort();
            }
        } catch (Exception e) {
            LOGGER.warn("Closing serial port", e);
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
