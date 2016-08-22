package com.grelobites.romgenerator.handlers.dandanatormini.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;


public class DandanatorMiniPreferencesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniPreferencesController.class);

    @FXML
    private TextField launchGamesMessage;

    @FXML
    private Button resetLaunchGamesMessage;

    @FXML
    private TextField togglePokesMessage;

    @FXML
    private Button resetTogglePokesMessage;

    @FXML
    private TextField selectPokesMessage;

    @FXML
    private Button resetSelectPokesMessage;

    @FXML
    private TextField extraRomMessage;

    @FXML
    private Button resetExtraRomMessage;

    @FXML
    private Label extraRomPath;

    @FXML
    private Button changeExtraRomButton;

    @FXML
    private Button resetExtraRomButton;

    @FXML
    private Label dandanatorMiniRomPath;

    @FXML
    private Button changeDandanatorMiniRomButton;

    @FXML
    private Button resetDandanatorMiniRomButton;

    @FXML
    private Label dandanatorPicFirmwarePath;

    @FXML
    private Button changeDandanatorPicFirmwareButton;

    @FXML
    private Button resetDandanatorPicFirmwareButton;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateExtraRom(File extraRomFile) {
        if (isReadableFile(extraRomFile)) {
            DandanatorMiniConfiguration.getInstance().setExtraRomPath(extraRomFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROM File provided");
        }

    }

    private boolean isDandanatorRomValid(File dandanatorRomFile) {
        return dandanatorRomFile.canRead() && dandanatorRomFile.isFile()
                && dandanatorRomFile.length() == DandanatorMiniConstants.BASEROM_SIZE;
    }

    private void updateDandanatorRom(File dandanatorRom) {
        if (isDandanatorRomValid(dandanatorRom)) {
            DandanatorMiniConfiguration.getInstance().setDandanatorRomPath(dandanatorRom.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROM file provided");
        }
    }

    private boolean isDandanatorPicFirmwareValid(File dandanatorPicFile) {
        return dandanatorPicFile.canRead() && dandanatorPicFile.isFile()
                && dandanatorPicFile.length() == DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE;
    }

    private void updateDandanatorPicFirmware(File dandanatorPicFirmware) {
        if (isDandanatorPicFirmwareValid(dandanatorPicFirmware)) {
            DandanatorMiniConfiguration.getInstance().setDandanatorPicFirmwarePath(dandanatorPicFirmware.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid PIC Firmware file provided");
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
        setupMessageWithResetButton(launchGamesMessage, DandanatorMiniConfiguration.getInstance()
                        .launchGameMessageProperty(),
                DandanatorMiniConstants.LAUNCH_GAME_MESSAGE_MAXLENGTH,
                resetLaunchGamesMessage,
                DandanatorMiniConstants.DEFAULT_LAUNCHGAME_MESSAGE);

        setupMessageWithResetButton(togglePokesMessage, DandanatorMiniConfiguration.getInstance()
                        .togglePokesMessageProperty(),
                DandanatorMiniConstants.TOGGLE_POKES_MESSAGE_MAXLENGTH,
                resetTogglePokesMessage,
                DandanatorMiniConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE);

        setupMessageWithResetButton(selectPokesMessage, DandanatorMiniConfiguration.getInstance()
                        .selectPokesMessageProperty(),
                DandanatorMiniConstants.SELECT_POKE_MESSAGE_MAXLENGTH,
                resetSelectPokesMessage,
                DandanatorMiniConstants.DEFAULT_SELECTPOKE_MESSAGE);
        setupMessageWithResetButton(extraRomMessage, DandanatorMiniConfiguration.getInstance()
                        .extraRomMessageProperty(),
                DandanatorMiniConstants.EXTRA_ROM_MESSAGE_MAXLENGTH,
                resetExtraRomMessage,
                DandanatorMiniConstants.DEFAULT_EXTRAROMKEY_MESSAGE);

        setupFileBasedParameter(changeExtraRomButton,
                LocaleUtil.i18n("selectExtraRomMessage"),
                extraRomPath,
                DandanatorMiniConfiguration.getInstance().extraRomPathProperty(),
                resetExtraRomButton,
                this::updateExtraRom);

        setupFileBasedParameter(changeDandanatorMiniRomButton,
                LocaleUtil.i18n("selectDandanatorRomMessage"),
                dandanatorMiniRomPath,
                DandanatorMiniConfiguration.getInstance().dandanatorRomPathProperty(),
                resetDandanatorMiniRomButton,
                this::updateDandanatorRom);

        setupFileBasedParameter(changeDandanatorPicFirmwareButton,
                LocaleUtil.i18n("selectDandanatorPicFirmwareMessage"),
                dandanatorPicFirmwarePath,
                DandanatorMiniConfiguration.getInstance().dandanatorPicFirmwarePathProperty(),
                resetDandanatorPicFirmwareButton,
                this::updateDandanatorPicFirmware);
    }
}
