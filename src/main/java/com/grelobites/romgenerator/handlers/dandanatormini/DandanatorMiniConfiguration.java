package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.romsethandler.RomSetType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class DandanatorMiniConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniConfiguration.class);

    private static final String DANDANATORROMPATH_PROPERTY = "dandanatorRomPath";
    private static final String EXTRAROMPATH_PROPERTY = "extraRomPath";
    private static final String TOGGLEPOKESMESSAGE_PROPERTY = "togglePokesMessage";
    private static final String EXTRAROMMESSAGE_PROPERTY = "extraRomMessage";
    private static final String LAUNCHGAMEMESSAGE_PROPERTY = "launchGameMessage";
    private static final String SELECTPOKESMESSAGE_PROPERTY = "selectPokesMessage";
    private static final String DANDANATORPICFIRMWAREPATH_PROPERTY = "dandanatorPicFirmwarePath";


    private static final String DEFAULT_MODE = RomSetType.DANDANATOR_MINI.name();

    private StringProperty dandanatorRomPath;
    private StringProperty extraRomPath;
    private StringProperty dandanatorPicFirmwarePath;
    private StringProperty togglePokesMessage;
    private StringProperty extraRomMessage;
    private StringProperty launchGameMessage;
    private StringProperty selectPokesMessage;

    private byte[] dandanatorRom;
    private byte[] extraRom;
    private byte[] dandanatorPicFirmware;

    private static DandanatorMiniConfiguration INSTANCE;

    public static DandanatorMiniConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DandanatorMiniConfiguration();
        }
        return INSTANCE;
    }

    private DandanatorMiniConfiguration() {
        dandanatorRomPath = new SimpleStringProperty();
        extraRomPath = new SimpleStringProperty();
        dandanatorPicFirmwarePath = new SimpleStringProperty();
        togglePokesMessage = new SimpleStringProperty();
        extraRomMessage = new SimpleStringProperty();
        launchGameMessage = new SimpleStringProperty();
        selectPokesMessage = new SimpleStringProperty();
    }

    private static boolean validConfigurationValue(String value) {
        return value != null && !Constants.ROMSET_PROVIDED.equals(value);
    }

    public String getDandanatorRomPath() throws IOException {
        return dandanatorRomPath.get();
    }

    public void setDandanatorRomPath(String dandanatorRomPath) {
        if (!Constants.ROMSET_PROVIDED.equals(dandanatorRomPath)) {
            dandanatorRom = null;
        }
        this.dandanatorRomPath.set(dandanatorRomPath);
        Configuration.persistConfigurationValue(DANDANATORROMPATH_PROPERTY, dandanatorRomPath);
    }

    public StringProperty dandanatorRomPathProperty() {
        return dandanatorRomPath;
    }

    public String getDandanatorPicFirmwarePath() {
        return dandanatorPicFirmwarePath.get();
    }

    public StringProperty dandanatorPicFirmwarePathProperty() {
        return dandanatorPicFirmwarePath;
    }

    public void setDandanatorPicFirmwarePath(String dandanatorPicFirmwarePath) {
        if (!Constants.ROMSET_PROVIDED.equals(dandanatorPicFirmwarePath)) {
            dandanatorPicFirmware = null;
        }
        this.dandanatorPicFirmwarePath.set(dandanatorPicFirmwarePath);
        Configuration.persistConfigurationValue(DANDANATORPICFIRMWAREPATH_PROPERTY, dandanatorPicFirmwarePath);
    }

    public String getExtraRomPath() {
        return extraRomPath.get();
    }

    public void setExtraRomPath(String extraRomPath) {
        extraRom = null;
        this.extraRomPath.set(extraRomPath);
        Configuration.persistConfigurationValue(EXTRAROMPATH_PROPERTY, extraRomPath);
    }

    public StringProperty extraRomPathProperty() {
        return extraRomPath;
    }

    public byte[] getDandanatorRom() throws IOException {
        if (dandanatorRom == null) {
            if (validConfigurationValue(dandanatorRomPath.get())) {
                try {
                    dandanatorRom = Files.readAllBytes(Paths.get(dandanatorRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Dandanator ROM from " + dandanatorRomPath, e);
                    dandanatorRom = DandanatorMiniConstants.getDandanatorRom();
                }
            } else {
                dandanatorRom = DandanatorMiniConstants.getDandanatorRom();
            }
        }
        return dandanatorRom;
    }

    public void setDandanatorRom(byte[] dandanatorRom) {
        this.dandanatorRom = dandanatorRom;
    }


    public byte[] getExtraRom() throws IOException {
        if (extraRom == null) {
            if (validConfigurationValue(extraRomPath.get())) {
                try {
                    extraRom = Files.readAllBytes(Paths.get(extraRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Extra ROM from " + extraRomPath, e);
                    extraRom = DandanatorMiniConstants.getExtraRom();
                }
            } else {
                extraRom = DandanatorMiniConstants.getExtraRom();
            }
        }
        return extraRom;
    }

    public void setExtraRom(byte[] extraRom) {
        this.extraRom = extraRom;
    }


    public String getTogglePokesMessage() {
        if (togglePokesMessage.get() == null) {
            return DandanatorMiniConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE;
        }
        return togglePokesMessage.get();
    }

    public void setTogglePokesMessage(String togglePokesMessage) {
        setTogglePokesMessage(togglePokesMessage, true);
    }

    public void setTogglePokesMessage(String togglePokesMessage, boolean persist) {
        this.togglePokesMessage.set(togglePokesMessage);
        if (persist) {
            Configuration.persistConfigurationValue(TOGGLEPOKESMESSAGE_PROPERTY, this.togglePokesMessage.get());
        }
    }

    public StringProperty togglePokesMessageProperty() {
        return togglePokesMessage;
    }

    public String getExtraRomMessage() {
        if (extraRomMessage.get() == null) {
            return DandanatorMiniConstants.DEFAULT_EXTRAROMKEY_MESSAGE;
        }
        return extraRomMessage.get();
    }

    public void setExtraRomMessage(String extraRomMessage) {
        setExtraRomMessage(extraRomMessage, true);
    }

    public void setExtraRomMessage(String extraRomMessage, boolean persist) {
        this.extraRomMessage.set(extraRomMessage);
        if (persist) {
            Configuration.persistConfigurationValue(EXTRAROMMESSAGE_PROPERTY, this.extraRomMessage.get());
        }
    }

    public StringProperty extraRomMessageProperty() {
        return extraRomMessage;
    }

    public String getLaunchGameMessage() {
        if (launchGameMessage.get() == null) {
            return DandanatorMiniConstants.DEFAULT_LAUNCHGAME_MESSAGE;
        }
        return launchGameMessage.get();
    }

    public void setLaunchGameMessage(String launchGameMessage) {
        setLaunchGameMessage(launchGameMessage, true);
    }

    public void setLaunchGameMessage(String launchGameMessage, boolean persist) {
        this.launchGameMessage.set(launchGameMessage);
        if (persist) {
            Configuration.persistConfigurationValue(LAUNCHGAMEMESSAGE_PROPERTY, this.launchGameMessage.get());
        }
    }

    public StringProperty launchGameMessageProperty() {
        return launchGameMessage;
    }

    public String getSelectPokesMessage() {
        if (selectPokesMessage.get() == null) {
            return DandanatorMiniConstants.DEFAULT_SELECTPOKE_MESSAGE;
        }
        return selectPokesMessage.get();
    }

    public StringProperty selectPokesMessageProperty() {
        return selectPokesMessage;
    }

    public void setSelectPokesMessage(String selectPokesMessage) {
        setSelectPokesMessage(selectPokesMessage, true);
    }

    public void setSelectPokesMessage(String selectPokesMessage, boolean persist) {
        this.selectPokesMessage.set(selectPokesMessage);
        if (persist) {
            Configuration.persistConfigurationValue(SELECTPOKESMESSAGE_PROPERTY, this.selectPokesMessage.get());
        }
    }

    public void setDandanatorPicFirmware(byte[] dandanatorPicFirmware) {
        this.dandanatorPicFirmware = dandanatorPicFirmware;
    }

    public byte[] getDandanatorPicFirmware() throws IOException {
        if (dandanatorPicFirmware == null) {
            if (validConfigurationValue(getDandanatorPicFirmwarePath())) {
                try {
                    dandanatorPicFirmware = Files.readAllBytes(Paths.get(dandanatorPicFirmwarePath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Dandanator PIC Firmware from " + dandanatorPicFirmwarePath, e);
                    dandanatorPicFirmware = DandanatorMiniConstants.getDefaultDandanatorPicFirmware();
                }
            } else {
                dandanatorPicFirmware = DandanatorMiniConstants.getDefaultDandanatorPicFirmware();
            }
        }
        return dandanatorPicFirmware;
    }

    private static DandanatorMiniConfiguration setFromPreferences(DandanatorMiniConfiguration configuration) {
        Preferences p = Configuration.getApplicationPreferences();
        //Do not use setters here, since they force the preferences to be reset again
        configuration.dandanatorRomPath.set(
                p.get(DANDANATORROMPATH_PROPERTY, null));
        configuration.extraRomPath.set(
                p.get(EXTRAROMPATH_PROPERTY, null));
        configuration.launchGameMessage.set(
                p.get(LAUNCHGAMEMESSAGE_PROPERTY, null));
        configuration.selectPokesMessage.set(
                p.get(SELECTPOKESMESSAGE_PROPERTY, null));
        configuration.extraRomMessage.set(
                p.get(EXTRAROMMESSAGE_PROPERTY, null));
        configuration.togglePokesMessage.set(
                p.get(TOGGLEPOKESMESSAGE_PROPERTY, null));
        configuration.dandanatorPicFirmwarePath.set(
                p.get(DANDANATORPICFIRMWAREPATH_PROPERTY, null));
        return configuration;
    }

}

