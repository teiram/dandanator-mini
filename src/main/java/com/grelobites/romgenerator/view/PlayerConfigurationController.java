package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.util.converter.NumberStringConverter;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class PlayerConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerConfigurationController.class);

    @FXML
    private Label loaderPath;

    @FXML
    private Button changeLoaderPathButton;

    @FXML
    private Button resetLoaderPathButton;

    @FXML
    private TextField blockSize;

    @FXML
    private ComboBox<String> audioMode;

    @FXML
    private TextField encodingSpeed;

    @FXML
    private TextField pilotLength;

    @FXML
    private TextField trailLength;

    @FXML
    private TextField recordingPause;

    @FXML
    private CheckBox useTargetFeedback;

    @FXML
    private ComboBox<String> serialPort;

    @FXML
    private CheckBox useSerialPort;

    @FXML
    private Button refreshSerialPorts;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateLoaderPath(File loaderFile) {
        if (isReadableFile(loaderFile)) {
            PlayerConfiguration.getInstance().setLoaderPath(loaderFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid Loader File provided");
        }

    }

    private static void showGenericFileErrorAlert() {
        DialogUtil.buildErrorAlert(LocaleUtil.i18n("fileImportError"),
                LocaleUtil.i18n("fileImportErrorHeader"),
                LocaleUtil.i18n("fileImportErrorContent"))
                .showAndWait();
    }

    private static void bindLabelToConfiguration(TextField textField,
                                                 StringProperty stringProperty,
                                                 int maxMessageLength) {
        textField.textProperty().bindBidirectional(stringProperty);
        textField.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null &&
                            newValue.length() > maxMessageLength) {
                        textField.setText(oldValue);
                    }
                });
    }

    private void setupMessageWithResetButton(TextField textField,
                                             StringProperty stringProperty,
                                             int maxMessageLength,
                                             Button resetButton,
                                             String defaultMessage) {
        bindLabelToConfiguration(textField, stringProperty, maxMessageLength);
        if (stringProperty.get() == null) {
            stringProperty.set(defaultMessage);
        }
        resetButton.setOnAction(event -> stringProperty.set(defaultMessage));

    }
    private void setupFileBasedParameter(Button changeButton,
                                         String changeMessage,
                                         Label pathLabel,
                                         StringProperty configurationProperty,
                                         Button resetButton,
                                         Consumer<File> consumer) {
        pathLabel.textProperty().bindBidirectional(configurationProperty,
                new StringConverter<String>() {
                    @Override
                    public String toString(String object) {
                        LOGGER.debug("Executing toString on " + object);
                        if (object == null) {
                            return LocaleUtil.i18n("builtInMessage");
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
        setupFileBasedParameter(changeLoaderPathButton,
                "Cargador",
                loaderPath,
                PlayerConfiguration.getInstance().loaderPathProperty(),
                resetLoaderPathButton,
                this::updateLoaderPath);

        Bindings.bindBidirectional(blockSize.textProperty(),
                PlayerConfiguration.getInstance().blockSizeProperty(),
                new NumberStringConverter());
        Bindings.bindBidirectional(encodingSpeed.textProperty(),
                PlayerConfiguration.getInstance().encodingSpeedProperty(),
                new NumberStringConverter());
        Bindings.bindBidirectional(pilotLength.textProperty(),
                PlayerConfiguration.getInstance().pilotLengthProperty(),
                new NumberStringConverter());
        Bindings.bindBidirectional(trailLength.textProperty(),
                PlayerConfiguration.getInstance().trailLengthProperty(),
                new NumberStringConverter());
        Bindings.bindBidirectional(recordingPause.textProperty(),
                PlayerConfiguration.getInstance().recordingPauseProperty(),
                new NumberStringConverter());

        useTargetFeedback.selectedProperty().bindBidirectional(
                PlayerConfiguration.getInstance().useTargetFeedbackProperty());

        audioMode.setItems(FXCollections.observableArrayList(
                "MONO", "STEREO", "STEREOINV"));
        audioMode.getSelectionModel().select(PlayerConfiguration.getInstance()
                .getAudioMode());
        audioMode.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) ->
                        PlayerConfiguration.getInstance().setAudioMode(newValue));

        serialPort.setItems(FXCollections.observableArrayList(SerialPortList.getPortNames()));
        serialPort.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    PlayerConfiguration.getInstance().setSerialPort(newValue);
                    if (newValue == null) {
                        useSerialPort.setSelected(false);
                    }
                    useSerialPort.setDisable(newValue == null);
                });
        useSerialPort.setDisable(true);
        useSerialPort.setSelected(false);
        useSerialPort.selectedProperty().bindBidirectional(
                PlayerConfiguration.getInstance().useSerialPortProperty());
        //useTargetFeedback.disableProperty().bind(useSerialPort.selectedProperty());
        refreshSerialPorts.setOnAction(e -> {
            serialPort.getSelectionModel().clearSelection();
            serialPort.getItems().clear();
            serialPort.getItems().addAll(SerialPortList.getPortNames());
        });
    }
}
