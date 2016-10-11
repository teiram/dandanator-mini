package com.grelobites.romgenerator.util.player;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;

import java.util.Optional;

public interface DataPlayer {

    void send();

    void stop();

    void onFinalization(Runnable onFinalization);

    DoubleProperty progressProperty();

    Optional<DoubleProperty> volumeProperty();

}
