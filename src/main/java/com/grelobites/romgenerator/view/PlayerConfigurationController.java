package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.player.EncodingSpeed;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class PlayerConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerConfigurationController.class);

    @FXML
    private ComboBox<String> audioMode;

    @FXML
    private ComboBox<EncodingSpeed> encodingSpeed;

    @FXML
    private CheckBox useTargetFeedback;

    @FXML
    private ComboBox<String> serialPort;

    @FXML
    private CheckBox useSerialPort;

    @FXML
    private Button refreshSerialPorts;

    @FXML
    private Label customRomSetPath;

    @FXML
    private Button changeCustomRomSetPathButton;

    @FXML
    private Button resetCustomRomSetPathButton;

    @FXML
    private CheckBox reversePhase;

    @FXML
    private CheckBox boostLevel;

    @FXML
    private CheckBox sendLoader;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateCustomRomSetPath(File romsetFile) {
        if (isReadableFile(romsetFile) && romsetFile.length() == 32 * Constants.SLOT_SIZE) {
            PlayerConfiguration.getInstance().setCustomRomSetPath(romsetFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROMSet file provided");
        }
    }

    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private void setupFileBasedParameter(Button changeButton,
                                         String changeMessage,
                                         Label pathLabel,
                                         StringProperty configurationProperty,
                                         Button resetButton,
                                         String defaultMessage,
                                         Consumer<File> consumer) {
        pathLabel.textProperty().bindBidirectional(configurationProperty,
                new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        LOGGER.debug("Executing toString on " + object);
                        if (object == null) {
                            return defaultMessage;
                        } else if (object.equals(Constants.ROMSET_PROVIDED)) {
                            return LocaleUtil.i18n("romsetProvidedMessage");
                        } else {
                            return object;
                        }
                    }

                    @Override
                    public String fromString(String string) {
                        LOGGER.debug("Executing fromString on " + string);
                        if (string == null) {
                            return null;
                        } else if (string.isEmpty()) {
                            return null;
                        } else {
                            return string;
                        }
                    }
                });

        changeButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(changeMessage);
            final File file = chooser.showOpenDialog(changeButton.getScene().getWindow());
            if (file != null) {
                try {
                    consumer.accept(file);
                    pathLabel.setText(file.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("In setupFileBasedParameter for " + changeMessage
                                    + " with file " + file, e);
                    showGenericFileErrorAlert();
                }
            }
        });

        resetButton.setOnAction(event ->
            configurationProperty.set(null));

    }
    @FXML
    private void initialize() throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();

        setupFileBasedParameter(changeCustomRomSetPathButton,
                LocaleUtil.i18n("useCustomRomSet"),
                customRomSetPath,
                configuration.customRomSetPathProperty(),
                resetCustomRomSetPathButton,
                LocaleUtil.i18n("none"),
                this::updateCustomRomSetPath);

        encodingSpeed.setItems(FXCollections.observableArrayList(EncodingSpeed.values()));
        encodingSpeed.getSelectionModel().select(EncodingSpeed.of(configuration.getEncodingSpeed()));
        encodingSpeed.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> configuration.setEncodingSpeed(newValue.speed()));

        useTargetFeedback.selectedProperty().bindBidirectional(
                configuration.useTargetFeedbackProperty());

        audioMode.setItems(FXCollections.observableArrayList(PlayerConfiguration.AUDIO_MODES));
        audioMode.getSelectionModel().select(configuration.getAudioMode());
        audioMode.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) ->
                        configuration.setAudioMode(newValue));

        sendLoader.selectedProperty().bindBidirectional(
                configuration.sendLoaderProperty());
        useSerialPort.selectedProperty().bindBidirectional(configuration.useSerialPortProperty());
        serialPort.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    configuration.setSerialPort(newValue);
                    if (newValue == null) {
                        useSerialPort.setSelected(false);
                    }
                    useSerialPort.setDisable(newValue == null);
                });

        useSerialPort.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                useTargetFeedback.setSelected(false);
            } else {
                sendLoader.setSelected(true);
            }
            useTargetFeedback.setDisable(newValue);
            encodingSpeed.setDisable(newValue);
            reversePhase.setDisable(newValue);
            boostLevel.setDisable(newValue);
            sendLoader.setDisable(!newValue);
        });

        sendLoader.selectedProperty().addListener((observable, oldValue, newValue) ->
                audioMode.setDisable(!newValue));

        refreshSerialPorts.setOnAction(e -> {
            serialPort.getSelectionModel().clearSelection();
            serialPort.getItems().clear();
            serialPort.getItems().addAll(SerialPortList.getPortNames());
        });

        reversePhase.selectedProperty().bindBidirectional(configuration.reversePhaseProperty());
        boostLevel.selectedProperty().bindBidirectional(configuration.boostLevelProperty());

        ObservableList<String> serialPortNames = FXCollections.observableArrayList(SerialPortList.getPortNames());
        serialPort.setItems(serialPortNames);
        if (serialPortNames.contains(configuration.getSerialPort())) {
            serialPort.getSelectionModel().select(configuration.getSerialPort());
        } else {
            serialPort.getSelectionModel().clearSelection();
            configuration.setSerialPort(null);
            configuration.setUseSerialPort(false);
        }

        useSerialPort.setDisable(serialPort.getSelectionModel().getSelectedItem() == null);
        sendLoader.setDisable(useSerialPort.isDisable());
    }
}
