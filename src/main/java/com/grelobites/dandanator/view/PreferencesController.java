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
    private TextField testRomMessage;

    @FXML
    private Button resetTestRomMessage;

    @FXML
    private Label testRomPath;

    @FXML
    private Button changeTestRomButton;

    @FXML
    private Button resetTestRomButton;

    @FXML
    private Label dandanatorMiniRomPath;

    @FXML
    private Button changeDandanatorMiniRomButton;

    @FXML
    private Button resetDandanatorMiniRomButton;

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

    private void updateTestRom(File testRomFile) {
        if (testRomFile.canRead() && testRomFile.isFile()) {
            getConfiguration().setTestRomPath(testRomFile.getAbsolutePath());
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

        testRomMessage.setText(getConfiguration().getTestRomMessage());
        testRomMessage.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > Constants.TEST_ROM_MESSAGE_MAXLENGTH) {
                testRomMessage.setText(oldValue);
            } else {
                getConfiguration().setTestRomMessage(newValue);
            }
        });
        resetTestRomMessage.setOnAction(event -> {
            getConfiguration().setTestRomMessage(null);
            testRomMessage.setText(getConfiguration().getTestRomMessage());
        });

        changeTestRomButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectTestRomMessage"));
            final File testRomFile = chooser.showOpenDialog(changeTestRomButton.getScene().getWindow());
            try {
                updateTestRom(testRomFile);
                testRomPath.setText(testRomFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Updating Test ROM from " +  testRomFile, e);
            }
        });

        resetTestRomButton.setOnAction(event -> {
            getConfiguration().setTestRomPath(null);
            testRomPath.setText(LocaleUtil.i18n("builtInMessage"));
        });

        changeDandanatorMiniRomButton.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(LocaleUtil.i18n("selectDandanatorRomMessage"));
            final File dandanatorRomFile = chooser.showOpenDialog(changeDandanatorMiniRomButton.getScene().getWindow());
            try {
                updateDandanatorRom(dandanatorRomFile);
                dandanatorMiniRomPath.setText(dandanatorRomFile.getAbsolutePath());
            } catch (Exception e) {
                LOGGER.error("Updating Dandanator ROM from " +  dandanatorRomFile, e);
            }
        });
        resetDandanatorMiniRomButton.setOnAction(event -> {
            getConfiguration().setDandanatorRomPath(null);
            dandanatorMiniRomPath.setText(LocaleUtil.i18n("builtInMessage"));
        });


    }

}
