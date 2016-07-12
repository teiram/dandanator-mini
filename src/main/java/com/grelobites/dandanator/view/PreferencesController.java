package com.grelobites.dandanator.view;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.util.ImageUtil;
import com.grelobites.dandanator.util.LocaleUtil;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;
import com.grelobites.dandanator.util.romset.RomSetType;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
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
        ImageUtil.scrLoader(backgroundImage,
                new ByteArrayInputStream(getConfiguration().getBackgroundImage()));
    }

    private void updateBackgroundImage(File backgroundImageFile) throws IOException {
        if (backgroundImageFile.canRead() && backgroundImageFile.isFile()) {
            getConfiguration().setBackgroundImagePath(backgroundImageFile.getAbsolutePath());
            recreateBackgroundImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateCharSetPath(File charSetFile) throws IOException {
        if (charSetFile.canRead() && charSetFile.isFile()) {
            getConfiguration().setCharSetPath(charSetFile.getAbsolutePath());
            recreateCharSetImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateExtraRom(File extraRomFile) {
        if (extraRomFile.canRead() && extraRomFile.isFile()) {
            getConfiguration().setExtraRomPath(extraRomFile.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateDandanatorRom(File dandanatorRom) {
        if (dandanatorRom.canRead() && dandanatorRom.isFile()) {
            getConfiguration().setDandanatorRomPath(dandanatorRom.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateDandanatorPicFirmware(File dandanatorPicFirmware) {
        if (dandanatorPicFirmware.canRead() && dandanatorPicFirmware.isFile()) {
            getConfiguration().setDandanatorPicFirmwarePath(dandanatorPicFirmware.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    @FXML
    private void initialize() throws IOException {
        initializeImages();
        backgroundImageView.setImage(backgroundImage);
        changeBackgroundImageButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewBackgroundImage"));
            final File backgroundImageFile = chooser.showOpenDialog(changeBackgroundImageButton
                    .getScene().getWindow());
            try {
                updateBackgroundImage(backgroundImageFile);
            } catch (Exception e) {
                LOGGER.error("Updating background image from " +  backgroundImageFile, e);
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

        charSetImageView.setImage(charSetImage);
        changeCharSetButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectNewCharSetMessage"));
            final File charSetFile = chooser.showOpenDialog(changeCharSetButton.getScene().getWindow());
            try {
                updateCharSetPath(charSetFile);
            } catch (Exception e) {
                LOGGER.error("Updating charset from " +  charSetFile, e);
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

        romSetModeCombo.setItems(FXCollections.observableArrayList(
              Stream.of(RomSetType.values()).map(Enum::name)
                        .collect(Collectors.toList())));
        romSetModeCombo.getSelectionModel().select(RomSetType.DANDANATOR_MINI.name());
        romSetModeCombo.onActionProperty().addListener(event -> {
            getConfiguration().setMode(romSetModeCombo.getSelectionModel().getSelectedItem());
        });

        launchGamesMessage.setText(getConfiguration().getLaunchGameMessage());
        launchGamesMessage.textProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue.length() > Constants.LAUNCH_GAME_MESSAGE_MAXLENGTH) {
                        launchGamesMessage.setText(oldValue);
                    } else {
                        getConfiguration().setLaunchGameMessage(newValue);
                    }
                });

        resetLaunchGamesMessage.setOnAction(event -> {
            getConfiguration().setLaunchGameMessage(null);
            launchGamesMessage.setText(getConfiguration().getLaunchGameMessage());
        });


        togglePokesMessage.setText(getConfiguration().getTogglePokesMessage());
        togglePokesMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > Constants.TOGGLE_POKES_MESSAGE_MAXLENGTH) {
                togglePokesMessage.setText(oldValue);
            } else {
                getConfiguration().setTogglePokesMessage(newValue);
            }
        });
        resetTogglePokesMessage.setOnAction(event -> {
            getConfiguration().setTogglePokesMessage(null);
            togglePokesMessage.setText(getConfiguration().getTogglePokesMessage());
        });


        selectPokesMessage.setText(getConfiguration().getSelectPokesMessage());
        selectPokesMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > Constants.SELECT_POKE_MESSAGE_MAXLENGTH) {
                selectPokesMessage.setText(oldValue);
            } else {
                getConfiguration().setSelectPokesMessage(newValue);
            }
        });
        resetSelectPokesMessage.setOnAction(event -> {
            getConfiguration().setSelectPokesMessage(null);
            selectPokesMessage.setText(getConfiguration().getSelectPokesMessage());
        });

        extraRomMessage.setText(getConfiguration().getExtraRomMessage());
        extraRomMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > Constants.EXTRA_ROM_MESSAGE_MAXLENGTH) {
                extraRomMessage.setText(oldValue);
            } else {
                getConfiguration().setExtraRomMessage(newValue);
            }
        });
        resetExtraRomMessage.setOnAction(event -> {
            getConfiguration().setExtraRomMessage(null);
            extraRomMessage.setText(getConfiguration().getExtraRomMessage());
        });

        changeExtraRomButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectExtraRomMessage"));
            final File extraRomFile = chooser.showOpenDialog(changeExtraRomButton.getScene().getWindow());
            if (extraRomFile != null) {
                try {
                    updateExtraRom(extraRomFile);
                    extraRomPath.setText(extraRomFile.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("Updating Extra ROM from " + extraRomFile, e);
                }
            }
        });

        resetExtraRomButton.setOnAction(event -> {
            getConfiguration().setExtraRomPath(null);
            extraRomPath.setText(LocaleUtil.i18n("builtInMessage"));
        });

        changeDandanatorMiniRomButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectDandanatorRomMessage"));
            final File dandanatorRomFile = chooser.showOpenDialog(changeDandanatorMiniRomButton.getScene().getWindow());
            if (dandanatorRomFile != null) {
                try {
                    updateDandanatorRom(dandanatorRomFile);
                    dandanatorMiniRomPath.setText(dandanatorRomFile.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("Updating Dandanator ROM from " + dandanatorRomFile, e);
                }
            }
        });
        resetDandanatorMiniRomButton.setOnAction(event -> {
            getConfiguration().setDandanatorRomPath(null);
            dandanatorMiniRomPath.setText(LocaleUtil.i18n("builtInMessage"));
        });

        changeDandanatorPicFirmwareButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectDandanatorPicFirmwareMessage"));
            final File dandanatorPicFwFile = chooser.showOpenDialog(changeDandanatorPicFirmwareButton
                    .getScene().getWindow());
            if (dandanatorPicFwFile != null) {
                try {
                    updateDandanatorPicFirmware(dandanatorPicFwFile);
                    dandanatorPicFirmwarePath.setText(dandanatorPicFwFile.getAbsolutePath());
                } catch (Exception e) {
                    LOGGER.error("Updating Dandanator PIC Firmware from " + dandanatorPicFwFile, e);
                }
            }
        });
        resetDandanatorPicFirmwareButton.setOnAction(event -> {
           getConfiguration().setDandanatorPicFirmwarePath(null);
            dandanatorPicFirmwarePath.setText(LocaleUtil.i18n("builtInMessage"));
        });


    }

}
