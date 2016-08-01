package com.grelobites.romgenerator.view;

import com.grelobites.romgenerator.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;

public class AboutController {

    @FXML
    private ImageView logo;

    @FXML
    private Label versionLabel;

    @FXML
    private void initialize() throws IOException {
        logo.setImage(new Image(AboutController.class.getResourceAsStream("/romgenerator-icon.png")));
        versionLabel.setText(String.format("Version %s", Constants.currentVersion()));
    }
}
