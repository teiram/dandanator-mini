package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import jssc.SerialPort;
import jssc.SerialPortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SerialDataPlayer extends DataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialDataPlayer.class);

    private static PlayerConfiguration configuration = PlayerConfiguration.getInstance();

    private Thread serviceThread;
    private SerialPort serialPort;
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
        serialPort =  new SerialPort(configuration.getSerialPort());
        serviceThread = new Thread(() -> {
            serialSendData();
        });
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

        Util.writeAsLittleEndian(data, blockSize + 1, getBlockCrc(data, blockSize + 1));
    }

    private void serialSendData() {
        try {
            serialPort.openPort();
            serialPort.setParams(SerialPort.BAUDRATE_38400, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);


            int counter = 0;
            for (byte value : data) {
                serialPort.writeByte(value);
                if (++counter % 25 == 0) {
                    final double progress = 1.0 * counter / data.length;
                    Platform.runLater(() -> {
                        progressProperty.set(progress);
                    });
                }
                if (state != State.RUNNING) {
                    break;
                }
            }
            state = State.STOPPED;
        } catch (SerialPortException e) {
            LOGGER.error("Serial port exception", e);
        } finally {
            try {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                }
            } catch (SerialPortException e) {
                LOGGER.error("Closing port", e);
            }
            if (onFinalization != null) {
                onFinalization.run();
            }
        }

    }

    @Override
    public void send() {
        state = State.RUNNING;
        serviceThread.start();
    }

    @Override
    public void stop() {
        state = State.STOPPING;
        while (state != State.STOPPED) {
            try {
                serviceThread.join();
            } catch (InterruptedException e) {
                LOGGER.debug("Serial thread was interrupted" , e);
            }
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
