package com.grelobites.dandanator.view.util;

import com.grelobites.dandanator.model.PokeEntity;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mteira on 20/6/16.
 */
public class PokeEntityTreeCell extends TreeCell<PokeEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PokeEntityTreeCell.class);
    private TextField textField;

    @Override
    public void cancelEdit() {
        LOGGER.debug("Cancelling edition");
        super.cancelEdit();
        setGraphic(null);
        setText(getItem().getViewRepresentation());
    }

    @Override
    public void startEdit() {
        LOGGER.debug("Starting edition");
        super.startEdit();
        if(textField == null) {
            createTextField();
        }
        setText(null);
        setGraphic(textField);
        textField.selectAll();
    }

    private void createTextField() {
        textField = new TextField(getItem().getViewRepresentation());
        textField.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                getItem().update(textField.getText());
                LOGGER.debug("Committing edition");
                commitEdit(getItem());
            } else if(e.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
            }
        });
    }

    @Override
    protected void updateItem(PokeEntity item, boolean empty) {
        super.updateItem(item, empty);
        if (empty) {
            setText(null);
            setGraphic(null);
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item.getViewRepresentation());
                }
                setText(null);
                setGraphic(textField);
            } else {
                setText(item.getViewRepresentation());
            }
            setGraphic(null);
            setText(item.getViewRepresentation());
            setGraphic(null);
        }
    }
}
