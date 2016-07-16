package com.grelobites.dandanator.view;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.util.ImageUtil;
import com.grelobites.dandanator.util.LocaleUtil;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;
import com.grelobites.dandanator.util.romset.RomSetType;
import com.grelobites.dandanator.view.util.DialogUtil;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PreferencesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesController.class);

    private WritableImage backgroundImage;
    private ZxScreen charSetImage;

    private Configuration configuration;

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private Button changeBackgroundImageButton;

    @FXML
    private Button resetBackgroundImageButton;

    @FXML
    private ImageView charSetImageView;

    @FXML
    private Button changeCharSetButton;

    @FXML
    private Button resetCharSetButton;

    @FXML
    private ComboBox<String> romSetModeCombo;

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

    private Configuration getConfiguration() {
        if (configuration == null) {
            return Configuration.getInstance();
        } else {
            return configuration;
        }
    }

    private void initializeImages() throws IOException {
        backgroundImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                new ByteArrayInputStream(getConfiguration().getBackgroundImage()));
        charSetImage = new ZxScreen(256, 64);
        recreateCharSetImage();
    }

    private void recreateCharSetImage() throws IOException {
        charSetImage.setCharSet(getConfiguration().getCharSet());
        charSetImage.setPen(ZxColor.BRIGHTWHITE);
        charSetImage.setInk(ZxColor.BLACK);
        charSetImage.clearScreen();
        charSetImage.printLine("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 3, 0);
        charSetImage.printLine("abcdefghijklmnopqrstuvwxyz", 1, 0);
        charSetImage.printLine("1234567890", 5, 0);
    }

    private void recreateBackgroundImage() throws IOException {
        LOGGER.debug("RecreateBackgroundImage");
        ImageUtil.scrLoader(backgroundImage,
                new ByteArrayInputStream(getConfiguration().getBackgroundImage()));
    }

    private void updateBackgroundImage(File backgroundImageFile) throws IOException {
        if (isReadableFile(backgroundImageFile)) {
            getConfiguration().setBackgroundImagePath(backgroundImageFile.getAbsolutePath());
            recreateBackgroundImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateCharSetPath(File charSetFile) throws IOException {
        if (isReadableFile(charSetFile)) {
            getConfiguration().setCharSetPath(charSetFile.getAbsolutePath());
            recreateCharSetImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }


    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
    }

    private void updateExtraRom(File extraRomFile) {
        if (isReadableFile(extraRomFile)) {
            getConfiguration().setExtraRomPath(extraRomFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROM File provided");
        }

    }

    private boolean isDandanatorRomValid(File dandanatorRomFile) {
        return dandanatorRomFile.canRead() && dandanatorRomFile.isFile()
                && dandanatorRomFile.getTotalSpace() == Constants.BASEROM_SIZE;
    }

    private void updateDandanatorRom(File dandanatorRom) {
        if (isDandanatorRomValid(dandanatorRom)) {
            getConfiguration().setDandanatorRomPath(dandanatorRom.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("Invalid ROM file provided");
        }
    }

    private boolean isDandanatorPicFirmwareValid(File dandanatorPicFile) {
        return dandanatorPicFile.canRead() && dandanatorPicFile.isFile()
                && dandanatorPicFile.getTotalSpace() == Constants.DANDANATOR_PIC_FW_SIZE;
    }

    private void updateDandanatorPicFirmware(File dandanatorPicFirmware) {
        if (isDandanatorPicFirmwareValid(dandanatorPicFirmware)) {
            getConfiguration().setDandanatorPicFirmwarePath(dandanatorPicFirmware.getAbsolutePath());
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

    private void backgroundImageSetup() {
        backgroundImageView.setImage(backgroundImage);
        changeBackgroundImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewBackgroundImage"));
            final File backgroundImageFile = chooser.showOpenDialog(changeBackgroundImageButton
                    .getScene().getWindow());
            if (backgroundImageFile != null) {
                try {
                    updateBackgroundImage(backgroundImageFile);
                } catch (Exception e) {
                    LOGGER.error("Updating background image from " + backgroundImageFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });

        resetBackgroundImageButton.setOnAction(event -> {
            try {
                getConfiguration().setBackgroundImagePath(null);
                recreateBackgroundImage();
            } catch (Exception e) {
                LOGGER.error("Resetting background Image", e);
            }
        });
        getConfiguration().backgroundImagePathProperty().addListener(
            (observable, oldValue, newValue) -> {
                try {
                    recreateBackgroundImage();
                } catch (IOException ioe) {
                    LOGGER.error("Updating background image", ioe);
                }
            });
    }

    private void charSetSetup() {
        charSetImageView.setImage(charSetImage);
        changeCharSetButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewCharSetMessage"));
            final File charSetFile = chooser.showOpenDialog(changeCharSetButton.getScene().getWindow());
            if (charSetFile != null) {
                try {
                    updateCharSetPath(charSetFile);
                } catch (Exception e) {
                    LOGGER.error("Updating charset from " + charSetFile, e);
                    showGenericFileErrorAlert();
                }
            }
        });
        resetCharSetButton.setOnAction(event -> {
            try {
                getConfiguration().setCharSetPath(null);
                recreateCharSetImage();
            } catch (Exception e) {
                LOGGER.error("Resetting charset", e);
            }
        });
        getConfiguration().charSetPathProperty().addListener(
                (observable, oldValue, newValue) -> {
                    try {
                        recreateCharSetImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating charset image", ioe);
                    }
                });

    }

    private void romSetModeSetup() {
        romSetModeCombo.setItems(FXCollections.observableArrayList(
                Stream.of(RomSetType.values()).map(Enum::name)
                        .collect(Collectors.toList())));
        romSetModeCombo.getSelectionModel().select(RomSetType.DANDANATOR_MINI.name());
        romSetModeCombo.onActionProperty().addListener(event ->
                getConfiguration().setMode(romSetModeCombo.getSelectionModel().getSelectedItem()));
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
                        } else if (object.equals(Configuration.ROMSET_PROVIDED)) {
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
        initializeImages();

        backgroundImageSetup();

        charSetSetup();

        romSetModeSetup();

        setupMessageWithResetButton(launchGamesMessage, getConfiguration().launchGameMessageProperty(),
                Constants.LAUNCH_GAME_MESSAGE_MAXLENGTH,
                resetLaunchGamesMessage,
                Constants.DEFAULT_LAUNCHGAME_MESSAGE);

        setupMessageWithResetButton(togglePokesMessage, getConfiguration().togglePokesMessageProperty(),
                Constants.TOGGLE_POKES_MESSAGE_MAXLENGTH,
                resetTogglePokesMessage,
                Constants.DEFAULT_TOGGLEPOKESKEY_MESSAGE);

        setupMessageWithResetButton(selectPokesMessage, getConfiguration().selectPokesMessageProperty(),
                Constants.SELECT_POKE_MESSAGE_MAXLENGTH,
                resetSelectPokesMessage,
                Constants.DEFAULT_SELECTPOKE_MESSAGE);
        setupMessageWithResetButton(extraRomMessage, getConfiguration().extraRomMessageProperty(),
                Constants.EXTRA_ROM_MESSAGE_MAXLENGTH,
                resetExtraRomMessage,
                Constants.DEFAULT_EXTRAROMKEY_MESSAGE);

        setupFileBasedParameter(changeExtraRomButton,
                LocaleUtil.i18n("selectExtraRomMessage"),
                extraRomPath,
                getConfiguration().extraRomPathProperty(),
                resetExtraRomButton,
                this::updateExtraRom);

        setupFileBasedParameter(changeDandanatorMiniRomButton,
                LocaleUtil.i18n("selectDandanatorRomMessage"),
                dandanatorMiniRomPath,
                getConfiguration().dandanatorRomPathProperty(),
                resetDandanatorMiniRomButton,
                this::updateDandanatorRom);

        setupFileBasedParameter(changeDandanatorPicFirmwareButton,
                LocaleUtil.i18n("selectDandanatorPicFirmwareMessage"),
                dandanatorPicFirmwarePath,
                getConfiguration().dandanatorPicFirmwarePathProperty(),
                resetDandanatorPicFirmwareButton,
                this::updateDandanatorPicFirmware);
    }
}
