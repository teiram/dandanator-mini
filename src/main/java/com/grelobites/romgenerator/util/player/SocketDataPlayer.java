package com.grelobites.romgenerator.util.player;

import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.Util;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Optional;

public class SocketDataPlayer extends DataPlayerSupport implements DataPlayer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketDataPlayer.class);
    private static final String SERVICE_THREAD_NAME = "SocketDataServiceThread";
    private static PlayerConfiguration configuration = PlayerConfiguration.getInstance();
    private static final int SEND_BUFFER_SIZE = 2048;

    private Thread serviceThread;
    private Socket socket;
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
        serviceThread = new Thread(null, this::socketSendData, SERVICE_THREAD_NAME);
    }

    public SocketDataPlayer(int block, byte[] data) {
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

    private void waitForStart(Socket socket) {
        try {
            InputStream is = socket.getInputStream();
            int b = is.read();
            if (b < 0) {
                LOGGER.error("Socket stream is closed");
            } else {
                LOGGER.debug("Got message from socket: {}", b);
            }
        } catch (Exception e) {
            LOGGER.error("Trying to read from socket", e);
        }
    }

    private void socketSendData() {
        try {
            LOGGER.debug("Connecting to {}:{}",
                    configuration.getSocketHostname(),
                    configuration.getSocketPort()
                    );
            socket = new Socket(configuration.getSocketHostname(),
                    configuration.getSocketPort());
            socket.setSoTimeout(10000);
            waitForStart(socket);
            OutputStream output = socket.getOutputStream();
            int sent = 0;
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            byte[] sendBuffer = new byte[SEND_BUFFER_SIZE];
            while (sent < data.length) {
                int count = bis.read(sendBuffer);
                LOGGER.debug("Sending block of " + count + " bytes");
                if (count < SEND_BUFFER_SIZE) {
                    output.write(Arrays.copyOfRange(sendBuffer, 0, count));
                } else {
                    output.write(sendBuffer);
                }
                sent += count;
                final double progress = 1.0 * sent / data.length;
                Platform.runLater(() -> progressProperty.set(progress));
                if (state != State.RUNNING) {
                    LOGGER.debug("No more in running state");
                    break;
                }
                Thread.sleep(100);
            }

            if (state == State.RUNNING && onFinalization != null) {
                Platform.runLater(onFinalization);
            }
            state = State.STOPPED;
            LOGGER.debug("State is now STOPPED");
        } catch (Exception e) {
            LOGGER.error("Exception during send process", e);
            state = State.STOPPED;
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                LOGGER.error("Closing socket", e);
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
        if (state == State.RUNNING) {
            state = State.STOPPING;
            LOGGER.debug("State changed to STOPPING");

            while (state != State.STOPPED) {
                try {
                    serviceThread.join();
                } catch (InterruptedException e) {
                    LOGGER.debug("Socket thread was interrupted", e);
                }
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
