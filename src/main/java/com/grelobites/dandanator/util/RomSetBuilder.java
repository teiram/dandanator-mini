package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.AddressValue;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.Poke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;



/**
 * Created by mteira on 17/6/16.
 */
public class RomSetBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetBuilder.class);

    private static final int DANDANATOR_ROMSET_SIZE = 512 * 1024;
    private static final int GAME_SIZE = 0xc000;
    private static final int VERSION_SIZE = 32;
    private static final String DEFAULT_TESTROMKEY_MESSAGE = "Test Rom";
    private static final String DEFAULT_TOGGLEPOKESKEY_MESSAGE = "Toggle Pokes";
    private static final String DEFAULT_LAUNCHGAME_MESSAGE = "Launch Game";
    private static final String DEFAULT_SELECTPOKE_MESSAGE = "Select Pokes";
    private static final String DEFAULT_VERSION = "v3.1";
    private static final int SAVEDGAMECHUNK_SIZE = 256;
    private static final int POKE_SPACE_SIZE = 3200;
    private byte[] baseRom;
    private byte[] charSet;
    private byte[] screen;
    private byte[] customRom;
    private Collection<Game> games;
    private String testRomKeyMessage = DEFAULT_TESTROMKEY_MESSAGE;
    private String togglePokesKeyMessage = DEFAULT_TOGGLEPOKESKEY_MESSAGE;
    private String launchGameMessage = DEFAULT_LAUNCHGAME_MESSAGE;
    private String selectPokeMessage = DEFAULT_SELECTPOKE_MESSAGE;
    private String version = DEFAULT_VERSION;

    private RomSetBuilder() {}


    public static RomSetBuilder newInstance() {
        return new RomSetBuilder();
    }

    public RomSetBuilder withVersion(String version) {
        this.version = version;
        return this;
    }

    public RomSetBuilder withBaseRom(byte[] baseRom) {
        if (baseRom.length != Constants.BASEROM_SIZE) {
            throw new IllegalArgumentException("Unexpected Base ROM size: " + baseRom.length);
        }
        this.baseRom = baseRom;
        return this;
    }

    public RomSetBuilder withCustomRom(byte[] customRom) {
        if (customRom.length != Constants.SLOT_SIZE) {
            throw new IllegalArgumentException("Unexpected Custom ROM size: " + customRom.length);
        }
        this.customRom = customRom;
        return this;
    }

    public RomSetBuilder withCharSet(byte[] charSet) {
        if (charSet.length != Constants.CHARSET_SIZE) {
            throw new IllegalArgumentException("Unexpected Charset size: " + charSet.length);
        }
        this.charSet = charSet;
        return this;
    }

    public RomSetBuilder withScreen(byte[] screen) {
        //TODO: Check boundaries
        this.screen = screen;
        return this;
    }

    public RomSetBuilder withTestRomKeyMessage(String testRomKeyMessage) {
        this.testRomKeyMessage = testRomKeyMessage;
        return this;
    }

    public RomSetBuilder withTogglePokesKeyMessage(String togglePokesKeyMessage) {
        this.togglePokesKeyMessage = togglePokesKeyMessage;
        return this;
    }

    public RomSetBuilder withLaunchGameMessage(String launchGameMessage) {
        this.launchGameMessage = launchGameMessage;
        return this;
    }

    public RomSetBuilder withSelectPokeMessage(String selectPokeMessage) {
        this.selectPokeMessage = selectPokeMessage;
        return this;
    }

    public RomSetBuilder withGames(Collection<Game> games) {
        if (games.size() != Constants.MAX_SLOTS) {
            throw new IllegalArgumentException("Unexpected number of games: " + games.size());
        }
        this.games = games;
        return this;
    }

    private byte[] getBaseRom() throws IOException {
        return baseRom == null ?
            Constants.getDandanatorRom() : baseRom;
    }

    private byte[] getCharSet() throws IOException {
        return charSet == null ?
            Constants.getDefaultCharset() : charSet;
    }

    private byte[] getScreen() throws IOException {
        return screen == null ?
            Constants.getDefaultDandanatorScreen() : screen;
    }

    private byte[] getCustomRom() throws IOException {
        return customRom == null ?
                Constants.getTestRom() : customRom;
    }

    private static byte[] asNullTerminatedByteArray(String name, int arrayLength) {
        String trimmedName =
            name.length() < arrayLength ?
            name : name.substring(0, arrayLength - 1);
        byte[] result = new byte[arrayLength];
        System.arraycopy(trimmedName.getBytes(), 0, result, 0, trimmedName.length());
        result[trimmedName.length()] = 0;
        return result;
    }

    private static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        String gameName = String.format("%d%c %s", (index + 1) % Constants.MAX_SLOTS,
                game.getRom() ? 'r' : '.',
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, 33));
    }

    private static void dumpGameSnaHeader(OutputStream os, Game game) throws IOException {
        os.write(Arrays.copyOfRange(game.getData(), 0, Constants.SNA_HEADER_SIZE ));
    }

    private static int dumpGameLaunchCode(OutputStream os, Game game) throws IOException {
        ByteArrayOutputStream launchCode = new ByteArrayOutputStream(5);
        int launchCodeSize = 2; //Minimum possible size (HALT + RET)
        launchCode.write(Z80.HALT);
        byte interruptMode;
        if ((interruptMode = game.getData()[SNA.INTERRUPT_MODE]) != 1) {
            launchCode.write(Z80.IMH);
            if (interruptMode == 0) {
                launchCode.write(Z80.IM0);
            } else {
                launchCode.write(Z80.IM2);
            }
            launchCodeSize += 2;
        }
        LOGGER.debug("Interrupt mode is " + interruptMode);
        if ((game.getData()[SNA.INTERRUPT_ENABLE] & 0x04) == 0) {
            launchCode.write(Z80.DI);
            launchCodeSize++;
        }
        launchCode.write(Z80.RET);
        for (int i = launchCodeSize; i < 5; i++) {
            launchCode.write(Z80.NOP);
        }
        LOGGER.debug("LaunchCodeSize: " + launchCodeSize);
        os.write((byte) launchCodeSize);
        os.write(launchCode.toByteArray());
        return launchCodeSize;
    }

    private void dumpScreenTexts(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", testRomKeyMessage), 33));
        os.write(asNullTerminatedByteArray(String.format("P. %s", togglePokesKeyMessage), 33));
        os.write(asNullTerminatedByteArray(String.format("0. %s", launchGameMessage), 33));
        os.write(asNullTerminatedByteArray(selectPokeMessage, 33));
    }

    private static byte[] asLittleEndianWord(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)};
    }

    private static int attr2pixelOffset(int attrOffset) {
        int x = attrOffset % 0x20;
        int y = (attrOffset - x) >> 2;
        return ((((y >> 3) & 0x18) + y & 0x07) << 8)
                + ((y << 2) & 0xe0) + x;

    }

    private void dumpGameRamCodeLocation(OutputStream os, Game game, int requiredSize) throws IOException {
        byte[] gameData = game.getData();
        int attributeBaseOffset = Constants.SPECTRUM_SCREEN_SIZE + Constants.SNA_HEADER_SIZE;
        int zoneSize = 0, i = 0;
        do {
            byte value = gameData[i + attributeBaseOffset];
            //Attribute byte with pen == ink
            if ((value & 0x7) == ((value >> 3) & 0x7)) {
                zoneSize++;
            } else {
                zoneSize = 0;
            }
            i++;
        } while (i < Constants.SPECTRUM_COLORINFO_SIZE && zoneSize < requiredSize);

        if (zoneSize == requiredSize) {
            os.write(asLittleEndianWord(Constants.SPECTRUM_SCREEN_OFFSET
                    + attr2pixelOffset(i - requiredSize - 1)));
        } else {
            //Use last screen pixels
            os.write(asLittleEndianWord(Constants.SPECTRUM_SCREEN_OFFSET
                    + Constants.SPECTRUM_SCREEN_SIZE - requiredSize));
        }

    }

    private static void dumpGameSavedChunk(OutputStream os, Game game) throws IOException {
        byte[] gameData = game.getData();
        os.write(Arrays.copyOfRange(gameData, gameData.length - SAVEDGAMECHUNK_SIZE, gameData.length));
    }

    private void dumpGameTable(OutputStream os, Game game, int index) throws IOException {
        dumpGameName(os, game, index);
        dumpGameSnaHeader(os, game);
        os.write(game.getScreen() ? Constants.B_01 : Constants.B_00);
        os.write(game.getRom() ? Constants.B_10 : Constants.B_00);
        int codeSize = dumpGameLaunchCode(os, game);
        dumpGameRamCodeLocation(os, game, codeSize);
        dumpGameSavedChunk(os, game);
    }

    private int pokeRequiredSize(Game game) {
        int headerSize = 25; //Fixed size required per poke
        //Sum of all the addressValues * 3 (address + value)
        int size = game.getPokes().stream()
                .map(p -> p.getAddressValues().size() * 3).reduce(0, (a, b) -> a + b);
        return size + headerSize * game.getPokes().size();
    }

    private void dumpGamePokeData(OutputStream os, Game game) throws IOException {
        for (Poke poke: game.getPokes()) {
            os.write((byte) poke.getAddressValues().size());
            os.write(asNullTerminatedByteArray(String.format(". %s", poke.getName()), 23));
            for (AddressValue av : poke.getAddressValues()) {
                os.write(av.address());
                os.write(av.value());
            }
        }
    }

    private void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    private void dumpVersionInfo(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(version, VERSION_SIZE));
    }

    public byte[] build() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        os.write(getBaseRom(), 0, Constants.BASEROM_SIZE);
        LOGGER.debug("Dumped base ROM. Offset: " + os.size());

        os.write(getCharSet(), 0, Constants.CHARSET_SIZE);
        LOGGER.debug("Dumped charset. Offset: " + os.size());

        os.write(Arrays.copyOfRange(getScreen(), 0, 2048));
        os.write(Arrays.copyOfRange(getScreen(), Constants.SPECTRUM_SCREEN_SIZE,
                Constants.SPECTRUM_SCREEN_SIZE + 256));
        LOGGER.debug("Dumped screen. Offset: " + os.size());

        dumpScreenTexts(os);
        LOGGER.debug("Dumped TextData. Offset: " + os.size());

        os.write((byte) games.size());
        LOGGER.debug("Dumped game count. Offset: " + os.size());

        int index = 0;
        for (Game game : games) {
            dumpGameTable(os, game, index++);
        }
        LOGGER.debug("Dumped game table. Offset: " + os.size());

        int pokeStartMark = os.size(); //Mark position before start of poke zone
        for (Game game : games) {
            byte pokeCount = (byte) game.getPokes().size();
            os.write(pokeCount);
        }
        LOGGER.debug("Dumped poke main header. Offset: " + os.size());
        int basePokeAddress = os.size() + 20; //Add the address header

        for (Game game : games) {
            os.write(asLittleEndianWord(basePokeAddress));
            basePokeAddress += pokeRequiredSize(game);
        }
        LOGGER.debug("Dumped poke headers. Offset: " + os.size());
        for (Game game : games) {
            dumpGamePokeData(os, game);
        }
        fillWithValue(os, (byte) 0, POKE_SPACE_SIZE - (os.size() - pokeStartMark));
        LOGGER.debug("Dumped poke data. Offset: " + os.size());

        fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - VERSION_SIZE);
        LOGGER.debug("Dumped padding zone. Offset: " + os.size());

        dumpVersionInfo(os);
        LOGGER.debug("Dumped version info. Offset: " + os.size());

        for (Game game : games) {
            os.write(Arrays.copyOfRange(game.getData(), Constants.SNA_HEADER_SIZE,
                    Constants.SNA_HEADER_SIZE + GAME_SIZE));
            LOGGER.debug("Dumped game. Offset: " + os.size());
        }

        os.write(getCustomRom());
        LOGGER.debug("Dumped custom rom. Offset: " + os.size());

        os.flush();
        LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

        return os.toByteArray();
    }
}
