package com.grelobites.romgenerator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

public class PlayerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerConfiguration.class);

    private static final String LOADERPATH_PROPERTY = "loaderPath";
    private static final String BLOCKSIZE_PROPERTY = "blockSize";
    private static final String AUDIOMODE_PROPERTY = "audioMode";
    private static final String ENCODINGSPEED_PROPERTY = "encodingSpeed";
    private static final String PILOTLENGTH_PROPERTY = "pilotLength";
    private static final String TRAILLENGTH_PROPERTY = "trailLength";
    private static final String PAUSEBETWEENBLOCKS_PROPERTY = "pauseBetweenBlocks";
    private static final String USETARGETFEEDBACK_PROPERTY = "useTargetFeedback";
    private static final int DEFAULT_BLOCKSIZE = 0x1000;
    private static final String DEFAULT_AUDIOMODE = "STEREOINV";
    private static final int DEFAULT_ENCODINGSPEED = 2;
    private static final int DEFAULT_PILOTLENGTH = 250;
    private static final int DEFAULT_TRAILLENGTH = 0;
    private static final int DEFAULT_PAUSEBETWEENBLOCKS = 3000;

    private StringProperty loaderPath;
    private IntegerProperty blockSize;
    private StringProperty audioMode;
    private IntegerProperty encodingSpeed;
    private IntegerProperty pilotLength;
    private IntegerProperty trailLength;
    private IntegerProperty pauseBetweenBlocks;
    private BooleanProperty useTargetFeedback;

    private static PlayerConfiguration INSTANCE;

    private PlayerConfiguration() {
        loaderPath = new SimpleStringProperty();
        blockSize = new SimpleIntegerProperty(DEFAULT_BLOCKSIZE);
        audioMode = new SimpleStringProperty(DEFAULT_AUDIOMODE);
        encodingSpeed = new SimpleIntegerProperty(DEFAULT_ENCODINGSPEED);
        pilotLength = new SimpleIntegerProperty(DEFAULT_PILOTLENGTH);
        trailLength = new SimpleIntegerProperty(DEFAULT_TRAILLENGTH);
        pauseBetweenBlocks = new SimpleIntegerProperty(DEFAULT_PAUSEBETWEENBLOCKS);
        useTargetFeedback = new SimpleBooleanProperty(false);

        loaderPath.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                LOADERPATH_PROPERTY, newValue));
        blockSize.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                BLOCKSIZE_PROPERTY, newValue.toString()));
        audioMode.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                AUDIOMODE_PROPERTY, newValue));
        encodingSpeed.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                ENCODINGSPEED_PROPERTY, newValue.toString()));
        pilotLength.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                PILOTLENGTH_PROPERTY, newValue.toString()));
        trailLength.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                TRAILLENGTH_PROPERTY, newValue.toString()));
        pauseBetweenBlocks.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                PAUSEBETWEENBLOCKS_PROPERTY, newValue.toString()));
        useTargetFeedback.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                USETARGETFEEDBACK_PROPERTY, newValue.toString()));

    }

    public static PlayerConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public InputStream getLoaderStream() throws IOException {
        if (loaderPath.get() == null) {
            return PlayerConfiguration.class.getResourceAsStream("/loader.tap");
        } else {
            return new FileInputStream(loaderPath.get());
        }
    }

    public String getLoaderPath() {
        return loaderPath.get();
    }

    public StringProperty loaderPathProperty() {
        return loaderPath;
    }

    public void setLoaderPath(String loaderPath) {
        this.loaderPath.set(loaderPath);
    }

    public int getBlockSize() {
        return blockSize.get();
    }

    public IntegerProperty blockSizeProperty() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize.set(blockSize);
    }

    public String getAudioMode() {
        return audioMode.get();
    }

    public StringProperty audioModeProperty() {
        return audioMode;
    }

    public void setAudioMode(String audioMode) {
        this.audioMode.set(audioMode);
    }

    public int getEncodingSpeed() {
        return encodingSpeed.get();
    }

    public IntegerProperty encodingSpeedProperty() {
        return encodingSpeed;
    }

    public void setEncodingSpeed(int encodingSpeed) {
        this.encodingSpeed.set(encodingSpeed);
    }

    public int getPilotLength() {
        return pilotLength.get();
    }

    public IntegerProperty pilotLengthProperty() {
        return pilotLength;
    }

    public void setPilotLength(int pilotLength) {
        this.pilotLength.set(pilotLength);
    }

    public int getTrailLength() {
        return trailLength.get();
    }

    public IntegerProperty trailLengthProperty() {
        return trailLength;
    }

    public void setTrailLength(int trailLength) {
        this.trailLength.set(trailLength);
    }

    public int getPauseBetweenBlocks() {
        return pauseBetweenBlocks.get();
    }

    public IntegerProperty pauseBetweenBlocksProperty() {
        return pauseBetweenBlocks;
    }

    public void setPauseBetweenBlocks(int pauseBetweenBlocks) {
        this.pauseBetweenBlocks.set(pauseBetweenBlocks);
    }

    public boolean isUseTargetFeedback() {
        return useTargetFeedback.get();
    }

    public BooleanProperty useTargetFeedbackProperty() {
        return useTargetFeedback;
    }

    public void setUseTargetFeedback(boolean useTargetFeedback) {
        this.useTargetFeedback.set(useTargetFeedback);
    }

    public static Preferences getApplicationPreferences() {
        return Preferences.userNodeForPackage(PlayerConfiguration.class);
    }

    public static void persistConfigurationValue(String key, String value) {
        LOGGER.debug("persistConfigurationValue " + key + ", " + value);
        Preferences p = getApplicationPreferences();
        if (value != null) {
            p.put(key, value);
        } else {
            p.remove(key);
        }
    }

    private static Integer wrapIntegerProperty(Preferences preferences, String key) {
        String value = preferences.get(key, null);
        Integer result = value == null ? null : Integer.parseInt(value);
        LOGGER.debug("Resolving value for key " + key + " as " + result);
        return result;
    }

    private static PlayerConfiguration setFromPreferences(PlayerConfiguration configuration) {
        Preferences p = getApplicationPreferences();
        configuration.audioMode.set(p.get(AUDIOMODE_PROPERTY, DEFAULT_AUDIOMODE));
        configuration.blockSize.set(p.getInt(BLOCKSIZE_PROPERTY, DEFAULT_BLOCKSIZE));
        configuration.encodingSpeed.set(p.getInt(ENCODINGSPEED_PROPERTY, DEFAULT_ENCODINGSPEED));
        configuration.loaderPath.set(p.get(LOADERPATH_PROPERTY, null));
        configuration.pauseBetweenBlocks.set(p.getInt(PAUSEBETWEENBLOCKS_PROPERTY, DEFAULT_PAUSEBETWEENBLOCKS));
        configuration.pilotLength.set(p.getInt(PILOTLENGTH_PROPERTY, DEFAULT_PILOTLENGTH));
        configuration.trailLength.set(p.getInt(TRAILLENGTH_PROPERTY, DEFAULT_TRAILLENGTH));
        configuration.useTargetFeedback.set(p.getBoolean(USETARGETFEEDBACK_PROPERTY, false));
        return configuration;
    }

    synchronized private static PlayerConfiguration newInstance() {
        final PlayerConfiguration configuration = new PlayerConfiguration();
        return setFromPreferences(configuration);
    }
}