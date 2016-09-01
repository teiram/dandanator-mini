package com.grelobites.romgenerator.view.util;

import javafx.scene.control.Alert;

public class DialogUtil {

    private static final String CSS_LOCATION = "/com/grelobites/romgenerator/view/theme.css";

    private static void applyTheme(Alert alert) {
        alert.getDialogPane().getStylesheets().add(DialogUtil.class.getResource(CSS_LOCATION).toExternalForm());
    }

    private static void populateAlert(Alert alert, String title, String headerText, String contentText) {
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        applyTheme(alert);
    }

    public static Alert buildAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        populateAlert(alert, title, headerText, contentText);

        return alert;
    }

    public static Alert buildErrorAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        populateAlert(alert, title, headerText, contentText);

        return alert;
    }

    public static Alert buildWarningAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        populateAlert(alert, title, headerText, contentText);

        return alert;
    }
}
