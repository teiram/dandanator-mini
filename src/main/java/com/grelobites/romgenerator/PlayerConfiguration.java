package com.grelobites.romgenerator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

public class PlayerConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerConfiguration.class);

    private static final String DEFAULT_LOADER_BINARY = "/player/eewriter.bin";
    private static final String ROMSET_LOADER_BINARY = "/player/romset_eewriter.bin";
    private static final String SCREEN_RESOURCE = "/player/screen.scr";
    private static final String AUDIOMODE_PROPERTY = "audioMode";
    private static final String ENCODINGSPEED_PROPERTY = "encodingSpeed";
    private static final String USETARGETFEEDBACK_PROPERTY = "useTargetFeedback";
    private static final String USESERIALPORT_PROPERTY = "useSerialPort";
    private static final String SERIALPORT_PROPERTY = "serialPort";
    private static final String SENDLOADER_PROPERTY = "sendLoader";

    private static final int DEFAULT_BLOCKSIZE = 0x8000;
    private static final String DEFAULT_AUDIOMODE = "STEREOINV";
    private static final int DEFAULT_ENCODINGSPEED = 5;
    private static final int DEFAULT_PILOTLENGTH = 500;
    private static final int DEFAULT_TRAILLENGTH = 0;
    private static final int DEFAULT_RECORDINGPAUSE = 8000;
    private static final String CASSETE_IMAGE_RESOURCE = "/player/cassette.jpg";
    private static final String KEMPSTON_IMAGE_RESOURCE = "/player/kempston.png";

    public static final String[] AUDIO_MODES = new String[] {"MONO", "STEREO", "STEREOINV"};

    private StringProperty loaderPath;
    private IntegerProperty blockSize;
    private StringProperty audioMode;
    private IntegerProperty encodingSpeed;
    private IntegerProperty pilotLength;
    private IntegerProperty trailLength;
    private IntegerProperty recordingPause;
    private BooleanProperty useTargetFeedback;
    private BooleanProperty useSerialPort;
    private StringProperty serialPort;
    private StringProperty customRomSetPath;
    private BooleanProperty sendLoader;

    private static Image cassetteImage;
    private static Image kempstonImage;

    private static PlayerConfiguration INSTANCE;

    private PlayerConfiguration() {
        loaderPath = new SimpleStringProperty();
        blockSize = new SimpleIntegerProperty(DEFAULT_BLOCKSIZE);
        audioMode = new SimpleStringProperty(DEFAULT_AUDIOMODE);
        encodingSpeed = new SimpleIntegerProperty(DEFAULT_ENCODINGSPEED);
        pilotLength = new SimpleIntegerProperty(DEFAULT_PILOTLENGTH);
        trailLength = new SimpleIntegerProperty(DEFAULT_TRAILLENGTH);
        recordingPause = new SimpleIntegerProperty(DEFAULT_RECORDINGPAUSE);
        useTargetFeedback = new SimpleBooleanProperty(true);
        useSerialPort = new SimpleBooleanProperty(false);
        serialPort = new SimpleStringProperty(null);
        customRomSetPath = new SimpleStringProperty(null);
        sendLoader = new SimpleBooleanProperty(true);

        audioMode.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                AUDIOMODE_PROPERTY, newValue));
        encodingSpeed.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                ENCODINGSPEED_PROPERTY, newValue.toString()));
        useTargetFeedback.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                USETARGETFEEDBACK_PROPERTY, newValue.toString()));
        useSerialPort.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                USESERIALPORT_PROPERTY, newValue.toString()));
        serialPort.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                SERIALPORT_PROPERTY, newValue));
        sendLoader.addListener((observable, oldValue, newValue) -> persistConfigurationValue(
                SENDLOADER_PROPERTY, newValue.toString()));

    }

    public static PlayerConfiguration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  newInstance();
        }
        return INSTANCE;
    }

    public InputStream getLoaderStream() throws IOException {
        if (loaderPath.get() == null) {
            return PlayerConfiguration.class.getResourceAsStream(DEFAULT_LOADER_BINARY);
        } else {
            return new FileInputStream(loaderPath.get());
        }
    }

    public InputStream getRomsetLoaderStream() throws IOException {
        return PlayerConfiguration.class.getResourceAsStream(ROMSET_LOADER_BINARY);
    }

    public InputStream getScreenStream() throws IOException {
        return PlayerConfiguration.class.getResourceAsStream(SCREEN_RESOURCE);
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

    public int getRecordingPause() {
        return recordingPause.get();
    }

    public IntegerProperty recordingPauseProperty() {
        return recordingPause;
    }

    public void setRecordingPause(int recordingPause) {
        this.recordingPause.set(recordingPause);
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

    public boolean isUseSerialPort() {
        return useSerialPort.get();
    }

    public BooleanProperty useSerialPortProperty() {
        return useSerialPort;
    }

    public void setUseSerialPort(boolean useSerialPort) {
        this.useSerialPort.set(useSerialPort);
    }

    public String getSerialPort() {
        return serialPort.get();
    }

    public StringProperty serialPortProperty() {
        return serialPort;
    }

    public void setSerialPort(String serialPort) {
        this.serialPort.set(serialPort);
    }

    public String getCustomRomSetPath() {
        return customRomSetPath.get();
    }

    public StringProperty customRomSetPathProperty() {
        return customRomSetPath;
    }

    public void setCustomRomSetPath(String customRomSetPath) {
        this.customRomSetPath.set(customRomSetPath);
    }

    public boolean getSendLoader() {
        return sendLoader.get();
    }

    public BooleanProperty sendLoaderProperty() {
        return sendLoader;
    }

    public void setSendLoader(boolean sendLoader) {
        this.sendLoader.set(sendLoader);
    }

    public Image getCassetteImage() {
        if (cassetteImage == null) {
            cassetteImage = new Image(PlayerConfiguration.class.getResourceAsStream(CASSETE_IMAGE_RESOURCE));
        }
        return cassetteImage;
    }

    public Image getKempstonImage() {
        if (kempstonImage == null) {
            kempstonImage = new Image(PlayerConfiguration.class.getResourceAsStream(KEMPSTON_IMAGE_RESOURCE));
        }
        return kempstonImage;
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

    private static PlayerConfiguration setFromPreferences(PlayerConfiguration configuration) {
        Preferences p = getApplicationPreferences();
        configuration.audioMode.set(p.get(AUDIOMODE_PROPERTY, DEFAULT_AUDIOMODE));
        configuration.encodingSpeed.set(p.getInt(ENCODINGSPEED_PROPERTY, DEFAULT_ENCODINGSPEED));
        configuration.useTargetFeedback.set(p.getBoolean(USETARGETFEEDBACK_PROPERTY, false));
        configuration.useSerialPort.set(p.getBoolean(USESERIALPORT_PROPERTY, false));
        configuration.serialPort.set(p.get(SERIALPORT_PROPERTY, null));
        configuration.sendLoader.set(p.getBoolean(SENDLOADER_PROPERTY, true));
        return configuration;
    }

    synchronized private static PlayerConfiguration newInstance() {
        final PlayerConfiguration configuration = new PlayerConfiguration();
        return setFromPreferences(configuration);
    }
}
