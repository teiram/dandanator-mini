package com.grelobites.romgenerator.model;

import javafx.scene.image.Image;

public interface RamGame extends Game {

    Image getScreenshot();

    void setScreenshot(Image screenshot);

    HardwareMode getHardwareMode();

    void setHardwareMode(HardwareMode hardwareMode);
}
