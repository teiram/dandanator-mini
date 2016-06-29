package com.grelobites.dandanator.view.util;

import javafx.scene.control.Alert;

public class DialogUtil {

    private static final String CSS_LOCATION = "/com/grelobites/dandanator/view/theme.css";

    public static Alert buildAlert(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.getDialogPane().getStylesheets().add(DialogUtil.class.getResource(CSS_LOCATION).toExternalForm());

        return alert;
    }
}
