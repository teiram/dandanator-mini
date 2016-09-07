package com.grelobites.romgenerator.handlers.dandanatormini.view;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.IfromConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.IfromConstants;
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


public class IfromPreferencesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(IfromPreferencesController.class);

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
    private TextField customRomLaunchMessage;

    @FXML
    private Button resetCustomRomLaunchMessage;

    @FXML
    private Label customRomPath;

    @FXML
    private Button changeCustomRomButton;

    @FXML
    private Button resetCustomRomButton;

    @FXML
    private Label baseRomPath;

    @FXML
    private Button changeBaseRomButton;

    @FXML
    private Button resetBaseRomButton;

    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateCustomRom(File customRomFile) {
        if (isReadableFile(customRomFile)) {
            IfromConfiguration.getInstance().setCustomRomPath(customRomFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid Custom Rom file provided");
        }

    }

    private boolean isBaseRomValid(File baseRomFile) {
        return baseRomFile.canRead() && baseRomFile.isFile()
                && baseRomFile.length() == IfromConstants.BASEROM_SIZE;
    }

    private void updateBaseRom(File baseRom) {
        if (isBaseRomValid(baseRom)) {
            IfromConfiguration.getInstance().setBaseRomPath(baseRom.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid Base Rom file provided");
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
        setupMessageWithResetButton(launchGamesMessage, IfromConfiguration.getInstance()
                        .launchGameMessageProperty(),
                IfromConstants.LAUNCH_GAME_MESSAGE_MAXLENGTH,
                resetLaunchGamesMessage,
                IfromConstants.DEFAULT_LAUNCHGAME_MESSAGE);

        setupMessageWithResetButton(togglePokesMessage, IfromConfiguration.getInstance()
                        .togglePokesMessageProperty(),
                IfromConstants.TOGGLE_POKES_MESSAGE_MAXLENGTH,
                resetTogglePokesMessage,
                IfromConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE);

        setupMessageWithResetButton(selectPokesMessage, IfromConfiguration.getInstance()
                        .selectPokesMessageProperty(),
                IfromConstants.SELECT_POKE_MESSAGE_MAXLENGTH,
                resetSelectPokesMessage,
                IfromConstants.DEFAULT_SELECTPOKE_MESSAGE);

        setupMessageWithResetButton(customRomLaunchMessage, IfromConfiguration.getInstance()
                        .launchCustomRomMessageProperty(),
                IfromConstants.EXTRA_ROM_MESSAGE_MAXLENGTH,
                resetCustomRomLaunchMessage,
                IfromConstants.DEFAULT_CUSTOMROMKEY_MESSAGE);

        setupFileBasedParameter(changeCustomRomButton,
                LocaleUtil.i18n("selectExtraRomMessage"),
                customRomPath,
                IfromConfiguration.getInstance().customRomPathProperty(),
                resetCustomRomButton,
                this::updateCustomRom);

        setupFileBasedParameter(changeBaseRomButton,
                LocaleUtil.i18n("selectDandanatorRomMessage"),
                baseRomPath,
                IfromConfiguration.getInstance().customRomPathProperty(),
                resetBaseRomButton,
                this::updateBaseRom);
    }
}
