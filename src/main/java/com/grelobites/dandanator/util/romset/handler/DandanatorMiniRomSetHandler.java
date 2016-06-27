package com.grelobites.dandanator.util.romset.handler;

import com.grelobites.dandanator.Configuration;
import com.grelobites.dandanator.Constants;
import com.grelobites.dandanator.Context;
import com.grelobites.dandanator.model.Game;
import com.grelobites.dandanator.model.Poke;
import com.grelobites.dandanator.model.PokeViewable;
import com.grelobites.dandanator.model.Trainer;
import com.grelobites.dandanator.model.TrainerList;
import com.grelobites.dandanator.util.SNAHeader;
import com.grelobites.dandanator.util.TrackeableInputStream;
import com.grelobites.dandanator.util.Util;
import com.grelobites.dandanator.util.Z80Opcode;
import com.grelobites.dandanator.util.ZxColor;
import com.grelobites.dandanator.util.ZxScreen;
import com.grelobites.dandanator.util.romset.RomSetHandler;
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

public class DandanatorMiniRomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniRomSetHandler.class);

    private static final int DANDANATOR_ROMSET_SIZE = 512 * 1024;
    static final int GAME_SIZE = 0xc000;
    private static final int VERSION_SIZE = 32;

    private static final int SAVEDGAMECHUNK_SIZE = 256;
    private static final int POKE_SPACE_SIZE = 3200;
    private static final int RESERVED_GAMETABLE_SIZE = 64;

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
        os.write(Z80Opcode.PUSH_HL);
        os.write(Z80Opcode.POP_HL);
        os.write(Z80Opcode.PUSH_HL);
        os.write(Z80Opcode.POP_HL);
        os.write((game.getData()[SNAHeader.INTERRUPT_ENABLE] & 0x04) == 0 ?
                Z80Opcode.DI : Z80Opcode.EI);
        os.write(Z80Opcode.RET);
        return 6;
    }

    private void dumpScreenTexts(OutputStream os, Configuration configuration) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", configuration.getTestRomMessage()), 33));
        os.write(asNullTerminatedByteArray(String.format("P. %s", configuration.getTogglePokesMessage()), 33));
        os.write(asNullTerminatedByteArray(String.format("0. %s", configuration.getLaunchGameMessage()), 33));
        os.write(asNullTerminatedByteArray(configuration.getSelectPokesMessage(), 33));
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
        os.write(asNullTerminatedByteArray(Constants.currentVersion(), VERSION_SIZE));
    }

    @Override
    public void createRomSet(Context context, OutputStream stream) {
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Configuration configuration = context.getConfiguration();
            Collection<Game> games = context.getGameList();
            os.write(configuration.getDandanatorRom(), 0, Constants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write(configuration.getCharSet(), 0, Constants.CHARSET_SIZE);
            LOGGER.debug("Dumped charset. Offset: " + os.size());

            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), 0, 2048));
            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), Constants.SPECTRUM_SCREEN_SIZE,
                    Constants.SPECTRUM_SCREEN_SIZE + 256));
            LOGGER.debug("Dumped screen. Offset: " + os.size());

            dumpScreenTexts(os, configuration);
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

            os.write(configuration.getTestRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    @Override
    public void importRomSet(Context context, InputStream stream) {
        try {
            TrackeableInputStream is = new TrackeableInputStream(stream);
            is.skip(Constants.BASEROM_SIZE);
            is.skip(Constants.CHARSET_SIZE);
            is.skip(2048);  //Screen third
            is.skip(256);   //Attributes
            is.skip(132);   //Text data
            is.skip(1);     //Game count
            LOGGER.debug("Skipped head. Position is " + is.position());

            ArrayList<GameDataHolder> recoveredGames = new ArrayList<>();
            //SNA Headers and flags
            for (int i = 0; i < Constants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading data from game " + i + " from position " + is.position());
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
                LOGGER.debug("Reading poke data for game " + i + " from position " + is.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.setTrainerCount(is.read());
            }
            is.skip(20); //Skip poke start addresses

            for (int i = 0; i < Constants.SLOT_COUNT; i++) {
                GameDataHolder holder = recoveredGames.get(i);
                int trainerCount = holder.getTrainerCount();
                if (trainerCount > 0) {
                    LOGGER.debug("Importing " + trainerCount + " trainers");
                    for (int j = 0; j < trainerCount; j++) {
                        int pokeCount = is.read();
                        if (pokeCount > 0) {
                            LOGGER.debug("Importing " + pokeCount + "pokes");
                            String trainerName = Util.getNullTerminatedString(is, 3, 24);
                            Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                            if (trainer.isPresent()) {
                                for (int k = 0; k < pokeCount; k++) {
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
            }

            is.skip(Constants.SLOT_SIZE - is.position());

            LOGGER.debug("After version. Position " + is.position());

            for (int i = 0; i < Constants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading game " + i + " data from " + is.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.readGameData(is);
            }
            //If we reached this far, we have all the data and it's safe to replace the game list
            LOGGER.debug("Clearing game list with recovered games count " + recoveredGames.size());
            Collection<Game> games = context.getGameList();
            games.clear();
            recoveredGames.stream()
                    .forEach(holder -> {
                        final Game game = new Game();
                        game.setName(holder.name);
                        game.setScreen(holder.holdScreen);
                        game.setRom(holder.activeRom);
                        game.setData(holder.getData());
                        holder.exportTrainers(game);
                        games.add(game);
                    });

            LOGGER.debug("Added " + games.size() + " to the list of games");
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    @Override
    public void updateScreen(Context context, ZxScreen screen) {
        LOGGER.debug("updateScreen");
        try {
            Configuration configuration = context.getConfiguration();
            screen.setCharSet(configuration.getCharSet());

            screen.setInk(ZxColor.BLACK);
            screen.setPen(ZxColor.BRIGHTMAGENTA);
            screen.printLine(Constants.currentVersion(), 8, 0);

            int line = 10;
            int index = 1;

            for (Game game : context.getGameList()) {
                screen.setPen(
                        game.getScreen() ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
                screen.deleteLine(line);
                screen.printLine(
                        String.format("%d%c %s", index % Constants.SLOT_COUNT,
                                game.getRom() ? 'r' : '.',
                                game.getName()),
                        line++, 0);
                index++;
            }
            while (index <= Constants.SLOT_COUNT) {
                screen.deleteLine(line);
                screen.setPen(ZxColor.WHITE);
                screen.printLine(String
                        .format("%d.", index % Constants.SLOT_COUNT), line++, 0);
                index++;
            }

            screen.setPen(ZxColor.BRIGHTBLUE);
            screen.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
            screen.setPen(ZxColor.BRIGHTRED);
            screen.printLine(String.format("R. %s", configuration.getTestRomMessage()), 23, 0);
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
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
            gameData = new byte[DandanatorMiniRomSetHandler.GAME_SIZE];
            is.read(gameData);
        }

        void readName(InputStream is) throws IOException {
            name = Util.getNullTerminatedString(is, 3, 33);
            LOGGER.debug("Name read as " + name);
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
                System.arraycopy(snaHeader, 0, data, 0, Constants.SNA_HEADER_SIZE);
                System.arraycopy(gameData, 0, data, Constants.SNA_HEADER_SIZE, GAME_SIZE);
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
