package com.grelobites.romgenerator.handlers.dandanatormini.model;

import com.grelobites.romgenerator.Constants;

public interface DandanatorConfigurationSetter {

    void setExtraRom(byte[] extraRom);
    void setExtraRomPath(String extraRomPath);
    void setExtraRomMessage(String extraRomMessage);
    void setTogglePokesMessage(String togglePokesMessage);
    void setLaunchGameMessage(String launchGameMessage);
    void setSelectPokesMessage(String selectPokesMessage);

}
