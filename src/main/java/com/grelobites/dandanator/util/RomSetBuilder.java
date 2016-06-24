package com.grelobites.dandanator.util;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.Poke;
import com.grelobites.dandanator.model.PokeViewable;
import com.grelobites.dandanator.model.Trainer;
import com.grelobites.dandanator.model.TrainerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;


public class RomSetBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RomSetBuilder.class);

    private static final int DANDANATOR_ROMSET_SIZE = 512 * 1024;
    static final int GAME_SIZE = 0xc000;
    private static final int VERSION_SIZE = 32;

    private static final int SAVEDGAMECHUNK_SIZE = 256;
    private static final int POKE_SPACE_SIZE = 3200;
    private static final int RESERVED_GAMETABLE_SIZE = 64;
    private byte[] baseRom;
    private byte[] charSet;
    private byte[] screen;
    private byte[] customRom;
    private Collection<Game> games;
    private String testRomKeyMessage;
    private String togglePokesKeyMessage;
    private String launchGameMessage;
    private String selectPokeMessage;
    private String version;

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
        if (games.size() != Constants.SLOT_COUNT) {
            throw new IllegalArgumentException("Unexpected number of games: " + games.size());
        }
        this.games = games;
        return this;
    }

    private byte[] getBaseRom() throws IOException {
        return baseRom == null ?
                Configuration.getInstance().getDandanatorRom() : baseRom;
    }

    private byte[] getCharSet() throws IOException {
        return charSet == null ?
                Configuration.getInstance().getCharSet() : charSet;
    }

    private byte[] getScreen() throws IOException {
        return screen == null ?
                Configuration.getInstance().getBackgroundImage() : screen;
    }

    private byte[] getCustomRom() throws IOException {
        return customRom == null ?
                Configuration.getInstance().getTestRom() : customRom;
    }

    private String getTestRomKeyMessage() {
        return  testRomKeyMessage == null ?
                Configuration.getInstance().getTestRomMessage() : testRomKeyMessage;
    }

    private String getTogglePokesKeyMessage() {
        return togglePokesKeyMessage == null ?
                Configuration.getInstance().getTogglePokesMessage() : togglePokesKeyMessage;
    }

    private String getLaunchGameMessage() {
        return launchGameMessage == null ?
                Configuration.getInstance().getLaunchGameMessage() : launchGameMessage;
    }

    private String getSelectPokeMessage() {
        return selectPokeMessage == null ?
                Configuration.getInstance().getSelectPokesMessage() : selectPokeMessage;
    }

    private String getVersion() {
        return version == null ?
                Constants.currentVersion() : version;
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
        String gameName = String.format("%d%c %s", (index + 1) % Constants.SLOT_COUNT,
                game.getRom() ? 'r' : '.',
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, 33));
    }

    private static void dumpGameSnaHeader(OutputStream os, Game game) throws IOException {
        os.write(Arrays.copyOfRange(game.getData(), 0, Constants.SNA_HEADER_SIZE ));
    }

    private static int dumpGameLaunchCode(OutputStream os, Game game) throws IOException {
        os.write(Z80.PUSH_HL);
        os.write(Z80.POP_HL);
        os.write(Z80.PUSH_HL);
        os.write(Z80.POP_HL);
        os.write((game.getData()[SNAHeader.INTERRUPT_ENABLE] & 0x04) == 0 ?
                Z80.DI : Z80.EI);
        os.write(Z80.RET);
        return 6;
    }

    private void dumpScreenTexts(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", getTestRomKeyMessage()), 33));
        os.write(asNullTerminatedByteArray(String.format("P. %s", getTogglePokesKeyMessage()), 33));
        os.write(asNullTerminatedByteArray(String.format("0. %s", getLaunchGameMessage()), 33));
        os.write(asNullTerminatedByteArray(getSelectPokeMessage(), 33));
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
        fillWithValue(os, (byte) 0, RESERVED_GAMETABLE_SIZE);
        dumpGameSavedChunk(os, game);
    }

    private int pokeRequiredSize(Game game) {
        int headerSize = 25; //Fixed size required per poke
        //Sum of all the addressValues * 3 (address + value)
        int size = game.getTrainerList().getChildren().stream()
                .map(p -> p.getChildren().size() * 3).reduce(0, (a, b) -> a + b);
        return size + headerSize * game.getTrainerList().getChildren().size();
    }

    private void dumpGamePokeData(OutputStream os, Game game) throws IOException {
        int index = 1;
        for (PokeViewable trainer: game.getTrainerList().getChildren()) {
            os.write((byte) trainer.getChildren().size());
            os.write(asNullTerminatedByteArray(String.format("%d. %s",
                    index++, trainer.getViewRepresentation()), 24));
            for (PokeViewable viewable : trainer.getChildren()) {
                Poke poke = (Poke) viewable;
                os.write(poke.addressBytes());
                os.write(poke.valueBytes());
            }
        }
    }

    private void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    private void dumpVersionInfo(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(getVersion(), VERSION_SIZE));
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
            byte pokeCount = (byte) game.getTrainerList().getChildren().size();
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

    public static void importFromStream(Collection<Game> gameList, InputStream is) throws IOException {
        is.skip(Constants.BASEROM_SIZE);
        is.skip(Constants.CHARSET_SIZE);
        is.skip(2048);  //Screen third
        is.skip(256);   //Attributes
        is.skip(132);   //Text data
        is.skip(1);     //Game count
        ArrayList<GameDataHolder> recoveredGames = new ArrayList<>();
        //SNA Headers and flags
        for (int i = 0; i < Constants.SLOT_COUNT; i++) {
            GameDataHolder gameData = new GameDataHolder();
            gameData.readName(is);
            gameData.readHeader(is);
            gameData.setHoldScreen(is.read());
            gameData.setActiveRom(is.read());
            recoveredGames.add(gameData);
            is.skip(6 + 2 + 64 + 256); //Skip launchCode + RAMAddr + Reserved + Saved Chunk
        }
        //Poke area
        for (int i = 0; i < Constants.SLOT_COUNT; i++) {
            GameDataHolder holder = recoveredGames.get(i);
            holder.setTrainerCount(is.read());
        }
        is.skip(20); //Skip poke start addresses

        for (int i = 0; i < Constants.SLOT_COUNT; i++) {
            GameDataHolder holder = recoveredGames.get(i);
            int trainerCount = holder.getTrainerCount();
            if (trainerCount > 0) {
                int pokeCount = is.read();
                if (pokeCount > 0) {
                    String trainerName = Util.getNullTerminatedString(is, 24);
                    Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent()) {
                        for (int j = 0; j < pokeCount; j++) {
                            int address = Util.asLittleEndian(is);
                            int value = is.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }

        //If we reached this far, we have all the data and it's safe to replace the game list
        gameList.clear();
        recoveredGames.stream()
                .map(holder -> {
                    Game game = new Game();
                    game.setName(holder.name);
                    game.setScreen(holder.holdScreen);
                    game.setRom(holder.activeRom);
                    game.setData(holder.getData());
                    holder.exportTrainers(game);
                    gameList.add(game);
                    return true;
                });

    }

    private static class GameDataHolder {
        private boolean holdScreen;
        private boolean activeRom;
        private String name;
        private byte[] snaHeader;
        private byte[] gameData;
        private TrainerList trainerList = new TrainerList(null);
        private int trainerCount = 0;

        void setHoldScreen(int holdScreenByte) {
            holdScreen = holdScreenByte != 0;
        }

        void setActiveRom(int activeRomByte) {
            activeRom = activeRomByte != 0;
        }

        void readHeader(InputStream is) throws IOException {
            this.snaHeader = new byte[Constants.SNA_HEADER_SIZE];
            is.read(snaHeader);
        }

        void readGameData(InputStream is) throws IOException {
            gameData = new byte[RomSetBuilder.GAME_SIZE];
            is.read(gameData);
        }

        void readName(InputStream is) throws IOException {
            name = Util.getNullTerminatedString(is, 33);
        }

        TrainerList getTrainerList() {
            return trainerList;
        }

        public int getTrainerCount() {
            return trainerCount;
        }

        public void setTrainerCount(int trainerCount) {
            this.trainerCount = trainerCount;
        }

        public byte[] getData() {
            if (snaHeader != null && gameData != null) {
                byte[] data = new byte[Constants.SNA_HEADER_SIZE + GAME_SIZE];
                Arrays.copyOfRange(snaHeader, 0, Constants.SNA_HEADER_SIZE);
                Arrays.copyOfRange(gameData, 0, GAME_SIZE + Constants.SNA_HEADER_SIZE);
                return data;
            } else {
                throw new IllegalStateException("Either SNA Header or Game Image not set");
            }
        }

        public void exportTrainers(Game game) {
            trainerList.setOwner(game);
            game.setTrainerList(trainerList);
        }
    }
}



