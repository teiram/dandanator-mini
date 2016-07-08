package com.grelobites.dandanator;

import com.grelobites.dandanator.util.romset.RomSetType;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String CONFIGURATION_FILE = ".dandanator.properties";

    private static final String DANDANATORROMPATH_PROPERTY = "dandanatorRomPath";
    private static final String BACKGROUNDIMAGEPATH_PROPERTY = "backgroundImagePath";
    private static final String EXTRAROMPATH_PROPERTY = "extraRomPath";
    private static final String CHARSETPATH_PROPERTY = "charSetPath";
    private static final String TOGGLEPOKESMESSAGE_PROPERTY = "togglePokesMessage";
    private static final String EXTRAROMMESSAGE_PROPERTY = "extraRomMessage";
    private static final String LAUNCHGAMEMESSAGE_PROPERTY = "launchGameMessage";
    private static final String SELECTPOKESMESSAGE_PROPERTY = "selectPokesMessage";

    private static final String DEFAULT_MODE = RomSetType.DANDANATOR_MINI.name();

    private String dandanatorRomPath;
    private StringProperty backgroundImagePath;
    private String extraRomPath;
    private StringProperty charSetPath;
    private StringProperty togglePokesMessage;
    private StringProperty extraRomMessage;
    private StringProperty launchGameMessage;
    private StringProperty selectPokesMessage;
    private String mode;

    private static Configuration INSTANCE;

    byte[] dandanatorRom;
    byte[] backgroundImage;
    byte[] extraRom;
    byte[] charSet;

    private Configuration() {
        backgroundImagePath = new SimpleStringProperty();
        charSetPath = new SimpleStringProperty();
        togglePokesMessage = new SimpleStringProperty();
        extraRomMessage = new SimpleStringProperty();
        launchGameMessage = new SimpleStringProperty();
        selectPokesMessage = new SimpleStringProperty();
    }

    public static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }


    public String getDandanatorRomPath() throws IOException {
        return dandanatorRomPath;
    }

    public void setDandanatorRomPath(String dandanatorRomPath) {
        this.dandanatorRomPath = dandanatorRomPath;
        dandanatorRom = null;
    }

    public String getBackgroundImagePath() {
        return backgroundImagePath.get();
    }

    public StringProperty backgroundImagePathProperty() {
        return backgroundImagePath;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        //Invalidate the background image in advance, to avoid the listeners to
        //enter before the property is set to null
        backgroundImage = null;
        this.backgroundImagePath.set(backgroundImagePath);
    }

    public String getExtraRomPath() {
        return extraRomPath;
    }

    public void setExtraRomPath(String extraRomPath) {
        this.extraRomPath = extraRomPath;
        extraRom = null;
    }

    public byte[] getDandanatorRom() throws IOException {
        if (dandanatorRom == null) {
            if (dandanatorRomPath != null) {
                try {
                    dandanatorRom = Files.readAllBytes(Paths.get(dandanatorRomPath));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Dandanator ROM from " + dandanatorRomPath, e);
                    dandanatorRom = Constants.getDandanatorRom();
                }
            } else {
                dandanatorRom = Constants.getDandanatorRom();
            }
        }
        return dandanatorRom;
    }

    public void setDandanatorRom(byte[] dandanatorRom) {
        this.dandanatorRom = dandanatorRom;
    }

    public byte[] getBackgroundImage() throws IOException {
        if (backgroundImage == null) {
            if (backgroundImagePath.get() != null) {
                try {
                    backgroundImage = Files.readAllBytes(Paths.get(backgroundImagePath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Background Image from  " + backgroundImagePath.get(), e);
                    backgroundImage = Constants.getDefaultDandanatorScreen();
                }
            } else {
                backgroundImage = Constants.getDefaultDandanatorScreen();
            }
        }
        return backgroundImage;
    }

    public void setBackgroundImage(byte[] backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public byte[] getExtraRom() throws IOException {
        if (extraRom == null) {
            if (extraRomPath != null) {
                try {
                    extraRom = Files.readAllBytes(Paths.get(extraRomPath));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Extra ROM from " + extraRomPath, e);
                    extraRom = Constants.getExtraRom();
                }
            } else {
                extraRom = Constants.getExtraRom();
            }
        }
        return extraRom;
    }

    public void setExtraRom(byte[] extraRom) {
        this.extraRom = extraRom;
    }

    public byte[] getCharSet() throws IOException {
        if (charSet == null) {
            if (getCharSetPath() != null) {
                try {
                    charSet = Files.readAllBytes(Paths.get(charSetPath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load CharSet from " + charSetPath, e);
                    charSet = Constants.getDefaultCharset();
                }
            } else {
                charSet = Constants.getDefaultCharset();
            }
        }
        return charSet;
    }

    public void setCharSet(byte[] charSet) {
        this.charSet = charSet;
    }

    public String getCharSetPath() {
        return charSetPath.get();
    }

    public StringProperty charSetPathProperty() {
        return charSetPath;
    }

    public void setCharSetPath(String charSetPath) {
        charSet = null;
        this.charSetPath.set(charSetPath);
    }

    public String getTogglePokesMessage() {
        if (togglePokesMessage.get() == null) {
            return Constants.DEFAULT_TOGGLEPOKESKEY_MESSAGE;
        }
        return togglePokesMessage.get();
    }

    public void setTogglePokesMessage(String togglePokesMessage) {
        this.togglePokesMessage.set(togglePokesMessage);
    }

    public StringProperty togglePokesMessageProperty() {
        return togglePokesMessage;
    }

    public String getExtraRomMessage() {
        if (extraRomMessage.get() == null) {
            return Constants.DEFAULT_EXTRAROMKEY_MESSAGE;
        }
        return extraRomMessage.get();
    }

    public void setExtraRomMessage(String extraRomMessage) {
        this.extraRomMessage.set(extraRomMessage);
    }

    public StringProperty extraRomMessageProperty() {
        return extraRomMessage;
    }

    public String getLaunchGameMessage() {
        if (launchGameMessage.get() == null) {
            return Constants.DEFAULT_LAUNCHGAME_MESSAGE;
        }
        return launchGameMessage.get();
    }

    public void setLaunchGameMessage(String launchGameMessage) {
        this.launchGameMessage.set(launchGameMessage);
    }

    public StringProperty launchGameMessageProperty() {
        return launchGameMessage;
    }

    public String getSelectPokesMessage() {
        if (selectPokesMessage.get() == null) {
            return Constants.DEFAULT_SELECTPOKE_MESSAGE;
        }
        return selectPokesMessage.get();
    }

    public StringProperty selectPokesMessageProperty() {
        return selectPokesMessage;
    }

    public void setSelectPokesMessage(String selectPokesMessage) {
        this.selectPokesMessage.set(selectPokesMessage);
    }


    public String getMode() {
        if (mode == null) {
            mode = DEFAULT_MODE;
        }
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }


    private static Configuration setFromProperties(Properties p, Configuration configuration) {
        configuration.setBackgroundImagePath(
                p.getProperty(BACKGROUNDIMAGEPATH_PROPERTY));
        configuration.setDandanatorRomPath(
                p.getProperty(DANDANATORROMPATH_PROPERTY));
        configuration.setExtraRomPath(
                p.getProperty(EXTRAROMPATH_PROPERTY));
        configuration.setCharSetPath(
                p.getProperty(CHARSETPATH_PROPERTY));
        configuration.setLaunchGameMessage(
                p.getProperty(LAUNCHGAMEMESSAGE_PROPERTY));
        configuration.setSelectPokesMessage(
                p.getProperty(SELECTPOKESMESSAGE_PROPERTY));
        configuration.setExtraRomMessage(
                p.getProperty(EXTRAROMMESSAGE_PROPERTY));
        configuration.setTogglePokesMessage(
                p.getProperty(TOGGLEPOKESMESSAGE_PROPERTY));

        return configuration;
    }

    private static Properties loadConfigurationFromFile(Path configFile) {
        LOGGER.debug("Trying to load configuration from " + configFile.toAbsolutePath());
        Properties properties = new Properties();
        try {
            if (Files.exists(configFile) && Files.isRegularFile(configFile) && Files.isReadable(configFile)) {
                properties.load(Files.newInputStream(configFile));
                return properties;
            }
        } catch (Exception e) {
            LOGGER.warn("Loading configuration from " + configFile, e);
        }
        return null;
    }

    private static Optional<Properties> loadConfigurationFile() {
        Properties properties = null;
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            Path configFile = Paths.get(userHome, CONFIGURATION_FILE);
            properties = loadConfigurationFromFile(configFile);
            if (properties == null) {
                Path currentPath = Paths.get("");
                properties = loadConfigurationFromFile(currentPath);
            }
        }
        return Optional.ofNullable(properties);
    }

    synchronized private static Configuration newInstance() {
        final Configuration configuration = new Configuration();
        loadConfigurationFile().map(p -> setFromProperties(p, configuration));
        return configuration;
    }
}
