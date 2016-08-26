package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.*;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import com.grelobites.romgenerator.view.util.DialogUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Pagination;
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

    @FXML
    private ImageView backgroundImageView;

    @FXML
    private Button changeBackgroundImageButton;

    @FXML
    private Button resetBackgroundImageButton;

    @FXML
    private Pagination charSetPagination;

    private ImageView charSetImageView;

    @FXML
    private Button changeCharSetButton;

    @FXML
    private Button resetCharSetButton;

    @FXML
    private ComboBox<String> romSetModeCombo;

    private void initializeImages() throws IOException {
        backgroundImage = ImageUtil.scrLoader(
                ImageUtil.newScreenshot(),
                new ByteArrayInputStream(Configuration.getInstance().getBackgroundImage()));
        charSetImage = new ZxScreen(256, 64);
        recreateCharSetImage();
    }

    private void recreateCharSetImage() throws IOException {
        charSetImage.setCharSet(Configuration.getInstance().getCharSet());
        charSetImage.setPen(ZxColor.BRIGHTWHITE);
        charSetImage.setInk(ZxColor.BLACK);
        charSetImage.clearScreen();
        charSetImage.printLine("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 3, 2);
        charSetImage.printLine("abcdefghijklmnopqrstuvwxyz", 1, 2);
        charSetImage.printLine("1234567890 !\"#$%&/()[]:;,.-_", 5, 2);
    }

    private void recreateBackgroundImage() throws IOException {
        LOGGER.debug("RecreateBackgroundImage");
        ImageUtil.scrLoader(backgroundImage,
                new ByteArrayInputStream(Configuration.getInstance().getBackgroundImage()));
    }

    private void updateBackgroundImage(File backgroundImageFile) throws IOException {
        if (isReadableFile(backgroundImageFile)) {
            Configuration.getInstance().setBackgroundImagePath(backgroundImageFile.getAbsolutePath());
            recreateBackgroundImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }

    private void updateCharSetPath(File charSetFile) throws IOException {
        if (isReadableFile(charSetFile)) {
            Configuration.getInstance().setCharSetPath(charSetFile.getAbsolutePath());
            recreateCharSetImage();
        } else {
            throw new IllegalArgumentException("No readable file provided");
        }
    }


    private boolean isReadableFile(File file) {
        return file.canRead() && file.isFile();
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
                Configuration.getInstance().setBackgroundImagePath(null);
                recreateBackgroundImage();
            } catch (Exception e) {
                LOGGER.error("Resetting background Image", e);
            }
        });
        Configuration.getInstance().backgroundImagePathProperty().addListener(
                (observable, oldValue, newValue) -> {
                    try {
                        recreateBackgroundImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating background image", ioe);
                    }
                });
    }

    private void charSetSetup() {
        Configuration configuration = Configuration.getInstance();
        charSetImageView = new ImageView();
        charSetImageView.setImage(charSetImage);
        charSetPagination.getStyleClass().add(Pagination.STYLE_CLASS_BULLET);
        //Disable the pagination when the charSetPath is externally provided
        charSetPagination.disableProperty().bind(configuration.charSetPathExternallyProvidedProperty());
        charSetPagination.setPageCount(configuration.getCharSetFactory().charSetCount());
        if (!configuration.getCharSetPathExternallyProvided()) {
            charSetPagination.setCurrentPageIndex(configuration.getInternalCharSetPathIndex());
        }
        charSetPagination.setPageFactory((index) -> {
            if (index < configuration.getCharSetFactory().charSetCount()) {
                return charSetImageView;
            } else {
                return null;
            }
        });

        charSetPagination.currentPageIndexProperty().addListener(
                (observable, oldValue, newValue) -> {
                    Configuration.getInstance().setCharSetPath(Configuration.INTERNAL_CHARSET_PREFIX + newValue);
                });

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
                Configuration.getInstance().setCharSetPath(null);
                charSetPagination.setCurrentPageIndex(0);
                recreateCharSetImage();
            } catch (Exception e) {
                LOGGER.error("Resetting charset", e);
            }
        });

        Configuration.getInstance().charSetPathProperty().addListener(
                (c) -> {
                    try {
                        recreateCharSetImage();
                    } catch (IOException ioe) {
                        LOGGER.error("Updating charset image", ioe);
                    }
                });

    }

    private void romSetModeSetup() {
        romSetModeCombo.setItems(FXCollections.observableArrayList(
                Stream.of(RomSetHandlerType.values())
                        .filter(RomSetHandlerType::isEnabled)
                        .map(Enum::name)
                        .collect(Collectors.toList())));
        romSetModeCombo.getSelectionModel().select(Configuration.getInstance()
                .getMode());
        romSetModeCombo.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    Configuration.getInstance().setMode(newValue);
                });
    }

    @FXML
    private void initialize() throws IOException {
        initializeImages();

        backgroundImageSetup();

        charSetSetup();

        romSetModeSetup();
    }
}
