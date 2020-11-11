package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.multiply.ArduinoConstants;
import com.grelobites.romgenerator.util.multiply.Binary;
import com.grelobites.romgenerator.util.multiply.HexUtil;
import com.grelobites.romgenerator.util.multiply.Stk500Programmer;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class MultiplyUpgradeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiplyUpgradeController.class);

    private enum SerialPortConfiguration {

        OLD_BOOTLOADER(SerialPort.BAUDRATE_57600,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE),
        NEW_BOOTLOADER(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        public int baudrate;
        public int dataBits;
        public int stopBits;
        public int parity;

        SerialPortConfiguration(int baudrate, int dataBits, int stopBits, int parity) {
            this.baudrate = baudrate;
            this.dataBits = dataBits;
            this.stopBits = stopBits;
            this.parity = parity;
        }

        @Override
        public String toString() {
            return "SerialPortConfiguration{" +
                    "baudrate=" + baudrate +
                    ", dataBits=" + dataBits +
                    ", stopBits=" + stopBits +
                    ", parity=" + parity +
                    '}';
        }
    }

    private static class TargetInfo {
        public final Image image;
        public final ArduinoConstants.ArduinoTarget target;
        public TargetInfo(Image image, ArduinoConstants.ArduinoTarget target) {
            this.image = image;
            this.target = target;
        }
    }
    private ApplicationContext applicationContext;

    @FXML
    private VBox multiplyUpdaterPane;

    @FXML
    private Circle multiplyDetectedLed;

    @FXML
    private Circle multiplyValidatedLed;

    @FXML
    private Circle multiplyUpdatedLed;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Button programButton;

    @FXML
    private ComboBox<String> serialPortList;

    @FXML
    private Button reloadPorts;

    @FXML
    private ImageView scenarioImage;

    private TargetInfo[] targets = new TargetInfo[2];

    private final static int MULTIPLY_TARGET = 0;
    private final static int DANDANATOR_V3_TARGET = 1;

    private int targetIndex = MULTIPLY_TARGET;

    private BooleanProperty programming;

    private DoubleProperty progress;

    private Animation currentLedAnimation;

    private SerialPort serialPort;
    private Stk500Programmer arduinoProgrammer;

    public MultiplyUpgradeController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.programming = new SimpleBooleanProperty(false);
        this.progress = new SimpleDoubleProperty(0.0);
    }

    public Animation createLedAnimation(Circle led) {
        FadeTransition ft = new FadeTransition(Duration.millis(1000), led);
        ft.setFromValue(1.0);
        ft.setToValue(0.0);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(false);
        return ft;
    }

    private void setLedOKStatus(Circle led) {
        led.setFill(Color.GREEN);
        led.setOpacity(1.0);
    }

    private void setLedWorkingStatus(Circle led) {
        led.setFill(Color.WHITE);
    }

    private void setLedErrorStatus(Circle led) {
        led.setFill(Color.RED);
        led.setOpacity(1.0);
    }

    private void resetLedStatus(Circle... leds) {
        for (Circle led: leds) {
            led.setFill(Color.DARKGREY);
            led.setOpacity(1.0);
        }
    }

    public void onProgrammingEnd() {
        if (serialPort != null) {
            try {
                serialPort.closePort();
            } catch (Exception e) {}
        }
        Platform.runLater(() -> {
            programming.set(false);
        });
    }

    public void onProgrammingStart() {
        Platform.runLater(() -> {
            programming.set(true);
            resetView();
        });
    }

    private void onStartOperation(Circle led) {
        Platform.runLater(() -> {
            setLedWorkingStatus(led);
            currentLedAnimation = createLedAnimation(led);
            currentLedAnimation.play();
        });
    }

    private void onFailedOperation(Circle led) {
        Platform.runLater(() -> {
            currentLedAnimation.stop();
            setLedErrorStatus(led);
        });
    }

    private void onSuccessfulOperation(Circle led, double currentProgress) {
        Platform.runLater(() -> {
            currentLedAnimation.stop();
            setLedOKStatus(led);
            progress.set(currentProgress);
        });
    }

    public void resetView() {
        progress.set(0.0);
        resetLedStatus(multiplyDetectedLed, multiplyValidatedLed,
                multiplyUpdatedLed);
    }

    private static void sync(SerialPort serialPort, Stk500Programmer programmer) {
        for (SerialPortConfiguration spc : SerialPortConfiguration.values()) {
            try {
                LOGGER.debug("Trying to sync with serial configuration {}", spc);
                serialPort.setParams(spc.baudrate, spc.dataBits, spc.stopBits, spc.parity);
                programmer.initialize(250, 50);
                programmer.sync();
                return;
            } catch (Exception e) {
                LOGGER.info("Unable to sync with serial port configuration {}", spc, e);
            }
        }
        throw new RuntimeException("Unable to sync with arduino");
    }

    @FXML
    void initialize() throws IOException {
        targets[MULTIPLY_TARGET] = new TargetInfo(
                new Image(MultiplyUpgradeController.class
                        .getResourceAsStream("/multiply/multiply-update.png")),
                ArduinoConstants.ArduinoTarget.MULTIPLY);
        targets[DANDANATOR_V3_TARGET] = new TargetInfo(
                new Image(MultiplyUpgradeController.class
                        .getResourceAsStream("/multiply/dandanator-v3-update.png")),
                ArduinoConstants.ArduinoTarget.DANDANATOR_V3);

        targetIndex = MULTIPLY_TARGET;

        multiplyUpdaterPane.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.isAltDown() && e.getCode() == KeyCode.DIGIT3) {
                targetIndex = (targetIndex + 1) % targets.length;
                scenarioImage.setImage(targets[targetIndex].image);
                LOGGER.debug("Target is {}", targets[targetIndex].target);
            }
        });

        programButton.disableProperty().bind(programming.or(
                serialPortList.valueProperty().isNull()));
        progressBar.progressProperty().bind(progress);

        reloadPorts.setOnAction(e -> {
            serialPortList.getSelectionModel().clearSelection();
            serialPortList.getItems().clear();
            serialPortList.getItems().addAll(Util.getSerialPortNames());
        });

        serialPortList.getItems().addAll(Util.getSerialPortNames());

        programButton.setOnAction(c -> {

            applicationContext.addBackgroundTask(() -> {
                onProgrammingStart();
                try {
                    try {
                        LOGGER.debug("Starting multiply detection");
                        onStartOperation(multiplyDetectedLed);
                        serialPort = new SerialPort(serialPortList
                                .getSelectionModel().getSelectedItem());
                        arduinoProgrammer = new Stk500Programmer(serialPort);
                        serialPort.openPort();
                        sync(serialPort, arduinoProgrammer);
                        onSuccessfulOperation(multiplyDetectedLed, 0.10);
                    } catch (Exception e) {
                        LOGGER.error("Unable to sync with multiply device");
                        onFailedOperation(multiplyDetectedLed);
                        throw e;
                    }

                    try {
                        LOGGER.debug("Starting multiply validation");
                        onStartOperation(multiplyValidatedLed);

                        byte[] signature = arduinoProgrammer.getDeviceSignature();
                        if (!arduinoProgrammer.supportedSignature(signature)) {
                            LOGGER.warn("Arduino model with signature {} not supported",
                                    Util.dumpAsHexString(signature));
                            throw new IllegalArgumentException("Unsupported Arduino model");
                        }
                        arduinoProgrammer.enterProgramMode();
                        onSuccessfulOperation(multiplyValidatedLed, 0.20);
                    } catch (Exception e) {
                        LOGGER.error("During multiply validation", e);
                        onFailedOperation(multiplyValidatedLed);
                        throw e;
                    }

                    try {
                        LOGGER.debug("Starting multiply update");
                        onStartOperation(multiplyUpdatedLed);
                        List<Binary> binaries = HexUtil.toBinaryList(
                                ArduinoConstants.hexResource(targets[targetIndex].target));
                        LOGGER.debug("Got a list of {} binaries to upload", binaries.size());
                        for (Binary binary : binaries) {
                            arduinoProgrammer.programBinary(binary, (d) -> progress.set(0.20 + 0.50 * d),
                                    true, true);
                        }
                        onSuccessfulOperation(multiplyUpdatedLed, 1.0);
                    } catch (Exception e) {
                        LOGGER.error("During multiply update", e);
                        onFailedOperation(multiplyUpdatedLed);
                        throw e;
                    } finally {
                        arduinoProgrammer.leaveProgramMode();
                    }

                    return OperationResult.successResult();
                } catch (Exception e) {
                    LOGGER.error("During programming operation", e);
                    return OperationResult.successResult();
                } finally {
                    onProgrammingEnd();
                }
            });
        });
    }
}
