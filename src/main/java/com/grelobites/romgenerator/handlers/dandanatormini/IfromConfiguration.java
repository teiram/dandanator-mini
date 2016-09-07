package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IfromConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(IfromConfiguration.class);

    private StringProperty baseRomPath;
    private StringProperty customRomPath;
    private StringProperty togglePokesMessage;
    private StringProperty launchGameMessage;
    private StringProperty selectPokesMessage;
    private StringProperty launchCustomRomMessage;

    private byte[] baseRom;
    private byte[] customRom;

    private static Compressor compressor = CompressorFactory.getDefaultCompressor();

    private static IfromConfiguration INSTANCE;


    public static IfromConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = newInstance();
        }
        return INSTANCE;
    }

    private IfromConfiguration() {
        baseRomPath = new SimpleStringProperty();
        customRomPath = new SimpleStringProperty();
        togglePokesMessage = new SimpleStringProperty();
        launchGameMessage = new SimpleStringProperty();
        selectPokesMessage = new SimpleStringProperty();
        launchCustomRomMessage = new SimpleStringProperty();
    }

    private static boolean validConfigurationValue(String value) {
        return value != null && !Constants.ROMSET_PROVIDED.equals(value);
    }

    public String getBaseRomPath() throws IOException {
        return baseRomPath.get();
    }

    public void setBaseRomPath(String baseRomPath) {
        if (!Constants.ROMSET_PROVIDED.equals(baseRomPath)) {
            baseRomPath = null;
        }
        this.baseRomPath.set(baseRomPath);
    }

    public StringProperty baseRomPathProperty() {
        return baseRomPath;
    }

    public String getCustomRomPath() {
        return customRomPath.get();
    }

    public void setCustomRomPath(String customRomPath) {
        customRom = null;
        this.customRomPath.set(customRomPath);
    }

    public StringProperty customRomPathProperty() {
        return customRomPath;
    }

    public byte[] getBaseRom() throws IOException {
        if (baseRom == null) {
            if (validConfigurationValue(baseRomPath.get())) {
                try {
                    baseRom = Files.readAllBytes(Paths.get(baseRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Base ROM from " + baseRomPath, e);
                    baseRom = IfromConstants.getBaseRom();
                }
            } else {
                baseRom = IfromConstants.getBaseRom();
            }
        }
        return baseRom;
    }

    public void setBaseRom(byte[] baseRom) {
        this.baseRom = baseRom;
    }


    public byte[] getCustomRom() throws IOException {
        if (customRom == null) {
            if (validConfigurationValue(customRomPath.get())) {
                try {
                    customRom = Files.readAllBytes(Paths.get(customRomPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Custom ROM from " + customRomPath, e);
                    customRom = IfromConstants.getCustomRom();
                }
            } else {
                customRom = IfromConstants.getCustomRom();
            }
        }
        return customRom;
    }

    public void setCustomRom(byte[] customRom) {
        this.customRom = customRom;
    }


    public String getTogglePokesMessage() {
        if (togglePokesMessage.get() == null) {
            return IfromConstants.DEFAULT_TOGGLEPOKESKEY_MESSAGE;
        }
        return togglePokesMessage.get();
    }

    public void setTogglePokesMessage(String togglePokesMessage) {
        this.togglePokesMessage.set(togglePokesMessage);
    }

    public StringProperty togglePokesMessageProperty() {
        return togglePokesMessage;
    }

    public String getLaunchGameMessage() {
        if (launchGameMessage.get() == null) {
            return IfromConstants.DEFAULT_LAUNCHGAME_MESSAGE;
        }
        return launchGameMessage.get();
    }

    public void setLaunchGameMessage(String launchGameMessage) {
        this.launchGameMessage.set(launchGameMessage);
    }

    public StringProperty launchGameMessageProperty() {
        return launchGameMessage;
    }

    public String getLaunchCustomRomMessage() {
        if (launchCustomRomMessage.get() == null) {
            return IfromConstants.DEFAULT_CUSTOMROMKEY_MESSAGE;
        }
        return launchCustomRomMessage.get();
    }

    public StringProperty launchCustomRomMessageProperty() {
        return launchCustomRomMessage;
    }

    public void setLaunchCustomRomMessage(String launchCustomRomMessage) {
        this.launchCustomRomMessage.set(launchCustomRomMessage);
    }

    public String getSelectPokesMessage() {
        if (selectPokesMessage.get() == null) {
            return IfromConstants.DEFAULT_SELECTPOKE_MESSAGE;
        }
        return selectPokesMessage.get();
    }

    public StringProperty selectPokesMessageProperty() {
        return selectPokesMessage;
    }

    public void setSelectPokesMessage(String selectPokesMessage) {
        this.selectPokesMessage.set(selectPokesMessage);
    }

    public Compressor getCompressor() {
        return compressor;
    }

    synchronized private static IfromConfiguration newInstance() {
        final IfromConfiguration configuration = new IfromConfiguration();
        return configuration;
    }
}
