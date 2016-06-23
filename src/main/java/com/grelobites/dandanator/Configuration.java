package com.grelobites.dandanator;

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
    private static final String TESTROMPATH_PROPERTY = "testRomPath";
    private static final String CHARSETPATH_PROPERTY = "charSetPath";
    private static final String TOGGLEPOKESMESSAGE_PROPERTY = "togglePokesMessage";
    private static final String TESTROMMESSAGE_PROPERTY = "testRomMessage";
    private static final String LAUNCHGAMEMESSAGE_PROPERTY = "launchGameMessage";
    private static final String SELECTPOKESMESSAGE_PROPERTY = "selectPokesMessage";


    private String dandanatorRomPath;
    private String backgroundImagePath;
    private String testRomPath;
    private String charSetPath;
    private String togglePokesMessage;
    private String testRomMessage;
    private String launchGameMessage;
    private String selectPokesMessage;

    private static Configuration INSTANCE;

    byte[] dandanatorRom;
    byte[] backgroundImage;
    byte[] testRom;
    byte[] charSet;

    private Configuration() {}

    public static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE =  instantiate();
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
        return backgroundImagePath;
    }

    public void setBackgroundImagePath(String backgroundImagePath) {
        this.backgroundImagePath = backgroundImagePath;
        backgroundImage = null;
    }

    public String getTestRomPath() {
        return testRomPath;
    }

    public void setTestRomPath(String testRomPath) {
        this.testRomPath = testRomPath;
        testRom = null;
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
            if (backgroundImagePath != null) {
                try {
                    backgroundImage = Files.readAllBytes(Paths.get(backgroundImagePath));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Background Image from  " + backgroundImagePath, e);
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

    public byte[] getTestRom() throws IOException {
        if (testRom == null) {
            if (testRomPath != null) {
                try {
                    testRom = Files.readAllBytes(Paths.get(testRomPath));
                } catch (Exception e) {
                    LOGGER.error("Unable to load Test ROM from " + testRomPath, e);
                    testRom = Constants.getTestRom();
                }
            } else {
                testRom = Constants.getTestRom();
            }
        }
        return testRom;
    }

    public void setTestRom(byte[] testRom) {
        this.testRom = testRom;
    }

    public byte[] getCharSet() throws IOException {
        if (charSet == null) {
            if (charSetPath != null) {
                try {
                    charSet = Files.readAllBytes(Paths.get(charSetPath));
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
        return charSetPath;
    }

    public void setCharSetPath(String charSetPath) {
        this.charSetPath = charSetPath;
        charSet = null;
    }

    public String getTogglePokesMessage() {
        if (togglePokesMessage == null) {
            togglePokesMessage = Constants.DEFAULT_TOGGLEPOKESKEY_MESSAGE;
        }
        return togglePokesMessage;
    }

    public void setTogglePokesMessage(String togglePokesMessage) {
        this.togglePokesMessage = togglePokesMessage;
    }

    public String getTestRomMessage() {
        if (testRomMessage == null) {
            testRomMessage = Constants.DEFAULT_TESTROMKEY_MESSAGE;
        }
        return testRomMessage;
    }

    public void setTestRomMessage(String testRomMessage) {
        this.testRomMessage = testRomMessage;
    }

    public String getLaunchGameMessage() {
        if (launchGameMessage == null) {
            launchGameMessage = Constants.DEFAULT_LAUNCHGAME_MESSAGE;
        }
        return launchGameMessage;
    }

    public void setLaunchGameMessage(String launchGameMessage) {
        this.launchGameMessage = launchGameMessage;
    }

    public String getSelectPokesMessage() {
        if (selectPokesMessage == null) {
            selectPokesMessage = Constants.DEFAULT_SELECTPOKE_MESSAGE;
        }
        return selectPokesMessage;
    }

    public void setSelectPokesMessage(String selectPokesMessage) {
        this.selectPokesMessage = selectPokesMessage;
    }

    private static Configuration setFromProperties(Properties p, Configuration configuration) {
        configuration.setBackgroundImagePath(
                p.getProperty(BACKGROUNDIMAGEPATH_PROPERTY));
        configuration.setDandanatorRomPath(
                p.getProperty(DANDANATORROMPATH_PROPERTY));
        configuration.setTestRomPath(
                p.getProperty(TESTROMPATH_PROPERTY));
        configuration.setCharSetPath(
                p.getProperty(CHARSETPATH_PROPERTY));
        configuration.setLaunchGameMessage(
                p.getProperty(LAUNCHGAMEMESSAGE_PROPERTY));
        configuration.setSelectPokesMessage(
                p.getProperty(SELECTPOKESMESSAGE_PROPERTY));
        configuration.setTestRomMessage(
                p.getProperty(TESTROMMESSAGE_PROPERTY));
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

    synchronized private static Configuration instantiate() {
        final Configuration configuration = new Configuration();
        loadConfigurationFile().map(p -> setFromProperties(p, configuration));
        return configuration;
    }
}
