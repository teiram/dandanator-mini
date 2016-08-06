package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.Poke;
import com.grelobites.romgenerator.model.PokeViewable;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.TrackeableInputStream;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.util.ZxScreen;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.view.MainAppController;
import javafx.beans.value.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DandanatorMiniRomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniRomSetHandler.class);

    private static final int DANDANATOR_ROMSET_SIZE = 512 * 1024;
    static final int GAME_SIZE = 0xc000;
    protected static final int VERSION_SIZE = 32;

    private static final int SAVEDGAMECHUNK_SIZE = 256;
    private static final int POKE_SPACE_SIZE = 3230;
    private static final int RESERVED_GAMETABLE_SIZE = 64;

    protected static final int SCREEN_THIRD_PIXEL_SIZE = 2048;
    protected static final int SCREEN_THIRD_ATTRINFO_SIZE = 256;

    private ZxScreen menuImage;
    protected MainAppController controller;
    private ChangeListener<? super String> updateImageListener =
            (observable, oldValue, newValue) -> updateMenuPreview();

    public DandanatorMiniRomSetHandler() throws IOException {
        initializeImages();
    }

    private void initializeImages() throws IOException {
        menuImage = ImageUtil.scrLoader(
                new ZxScreen(),
                new ByteArrayInputStream(Configuration.getInstance()
                        .getBackgroundImage()));
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

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        String gameName = String.format("%d%c %s", (index + 1) % DandanatorMiniConstants.SLOT_COUNT,
                isGameRom(game) ? 'r' : '.',
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorMiniConstants.GAMENAME_SIZE));
    }

    private static void dumpGameSnaHeader(OutputStream os, RamGame game) throws IOException {
        os.write(Arrays.copyOfRange(game.getSnaHeader().asByteArray(), 0, Constants.SNA_HEADER_SIZE));
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            if (game.getType() == GameType.RAM48) {
                RamGame ramGame = (RamGame) game;

                os.write(Z80Opcode.PUSH_HL);
                os.write(Z80Opcode.POP_HL);
                os.write(Z80Opcode.PUSH_HL);
                os.write(Z80Opcode.POP_HL);
                os.write((ramGame.getSnaHeader().asByteArray()[SNAHeader.INTERRUPT_ENABLE] & 0x04) == 0 ?
                        Z80Opcode.DI : Z80Opcode.EI);
                os.write(Z80Opcode.RET);
                return 6;
            } else {
                throw new IllegalArgumentException("Not implemented yet");
            }
        } else {
            throw new IllegalStateException("Unsupported game type");
        }
    }

    protected static void dumpScreenTexts(OutputStream os, DandanatorMiniConfiguration configuration) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", configuration.getExtraRomMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("P. %s", configuration.getTogglePokesMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("0. %s", configuration.getLaunchGameMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(configuration.getSelectPokesMessage(), DandanatorMiniConstants.GAMENAME_SIZE));
    }


    protected static byte[] asLittleEndianWord(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)};
    }

    private static int attr2pixelOffset(int attrOffset) {
        int x = attrOffset % 0x20;
        int y = (attrOffset - x) >> 2;
        return ((((y >> 3) & 0x18) + y & 0x07) << 8)
                + ((y << 2) & 0xe0) + x;

    }

    protected static void dumpGameRamCodeLocation(OutputStream os, Game game, int requiredSize) throws IOException {
        byte[] screenData = game.getSlot(0); //Screen slot
        int attributeBaseOffset = Constants.SPECTRUM_SCREEN_SIZE + Constants.SNA_HEADER_SIZE;
        int zoneSize = 0, i = 0;
        do {
            byte value = screenData[i + attributeBaseOffset];
            //Attribute byte with pen == ink
            if ((value & 0x7) == ((value >> 3) & 0x7)) {
                zoneSize++;
            } else {
                zoneSize = 0;
            }
            i++;
        } while (i < Constants.SPECTRUM_COLORINFO_SIZE && zoneSize < requiredSize);

        int ramAddress = Constants.SPECTRUM_SCREEN_OFFSET;
        if (zoneSize == requiredSize) {
            ramAddress += attr2pixelOffset(i - requiredSize);
        } else {
            //Use last screen pixels
            LOGGER.debug("Using last screen pixels for RAM Address");
            ramAddress += Constants.SPECTRUM_SCREEN_SIZE - requiredSize;
        }
        os.write(asLittleEndianWord(ramAddress));
        LOGGER.debug(String.format("RAM Address calculated as 0x%04X", ramAddress));
    }

    private static void dumpGameSavedChunk(OutputStream os, Game game) throws IOException {
        byte[] lastSlot = game.getSlot(game.getSlotCount() - 1);
        os.write(Arrays.copyOfRange(lastSlot, lastSlot.length - SAVEDGAMECHUNK_SIZE, lastSlot.length));
    }

    private void dumpGameTable(OutputStream os, RamGame game, int index) throws IOException {
        dumpGameName(os, game, index);
        dumpGameSnaHeader(os, game);
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(isGameRom(game) ? Constants.B_10 : Constants.B_00);
        int codeSize = dumpGameLaunchCode(os, game);
        dumpGameRamCodeLocation(os, game, codeSize);
        fillWithValue(os, (byte) 0, RESERVED_GAMETABLE_SIZE);
        dumpGameSavedChunk(os, game);
    }

    protected static int pokeRequiredSize(Game game) {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            int headerSize = 25; //Fixed size required per poke
            //Sum of all the addressValues * 3 (address + value)
            int size = ramGame.getTrainerList().getChildren().stream()
                    .map(p -> p.getChildren().size() * 3).reduce(0, (a, b) -> a + b);
            return size + headerSize * ramGame.getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static int getGamePokeCount(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static void dumpGamePokeData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            int index = 1;
            for (PokeViewable trainer : ramGame.getTrainerList().getChildren()) {
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
    }

    private String getVersionInfo() {
        return String.format("v%s", Util.stripSnapshotVersion(Constants.currentVersion()));
    }

    protected void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    protected void dumpVersionInfo(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(getVersionInfo(), VERSION_SIZE));
    }

    private void dumpGameData(OutputStream os, Game game) throws IOException {
        for (int i = 0; i < 3; i++) {
            os.write(game.getSlot(i));
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            //Only RamGame type supported in this RomSetHandler
            Collection<RamGame> games = Util.collectionUpcast(controller.getGameList());
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            int firmwareHeaderLength = DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.length();
            os.write(DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.getBytes(), 0, firmwareHeaderLength);
            os.write(dmConfiguration.getDandanatorPicFirmware(), 0,
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0);
            LOGGER.debug("Dumped first chunk of PIC firmware. Offset: " + os.size());

            os.write(configuration.getCharSet(), 0, Constants.CHARSET_SIZE);
            LOGGER.debug("Dumped charset. Offset: " + os.size());

            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), 0, SCREEN_THIRD_PIXEL_SIZE));
            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), Constants.SPECTRUM_SCREEN_SIZE,
                    Constants.SPECTRUM_SCREEN_SIZE + SCREEN_THIRD_ATTRINFO_SIZE));
            LOGGER.debug("Dumped screen. Offset: " + os.size());

            dumpScreenTexts(os, dmConfiguration);
            LOGGER.debug("Dumped TextData. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            int index = 0;
            for (RamGame game : games) {
                dumpGameTable(os, game, index++);
            }
            LOGGER.debug("Dumped game table. Offset: " + os.size());

            int pokeStartMark = os.size(); //Mark position before start of poke zone
            for (RamGame game : games) {
                byte pokeCount = (byte) game.getTrainerList().getChildren().size();
                os.write(pokeCount);
            }
            LOGGER.debug("Dumped poke main header. Offset: " + os.size());
            int basePokeAddress = os.size() + 20; //Add the address header

            for (RamGame game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            LOGGER.debug("Dumped poke headers. Offset: " + os.size());
            for (RamGame game : games) {
                dumpGamePokeData(os, game);
            }
            fillWithValue(os, (byte) 0, POKE_SPACE_SIZE - (os.size() - pokeStartMark));
            LOGGER.debug("Dumped poke data. Offset: " + os.size());

            os.write(dmConfiguration.getDandanatorPicFirmware(),
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0,
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_1);
            LOGGER.debug("Dumped second chunk of PIC firmware. Offset: " + os.size());

            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - VERSION_SIZE);
            LOGGER.debug("Dumped padding zone. Offset: " + os.size());

            dumpVersionInfo(os);
            LOGGER.debug("Dumped version info. Offset: " + os.size());

            for (Game game : games) {
                dumpGameData(os, game);
                LOGGER.debug("Dumped game. Offset: " + os.size());
            }

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    @Override
    public void importRomSet(InputStream stream) {
        try {
            byte[] dandanatorPicFirmware = new byte[DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE];
            TrackeableInputStream is = new TrackeableInputStream(stream);
            byte[] baseRom = is.getAsByteArray(DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("After reading the base rom. Offset " + is.position());
            is.skip(DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.length());
            is.read(dandanatorPicFirmware, 0, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0);
            LOGGER.debug("After reading 1st section of PIC firmware. Offset " + is.position());

            byte[] charSet = is.getAsByteArray(Constants.CHARSET_SIZE);
            LOGGER.debug("After reading the charset. Offset " + is.position());
            byte[] screen = is.getAsByteArray(SCREEN_THIRD_PIXEL_SIZE);
            byte[] attributes = is.getAsByteArray(SCREEN_THIRD_ATTRINFO_SIZE);
            LOGGER.debug("After reading the screen. Offset " + is.position());

            String extraRomMessage = is.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String togglePokesMessage = is.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String launchGameMessage = is.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String selectPokesMessage = is.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE);

            is.skip(1);     //Game count
            LOGGER.debug("Skipped head. Position is " + is.position());

            ArrayList<GameDataHolder> recoveredGames = new ArrayList<>();
            //SNA Headers and flags
            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
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
            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading poke data for game " + i + " from position " + is.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.setTrainerCount(is.read());
            }
            long beforePokesPosition = is.position();
            is.skip(20); //Skip poke start addresses

            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                GameDataHolder holder = recoveredGames.get(i);
                int trainerCount = holder.getTrainerCount();
                if (trainerCount > 0) {
                    LOGGER.debug("Importing " + trainerCount + " trainers");
                    for (int j = 0; j < trainerCount; j++) {
                        int pokeCount = is.read();
                        String trainerName = Util.getNullTerminatedString(is, 3, 24);
                        Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                        if (trainer.isPresent() && pokeCount > 0) {
                            LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
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
            is.skip(DandanatorMiniConstants.POKE_ZONE_SIZE - (is.position() - beforePokesPosition));
            is.read(dandanatorPicFirmware, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_1);

            LOGGER.debug("After pic firmware. Position " + is.position());

            is.skip(Constants.SLOT_SIZE - is.position());
            LOGGER.debug("After version. Position " + is.position());

            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading game " + i + " data from " + is.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.readGameSlots(is);
            }
            //If we reached this far, we have all the data and it's safe to replace the game list
            LOGGER.debug("Clearing game list with recovered games count " + recoveredGames.size());
            Collection<Game> games = controller.getGameList();
            games.clear();
            recoveredGames.forEach(holder -> {
                final RamGame game = new RamGame(GameType.RAM48, holder.getGameSlots());
                game.setName(holder.name);
                game.setHoldScreen(holder.holdScreen);
                game.setRom(holder.activeRom);
                holder.exportTrainers(game);
                games.add(game);
            });

            LOGGER.debug("Added " + games.size() + " to the list of games");

            byte[] extraRom = is.getAsByteArray(Constants.SLOT_SIZE);

            //Update preferences only if everything was OK
            Configuration globalConfiguration = Configuration.getInstance();
            DandanatorMiniConfiguration dandanatorMiniConfiguration = DandanatorMiniConfiguration.getInstance();

            //Keep this order, first the image and then the path, to avoid listeners to
            //enter before the image is set
            dandanatorMiniConfiguration.setDandanatorRom(baseRom);
            dandanatorMiniConfiguration.setDandanatorRomPath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setCharSet(charSet);
            globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setDandanatorPicFirmware(dandanatorPicFirmware);
            dandanatorMiniConfiguration.setDandanatorPicFirmwarePath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setBackgroundImage(ImageUtil.fillZxImage(screen, attributes));
            globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRom(extraRom);
            dandanatorMiniConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRomMessage(extraRomMessage);
            dandanatorMiniConfiguration.setTogglePokesMessage(togglePokesMessage);
            dandanatorMiniConfiguration.setLaunchGameMessage(launchGameMessage);
            dandanatorMiniConfiguration.setSelectPokesMessage(selectPokesMessage);
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    public void bind(MainAppController controller) {
        this.controller = controller;
        updateMenuPreview();
        controller.getMenuPreviewImage().setImage(menuImage);
        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .addListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .addListener(updateImageListener);
    }

    public void unbind() {
        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .removeListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .removeListener(updateImageListener);
    }


    protected static boolean isGameScreenHold(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getHoldScreen();
        } else {
            return false;
        }
    }

    protected static boolean isGameRom(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getRom();
        } else {
            return false;
        }
    }

    protected static boolean isGameCompressed(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getCompressed();
        } else {
            return false;
        }
    }

    protected static TrainerList gameTrainers(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getTrainerList();
        } else {
            return TrainerList.EMPTY_LIST;
        }
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            DandanatorMiniConfiguration configuration = DandanatorMiniConfiguration.getInstance();
            menuImage.setCharSet(Configuration.getInstance().getCharSet());

            menuImage.setInk(ZxColor.BLACK);
            menuImage.setPen(ZxColor.BRIGHTMAGENTA);
            for (int line = menuImage.getLines() - 1; line >= 8; line--) {
                menuImage.deleteLine(line);
            }

            menuImage.printLine(getVersionInfo(), 8, 0);

            int line = 10;
            int index = 1;

            for (Game game : controller.getGameList()) {
                menuImage.setPen(
                        isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
                menuImage.deleteLine(line);
                menuImage.printLine(
                        String.format("%d%c %s", index % DandanatorMiniConstants.SLOT_COUNT,
                                isGameRom(game) ? 'r' : '.',
                                game.getName()),
                        line++, 0);
                index++;
            }
            while (index <= DandanatorMiniConstants.SLOT_COUNT) {
                menuImage.deleteLine(line);
                menuImage.setPen(ZxColor.WHITE);
                menuImage.printLine(String
                        .format("%d.", index % DandanatorMiniConstants.SLOT_COUNT), line++, 0);
                index++;
            }

            menuImage.setPen(ZxColor.BRIGHTBLUE);
            menuImage.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
            menuImage.setPen(ZxColor.BRIGHTRED);
            menuImage.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    @Override
    public boolean addGame(Game game) {
        if (game.getType() == GameType.RAM48) {
            if (game instanceof RamGame) {
                if (controller.getGameList().size() < DandanatorMiniConstants.SLOT_COUNT) {
                    controller.getGameList().add(game);
                    return true;
                }
            } else {
                LOGGER.warn("Non RAM48 games are not supported by this RomSethandler");
            }
        }
        return false;
    }

    private static class GameDataHolder {
        private boolean holdScreen;
        private boolean activeRom;
        private String name;
        private byte[] snaHeader;
        private List<byte[]> gameSlots;
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

        void readGameSlots(InputStream is) throws IOException {
            gameSlots = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                gameSlots.add(Util.fromInputStream(is, Constants.SLOT_SIZE));
            }
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

        public List<byte[]> getGameSlots() {
            if (gameSlots != null) {
                return gameSlots;
            } else {
                throw new IllegalStateException("Game slots not set");
            }
        }

        public byte[] getSnaHeader() {
            if (snaHeader != null) {
                return snaHeader;
            } else {
                throw new IllegalStateException("SNA Header not set");
            }
        }

        public void exportTrainers(RamGame game) {
            trainerList.setOwner(game);
            game.setTrainerList(trainerList);
        }
    }
}

