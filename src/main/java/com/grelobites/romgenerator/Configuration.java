package com.grelobites.romgenerator;

import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RomSet;
import com.grelobites.romgenerator.util.CharSetFactory;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

public class Configuration {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

    private static final String MODE_PROPERTY = "mode";
    private static final String BACKGROUNDIMAGEPATH_PROPERTY = "backgroundImagePath";
    private static final String CHARSETPATH_PROPERTY = "charSetPath";
    private static final String TAPLOADERTARGET_PROPERTY = "tapLoaderTarget";
    private static final String PLUS2AROMSET_PROPERTY = "plus2ARomSet";
    private static final String DEFAULT_TAPLOADER_TARGET = HardwareMode.HW_48K.name();
    private static final RomSet DEFAULT_PLUS2AROMSET = RomSet.PLUS2A_40_EN;
    public static final String INTERNAL_CHARSET_PREFIX= "internal://";
    private static final String DEFAULT_MODE = RomSetHandlerType.DDNTR_V7.name();

    byte[] charSet;
    byte[] backgroundImage;

    private StringProperty mode;
    private StringProperty backgroundImagePath;
    private StringProperty charSetPath;
    private BooleanProperty charSetPathExternallyProvided;
    private CharSetFactory charSetFactory;
    private RamGameCompressor ramGameCompressor;
    private BooleanProperty allowExperimentalGames;
    private StringProperty tapLoaderTarget;
    private RomSet plus2ARomSet = DEFAULT_PLUS2AROMSET;

    private static Configuration INSTANCE;

    private static boolean isInternalCharSetPath(String value) {
        return value == null || value.startsWith(Configuration.INTERNAL_CHARSET_PREFIX);
    }

    private static boolean isCharSetExternallyProvided(String value) {
        return Constants.ROMSET_PROVIDED.equals(value) || !isInternalCharSetPath(value);
    }

    private Configuration() {
        this.backgroundImagePath = new SimpleStringProperty();
        this.charSetPath = new SimpleStringProperty();
        this.charSetPathExternallyProvided = new SimpleBooleanProperty();
        this.mode = new SimpleStringProperty(DEFAULT_MODE);
        this.allowExperimentalGames = new SimpleBooleanProperty(true);

        this.charSetPath.addListener((observable, oldValue, newValue) -> {
           charSetPathExternallyProvided.set(isCharSetExternallyProvided(newValue));
        });
        this.charSetFactory = new CharSetFactory();
        this.tapLoaderTarget = new SimpleStringProperty(DEFAULT_TAPLOADER_TARGET);
    }

    public static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public CharSetFactory getCharSetFactory() {
        return charSetFactory;
    }

    private static boolean validConfigurationValue(String value) {
        return value != null && !Constants.ROMSET_PROVIDED.equals(value);
    }


    public String getMode() {
        if (mode.get() == null) {
            return DEFAULT_MODE;
        } else {
            return mode.get();
        }
    }

    public void setMode(String mode) {
        this.mode.set(mode);
    }

    public StringProperty modeProperty() {
        return mode;
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
        if (!Constants.ROMSET_PROVIDED.equals(backgroundImagePath)) {
            backgroundImage = null;
        }

        this.backgroundImagePath.set(backgroundImagePath);
    }

    public byte[] getBackgroundImage() throws IOException {
        if (backgroundImage == null) {
            if (validConfigurationValue(backgroundImagePath.get())) {
                try {
                    backgroundImage = Files.readAllBytes(Paths.get(backgroundImagePath.get()));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Background Image from  " + backgroundImagePath.get(), e);
                    backgroundImage = Constants.getDefaultMenuScreen();
                }
            } else {
                backgroundImage = Constants.getDefaultMenuScreen();
            }
        }
        return backgroundImage;
    }

    public void setBackgroundImage(byte[] backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public int getInternalCharSetPathIndex() {
        if (getCharSetPath() != null) {
            return Integer.parseInt(getCharSetPath().substring(INTERNAL_CHARSET_PREFIX.length()));
        } else {
            return 0;
        }
    }

    public byte[] getCharSet() throws IOException {
        if (charSet == null) {
            if (isInternalCharSetPath(getCharSetPath())) {
                return charSetFactory.getCharSetAt(getInternalCharSetPathIndex());
            } else {
                if (validConfigurationValue(getCharSetPath())) {
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

    public BooleanProperty charSetPathExternallyProvidedProperty() {
        return charSetPathExternallyProvided;
    }

    public boolean getCharSetPathExternallyProvided() {
        return charSetPathExternallyProvided.get();
    }

    public void setCharSetPath(String charSetPath) {
        if (!Constants.ROMSET_PROVIDED.equals(charSetPath)) {
            charSet = null;
        }
        this.charSetPath.set(charSetPath);
    }

    public RamGameCompressor getRamGameCompressor() {
        return ramGameCompressor;
    }

    public void setRamGameCompressor(RamGameCompressor ramGameCompressor) {
        this.ramGameCompressor = ramGameCompressor;
    }

    public boolean isAllowExperimentalGames() {
        return allowExperimentalGames.get();
    }

    public BooleanProperty allowExperimentalGamesProperty() {
        return allowExperimentalGames;
    }

    public void setAllowExperimentalGames(boolean allowExperimentalGames) {
        this.allowExperimentalGames.set(allowExperimentalGames);
    }

    public String getTapLoaderTarget() {
        return tapLoaderTarget.get();
    }

    public StringProperty tapLoaderTargetProperty() {
        return tapLoaderTarget;
    }

    public RomSet getPlus2ARomSet() {
        return plus2ARomSet;
    }

    public void setPlus2ARomSet(RomSet plus2ARomSet) {
        this.plus2ARomSet = plus2ARomSet;
        persistConfigurationValue(PLUS2AROMSET_PROPERTY, plus2ARomSet.name());
    }

    public void setTapLoaderTarget(String tapLoaderTarget) {
        this.tapLoaderTarget.set(tapLoaderTarget);
        persistConfigurationValue(TAPLOADERTARGET_PROPERTY, tapLoaderTarget);
    }

    public static Preferences getApplicationPreferences() {
        return Preferences.userNodeForPackage(Configuration.class);
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        if (!Constants.ROMSET_PROVIDED.equals(value)) {
            Preferences p = getApplicationPreferences();
            if (value != null) {
                p.put(key, value);
            } else {
                p.remove(key);
            }
        }
    }

    private static Configuration setFromPreferences(Configuration configuration) {
        Preferences p = getApplicationPreferences();
        configuration.tapLoaderTarget.set(p.get(TAPLOADERTARGET_PROPERTY,
                DEFAULT_TAPLOADER_TARGET));
        configuration.plus2ARomSet = RomSet.valueOf(
                p.get(PLUS2AROMSET_PROPERTY, DEFAULT_PLUS2AROMSET.name()));
        return configuration;
    }

    synchronized private static Configuration newInstance() {
        final Configuration configuration = new Configuration();
        return setFromPreferences(configuration);
    }
}
