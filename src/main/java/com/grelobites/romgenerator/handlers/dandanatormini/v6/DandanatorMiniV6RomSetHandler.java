package com.grelobites.romgenerator.handlers.dandanatormini.v6;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.PlayerConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniRomSetHandlerSupport;
import com.grelobites.romgenerator.handlers.dandanatormini.ExtendedCharSet;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniRamGameCompressor;
import com.grelobites.romgenerator.handlers.dandanatormini.RomSetUtil;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatormini.view.DandanatorMiniFrameController;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.RamGameCompressor;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.util.ZxScreen;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

public class DandanatorMiniV6RomSetHandler extends DandanatorMiniRomSetHandlerSupport implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV6RomSetHandler.class);

    private static final byte[] EMPTY_CBLOCK = new byte[5];
    private static final int MAX_MENU_PAGES = 3;
    protected static final int SCREEN_THIRD_PIXEL_SIZE = 2048;
    protected static final int SCREEN_THIRD_ATTRINFO_SIZE = 256;

    private static RamGameCompressor ramGameCompressor = new DandanatorMiniRamGameCompressor();
    private DoubleProperty currentRomUsage;

    protected DandanatorMiniFrameController dandanatorMiniFrameController;
    protected Pane dandanatorMiniFrame;
    protected MenuItem exportPokesMenuItem;
    protected MenuItem importPokesMenuItem;
    protected MenuItem exportDivIdeTapMenuItem;
    protected MenuItem exportToWavsMenuItem;
    protected MenuItem exportExtraRomMenuItem;
    private BooleanProperty generationAllowedProperty = new SimpleBooleanProperty(false);


    private ZxScreen[] menuImages;
    private AnimationTimer previewUpdateTimer;
    private static final long SCREEN_UPDATE_PERIOD_NANOS = 3 * 1000000000L;

    private InvalidationListener updateImageListener =
            (c) -> updateMenuPreview();

    private InvalidationListener updateRomUsageListener =
            (c) -> updateRomUsage();

    private static void initializeMenuImages(ZxScreen[] menuImages) throws IOException {
        for (int i = 0; i < menuImages.length; i++) {
            menuImages[i] = new ZxScreen();
            updateBackgroundImage(menuImages[i]);
        }
    }

    private void refreshActiveRoms(Game removedRom) {
        LOGGER.debug("refreshActiveRoms " + removedRom);
        Collection<Game> gameList = getApplicationContext().getGameList();
        for (Game game : gameList) {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                if (removedRom == ramGame.getRom()) {
                    ramGame.setRom(DandanatorMiniConstants.INTERNAL_ROM_GAME);
                }
            }
        }
    }

    private void updateRomUsage() {
        getApplicationContext().setRomUsage(calculateRomUsage());
        getApplicationContext().setRomUsageDetail(generateRomUsageDetail());
    }

    public DandanatorMiniV6RomSetHandler() throws IOException {
        menuImages = new ZxScreen[MAX_MENU_PAGES];
        initializeMenuImages(menuImages);
        currentRomUsage = new SimpleDoubleProperty();
        previewUpdateTimer = new AnimationTimer() {
            int currentFrame = 0;
            long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > SCREEN_UPDATE_PERIOD_NANOS) {
                    if (applicationContext != null) {
                        int nextFrame;
                        int gameCount = applicationContext.getGameList().size();
                        if (gameCount > ((currentFrame + 1) * DandanatorMiniConstants.SLOT_COUNT)) {
                            nextFrame = currentFrame + 1;
                        } else {
                            nextFrame = 0;
                        }
                        if (nextFrame >= menuImages.length) {
                            LOGGER.warn("Out of bounds calculated next frame " + nextFrame);
                            nextFrame = 0;
                        }
                        applicationContext.getMenuPreview().setImage(menuImages[nextFrame]);
                        currentFrame = nextFrame;
                        lastUpdate = now;
                    }
                }
            }
        };
    }

    private static Compressor getCompressor() {
        return DandanatorMiniConfiguration.getInstance()
                .getCompressor();
    }

    private static byte[] compress(byte[]... sources) throws IOException {
        ByteArrayOutputStream target = new ByteArrayOutputStream();
        OutputStream os = getCompressor().getCompressingOutputStream(target);
        for (byte[] source : sources) {
            os.write(source);
        }
        os.close();
        return target.toByteArray();
    }

    private static byte[] getEepromLoaderCode() throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] eewriter = Util.fromInputStream(configuration.getRomsetLoaderStream());
        return compress(eewriter);
    }

    private static byte[] getEepromLoaderScreen() throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] screen = Util.fromInputStream(configuration.getScreenStream());
        return compress(screen);
    }

    private static byte[] getEepromLoader(int offset) throws IOException {
        PlayerConfiguration configuration = PlayerConfiguration.getInstance();
        byte[] screen = Util.fromInputStream(configuration.getScreenStream());
        byte[] eewriter = Util.fromInputStream(configuration.getRomsetLoaderStream());
        byte[] compressedScreen = compress(screen);
        byte[] compressedWriter = compress(eewriter);
        return ByteBuffer.allocate(2 + compressedScreen.length + compressedWriter.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(Integer.valueOf(compressedScreen.length + offset + 2).shortValue())
                .put(compressedScreen)
                .put(compressedWriter).array();
    }

    private static byte[] getGamePaddedSnaHeader(Game game) throws IOException {
        byte[] paddedHeader = new byte[V6Constants.SNA_HEADER_SIZE];
        Arrays.fill(paddedHeader, Constants.B_00);
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            GameHeaderV6Serializer.serialize(ramGame, os);
            byte[] snaHeader = os.toByteArray();
            System.arraycopy(snaHeader, 0, paddedHeader, 0, snaHeader.length);
        }
        return paddedHeader;
    }

    private static int getAnyRetCodeLocation(RamGame game) {
        int location = GameUtil.findInGameRam(game, Z80Opcode.RET);
        LOGGER.debug("Found RET opcode at offset 0x" + Integer.toHexString(location));
        if (location < 0) {
            byte[] screenSlot = game.getSlot(0);
            location = ImageUtil.getHiddenDisplayOffset(screenSlot, 1)
                    .orElse(Constants.SPECTRUM_SCREEN_SIZE - 1);
            LOGGER.debug("Injecting RET opcode at screen offset 0x" + Integer.toHexString(location));
            screenSlot[location] = Z80Opcode.RET;
            location += Constants.SLOT_SIZE;
        }
        LOGGER.debug("RET location calculated as 0x" + Integer.toHexString(location));
        return location;
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;

            int baseAddress = V6Constants.GAME_STRUCT_OFFSET + V6Constants.GAME_STRUCT_SIZE * index;
            int retLocation = getAnyRetCodeLocation(ramGame);
            os.write(Z80Opcode.LD_IX_NN(baseAddress + SNAHeader.REG_IX));
            os.write(Z80Opcode.LD_SP_NN(baseAddress + SNAHeader.REG_SP));
            os.write(Z80Opcode.LD_NN_A(0));
            boolean interruptDisable = (ramGame.getGameHeader().getInterruptEnable() & 0x04) == 0;

            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(interruptDisable ? Z80Opcode.DI : Z80Opcode.EI);
            os.write(Z80Opcode.JP_NN(retLocation));

        } else {
            os.write(new byte[V6Constants.GAME_LAUNCH_SIZE]);
        }
        return V6Constants.GAME_LAUNCH_SIZE;
    }

    private int dumpUncompressedGameCBlocks(OutputStream os, Game game, int offset)
            throws IOException {
        LOGGER.debug("Writing CBlocks for uncompressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();

        for (int i = 0; i < game.getSlotCount(); i++) {
            if (!game.isSlotZeroed(i)) {
                byte[] block = game.getSlot(i);
                offset -= Constants.SLOT_SIZE;
                LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                gameCBlocks.write(offset / Constants.SLOT_SIZE);
                gameCBlocks.write(asLittleEndianWord(Constants.B_00)); //Blocks always at offset 0 (uncompressed)
                //The chunk slot reports its size subtracting the chunk size (we are dumping the whole slot though)
                gameCBlocks.write(asLittleEndianWord(i == DandanatorMiniConstants.GAME_CHUNK_SLOT ?
                        Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE : Constants.SLOT_SIZE));
            } else {
                LOGGER.debug("Writing empty CBlock");
                gameCBlocks.write(EMPTY_CBLOCK);
            }
        }
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorMiniConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    private int dumpCompressedGameCBlocks(OutputStream os, Game game, int offset)
            throws IOException {
        LOGGER.debug("Writing CBlocks for compressed game " + game.getName()
                + ", of type " + game.getType()
                + ", at offset " + offset);
        ByteArrayOutputStream gameCBlocks = new ByteArrayOutputStream();
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            List<byte[]> compressedBlocks = ramGame.getCompressedData(ramGameCompressor);
            for (byte[] block : compressedBlocks) {
                if (block != null) {
                    LOGGER.debug("Writing CBlock with offset " + offset + " and length " + block.length);
                    gameCBlocks.write(offset / Constants.SLOT_SIZE);
                    gameCBlocks.write(asLittleEndianWord(offset % Constants.SLOT_SIZE));
                    gameCBlocks.write(asLittleEndianWord(block.length));
                    offset += block.length;
                } else {
                    LOGGER.debug("Writing empty CBlock");
                    gameCBlocks.write(EMPTY_CBLOCK);
                }
            }
        } else {
            throw new IllegalArgumentException("Cannot extract compressed blocks from a non-RAM game");
        }
        //Fill the remaining space with 0xFF
        byte[] cBlocksArray = Util.paddedByteArray(gameCBlocks.toByteArray(), 5 * 8, (byte) DandanatorMiniConstants.FILLER_BYTE);
        LOGGER.debug("CBlocks array calculated as " + Util.dumpAsHexString(cBlocksArray));
        os.write(cBlocksArray);
        return offset;
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        int gameSymbolCode = getGameSymbolCode(game);
        String gameName = String.format("%1d%c%c%s", (index + 1) % DandanatorMiniConstants.SLOT_COUNT,
                gameSymbolCode, gameSymbolCode + 1,
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorMiniConstants.GAMENAME_SIZE));
    }

    private static int getGameHardwareMode(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getHardwareMode().intValue();
        } else {
            return 0;
        }
    }

    private int getActiveRomSlot(RamGame game) {
        int romSlot;

        if (game.getRom() == DandanatorMiniConstants.INTERNAL_ROM_GAME) {
            romSlot = 33;
        } else if (game.getRom() == DandanatorMiniConstants.EXTRA_ROM_GAME) {
            romSlot =  32;
        } else {
            int activeRomIndex = getApplicationContext().getGameList().filtered(g -> g.getType().equals(GameType.ROM))
                    .indexOf(game.getRom());
            romSlot = 31 - activeRomIndex;
        }
        LOGGER.debug("Calculated ROM slot as " + romSlot + " for ROM " + game.getRom());
        return romSlot;
    }

    private int dumpGameHeader(OutputStream os, int index, Game game,
                               GameChunk gameChunk, int offset) throws IOException {
        os.write(getGamePaddedSnaHeader(game));
        dumpGameName(os, game, index);
        os.write(getGameHardwareMode(game));
        os.write(isGameCompressed(game) ? Constants.B_01 : Constants.B_00);
        os.write(game.getType().typeId());
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(game instanceof RamGame ? getActiveRomSlot((RamGame) game) : Constants.B_00);
        dumpGameLaunchCode(os, game, index);
        os.write(asLittleEndianWord(gameChunk.getAddress()));
        os.write(asLittleEndianWord(gameChunk.getData().length));
        return isGameCompressed(game) ?
                dumpCompressedGameCBlocks(os, game, offset) :
                dumpUncompressedGameCBlocks(os, game, offset);
    }

    private void dumpGameHeaders(ByteArrayOutputStream os, GameChunk[] gameChunkTable) throws IOException {
        int index = 0;
        //forwardOffset after the slot zero
        int forwardOffset = Constants.SLOT_SIZE + DandanatorMiniConstants.SLOT1_RESERVED_SIZE;
        //backwardsOffset starts before the test ROM
        int backwardsOffset = Constants.SLOT_SIZE * (DandanatorMiniConstants.GAME_SLOTS + 1);
        for (Game game : getApplicationContext().getGameList()) {
            if (isGameCompressed(game)) {
                forwardOffset = dumpGameHeader(os, index, game, gameChunkTable[index], forwardOffset);
            } else {
                backwardsOffset = dumpGameHeader(os, index, game, gameChunkTable[index], backwardsOffset);
            }
            LOGGER.debug("Dumped gamestruct for " + game.getName() + ". Offset: " + os.size());
            index++;
        }
        fillWithValue(os, (byte) 0, V6Constants.GAME_STRUCT_SIZE * (DandanatorMiniConstants.MAX_GAMES - index));
        LOGGER.debug("Filled to end of gamestruct. Offset: " + os.size());
    }

    private static byte[] getScreenThirdSection(byte[] fullScreen) {
        byte[] result = new byte[Constants.SPECTRUM_FULLSCREEN_SIZE];
        System.arraycopy(fullScreen, 0, result, 0, SCREEN_THIRD_PIXEL_SIZE);
        System.arraycopy(fullScreen, Constants.SPECTRUM_SCREEN_SIZE, result, Constants.SPECTRUM_SCREEN_SIZE,
                SCREEN_THIRD_ATTRINFO_SIZE);
        return result;
    }

    private static byte[] getScreenTexts(DandanatorMiniConfiguration configuration) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            dumpScreenTexts(os, configuration);
            return os.toByteArray();
        }
    }

    private static byte[] getPokeStructureData(Collection<Game> games) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            for (Game game : games) {
                os.write(getGamePokeCount(game));
            }
            fillWithValue(os, Constants.B_00, DandanatorMiniConstants.MAX_GAMES - games.size());

            int basePokeAddress = DandanatorMiniConstants.POKE_TARGET_ADDRESS +
                    DandanatorMiniConstants.MAX_GAMES * 3;

            for (Game game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            fillWithValue(os, Constants.B_00, (DandanatorMiniConstants.MAX_GAMES - games.size()) * 2);

            for (Game game : games) {
                dumpGamePokeData(os, game);
            }
            LOGGER.debug("Poke Structure before compressing: " + Util.dumpAsHexString(os.toByteArray()));
            return os.toByteArray();

        }
    }

    private static GameChunk getCompressedGameChunk(RamGame game, int cBlockOffset) throws IOException {
        try (ByteArrayOutputStream compressedChunk = new ByteArrayOutputStream()) {
            OutputStream compressingOs = getCompressor().getCompressingOutputStream(compressedChunk);
            compressingOs.write(game.getSlot(DandanatorMiniConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE,
                    DandanatorMiniConstants.GAME_CHUNK_SIZE);
            compressingOs.flush();
            byte[] compressedData = compressedChunk.toByteArray();
            if (compressedData.length > (DandanatorMiniConstants.GAME_CHUNK_SIZE - 6)) {
                LOGGER.debug("Compressed chunk for " + game.getName() + " exceeds boundaries");
                return getUncompressedGameChunk(game, cBlockOffset);
            } else {
                GameChunk gameChunk = new GameChunk();
                gameChunk.setAddress(cBlockOffset);
                gameChunk.setData(compressedData);
                LOGGER.debug("Compressed chunk for game " + game.getName() + " calculated offset " +
                    gameChunk.getAddress());
                return gameChunk;
            }
        }
    }

    private static GameChunk getUncompressedGameChunk(Game game, int cBlockOffset) throws IOException {
        GameChunk gameChunk = new GameChunk();
        if (game instanceof RamGame) {
            gameChunk.setData(Arrays.copyOfRange(game.getSlot(DandanatorMiniConstants.GAME_CHUNK_SLOT),
                    Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE,
                    Constants.SLOT_SIZE));
            gameChunk.setAddress(cBlockOffset);
        } else if (game instanceof RomGame) {
            gameChunk.setData(new byte[0]);
            gameChunk.setAddress(cBlockOffset);
        }
        LOGGER.debug("Uncompressed chunk for game " + game.getName() + " calculated offset " +
                gameChunk.getAddress());
        return gameChunk;
    }

    private static GameChunk[] calculateGameChunkTable(Collection<Game> games, int cBlockOffset) throws IOException {
        List<GameChunk> chunkList = new ArrayList<>();
        for (Game game : games) {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                GameChunk gameChunk = ramGame.getCompressed() ?
                        getCompressedGameChunk(ramGame, cBlockOffset) :
                        getUncompressedGameChunk(game, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            } else {
                GameChunk gameChunk = getUncompressedGameChunk(game, cBlockOffset);
                cBlockOffset += gameChunk.getData().length;
                chunkList.add(gameChunk);
            }
        }
        return chunkList.toArray(new GameChunk[0]);
    }

    private void dumpCompressedGameData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            for (byte[] compressedSlot : ramGame.getCompressedData(ramGameCompressor)) {
                if (compressedSlot != null) {
                    os.write(compressedSlot);
                    LOGGER.debug("Dumped compressed slot for game " + ramGame.getName()
                            + " of size: " + compressedSlot.length);
                } else {
                    LOGGER.debug("Skipped zeroed slot");
                }
            }
        }
    }

    private void dumpUncompressedGameData(OutputStream os, Game game) throws IOException {
        for (int i = game.getSlotCount() - 1; i >= 0; i--) {
            if (!game.isSlotZeroed(i)) {
                os.write(game.getSlot(i));
                LOGGER.debug("Dumped uncompressed slot " + i + " for game " + game.getName());
            } else {
                LOGGER.debug("Skipped zeroed slot");
            }
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            List<Game> games = getApplicationContext().getGameList();
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            int cblocksOffset = V6Constants.GREY_ZONE_OFFSET;
            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            byte[] compressedScreen = compress(getScreenThirdSection(configuration.getBackgroundImage()));
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreen.length));
            cblocksOffset += compressedScreen.length;

            byte[] compressedScreenTexts = compress(getScreenTexts(dmConfiguration));
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreenTexts.length));
            cblocksOffset += compressedScreenTexts.length;

            byte[] compressedPokeData = compress(getPokeStructureData(games));
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedPokeData.length));
            cblocksOffset += compressedPokeData.length;

            ExtendedCharSet extendedCharset = new ExtendedCharSet(configuration.getCharSet());
            byte[] compressedCharSetAndFirmware = compress(extendedCharset.getCharSet(),
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.getBytes(),
                    dmConfiguration.getDandanatorPicFirmware());
            cBlocksTable.write(asLittleEndianWord(cblocksOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharSetAndFirmware.length));
            cblocksOffset += compressedCharSetAndFirmware.length;

            GameChunk[] gameChunkTable = calculateGameChunkTable(games, cblocksOffset);
            dumpGameHeaders(os, gameChunkTable);
            LOGGER.debug("Dumped game struct. Offset: " + os.size());

            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedPokeData);
            os.write(compressedCharSetAndFirmware);

            for (GameChunk gameChunk : gameChunkTable) {
                os.write(gameChunk.getData());
                LOGGER.debug("Dumped game chunk. Offset: " + os.size());
            }
            LOGGER.debug("Dumped all game chunks. Offset: " + os.size());

            //loader if enough room
            int freeSpace = V6Constants.VERSION_OFFSET - os.size();
            byte[] eepromLoaderCode = getEepromLoaderCode();
            byte[] eepromLoaderScreen = getEepromLoaderScreen();
            int requiredEepromLoaderSpace = eepromLoaderCode.length + eepromLoaderScreen.length;
            int eepromLocation = 0;
            if (requiredEepromLoaderSpace <= freeSpace) {
                eepromLocation = os.size();
                LOGGER.debug("Dumping EEPROM Loader with size " + requiredEepromLoaderSpace
                        + " at offset " + eepromLocation + ". Free space was " + freeSpace);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderScreen);
                cBlocksTable.write(asLittleEndianWord(os.size()));
                os.write(eepromLoaderCode);
            } else {
                LOGGER.debug("Skipping EEPROM Loader. Not enough free space: " +
                        freeSpace + ". Needed: " + requiredEepromLoaderSpace);
                cBlocksTable.write(asLittleEndianWord(0));
                cBlocksTable.write(asLittleEndianWord(0));
            }
            fillWithValue(os, (byte) 0, V6Constants.VERSION_OFFSET - os.size());
            LOGGER.debug("Dumped compressed data. Offset: " + os.size());

            os.write(asNullTerminatedByteArray(getVersionInfo(), V6Constants.VERSION_SIZE));
            LOGGER.debug("Dumped version info. Offset: " + os.size());

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table. Offset " + os.size());

            os.write(dmConfiguration.isDisableBorderEffect() ? 1 : 0);
            LOGGER.debug("Dumped border effect flag. Offset: " + os.size());

            os.write(dmConfiguration.isAutoboot() ? 1 : 0);
            LOGGER.debug("Dumped autoboot configuration. Offset: " + os.size());

            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size());

            LOGGER.debug("Slot zero completed. Offset: " + os.size());

            byte[] slot1Rom = DandanatorMiniConstants.getSlot1Rom();
            os.write(slot1Rom);
            LOGGER.debug("Slot one header completed. Offset: " + os.size());

            for (Game game : games) {
                if (isGameCompressed(game)) {
                    dumpCompressedGameData(os, game);
                    LOGGER.debug("Dumped compressed game. Offset: " + os.size());
                }
            }

            ByteArrayOutputStream uncompressedStream = new ByteArrayOutputStream();
            for (int i = games.size() - 1; i >= 0; i--) {
                Game game = games.get(i);
                if (!isGameCompressed(game)) {
                    dumpUncompressedGameData(uncompressedStream, game);
                }
            }

            //Uncompressed data goes at the end minus the extra ROM size
            //and grows backwards
            int uncompressedOffset = Constants.SLOT_SIZE * (DandanatorMiniConstants.GAME_SLOTS + 1)
                    - uncompressedStream.size();
            int gapSize = uncompressedOffset - os.size();
            LOGGER.debug("Gap to uncompressed zone: " + gapSize);
            fillWithValue(os, Constants.B_00, gapSize);

            os.write(uncompressedStream.toByteArray());
            LOGGER.debug("Dumped uncompressed game data. Offset: " + os.size());

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    private static int getGameSize(Game game) throws IOException {
        if (game.getType() == GameType.ROM) {
            return game.getSlotCount() * Constants.SLOT_SIZE;
        } else {
            if (game instanceof RamGame) {
                RamGame ramGame = (RamGame) game;
                //Calculate compression always here, to avoid locking the GUI later
                int compressedSize = ramGame.getCompressedSize(ramGameCompressor);
                return ramGame.getCompressed() ? compressedSize : ramGame.getSize();
            } else {
                throw new IllegalArgumentException("Unable to calculate size for game " + game);
            }
        }
    }

    protected BooleanBinding getGenerationAllowedBinding(ApplicationContext ctx) {
        return Bindings.size(ctx.getGameList())
                .greaterThan(0)
                .and(Bindings.size(ctx.getGameList()).lessThanOrEqualTo(DandanatorMiniConstants.MAX_GAMES))
                .and(currentRomUsage.lessThan(1.0));
    }

    protected double calculateRomUsage() {
        int size = DandanatorMiniConstants.SLOT1_RESERVED_SIZE;
        for (Game game : getApplicationContext().getGameList()) {
            try {
                size += getGameSize(game);
            } catch (Exception e) {
                LOGGER.warn("Calculating game size usage", e);
            }
        }
        LOGGER.debug("Used size: " + size + ", total size: "
                + DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE);
        currentRomUsage.set(((double) size /
                (DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE)));
        return currentRomUsage.get();
    }

    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.DDNTR_V6;
    }

    protected String generateRomUsageDetail() {
        return String.format(LocaleUtil.i18n("romUsageV5Detail"),
                getApplicationContext().getGameList().size(),
                DandanatorMiniConstants.MAX_GAMES,
                calculateRomUsage() * 100);
    }

    private void prepareAddedGame(Game game) throws IOException {
        getGameSize(game);
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            if (ramGame.getRom() == null) {
                ramGame.setRom(DandanatorMiniConstants.INTERNAL_ROM_GAME);
            }
        }
    }

    @Override
    public Future<OperationResult> addGame(Game game) {
        return getApplicationContext().addBackgroundTask(() -> {
                try {
                    //Force compression calculation
                    prepareAddedGame(game);
                    Platform.runLater(() -> getApplicationContext().getGameList().add(game));
                } catch (Exception e) {
                    LOGGER.error("Calculating game size", e);
                }
            return OperationResult.successResult();
        });
    }

    @Override
    public void removeGame(Game game) {
        if (game.getType() == GameType.ROM) {
            refreshActiveRoms(game);
        }
        getApplicationContext().getGameList().remove(game);
    }

    private static void printVersionAndPageInfo(ZxScreen screen, int line, int page, int numPages) {
        String versionInfo = getVersionInfo();
        screen.setInk(ZxColor.BLACK);
        screen.setPen(ZxColor.BRIGHTMAGENTA);
        screen.printLine(versionInfo, line, 0);
        if (numPages > 1) {
            screen.setPen(ZxColor.WHITE);
            String pageInfo = numPages > 1 ?
                    String.format("%d/%d", page, numPages) : "";
            String keyInfo = "SPC - ";
            screen.printLine(keyInfo, line, screen.getColumns() - pageInfo.length() - keyInfo.length());
            screen.setPen(ZxColor.YELLOW);
            screen.printLine(pageInfo, line, screen.getColumns() - pageInfo.length());
        }
    }

    private static int getGameSymbolCode(Game game) {
        if (game instanceof RamGame) {
            switch (((RamGame) game).getHardwareMode()) {
                case HW_48K:
                case HW_48K_IF1:
                case HW_48K_MGT:
                    return ExtendedCharSet.SYMBOL_48K_0_CODE;
                case HW_128K:
                case HW_128K_IF1:
                case HW_128K_MGT:
                case HW_PLUS2:
                    return ExtendedCharSet.SYMBOL_128K_0_CODE;
                case HW_PLUS2A:
                case HW_PLUS3:
                    return ExtendedCharSet.SYMBOL_PLUS2A_0_CODE;
                default:
                    LOGGER.error("Unable to get symbol for hardware mode in game " + game);
                    return ExtendedCharSet.SYMBOL_SPACE;
            }
        } else {
            switch (game.getType()) {
                case ROM:
                    return ExtendedCharSet.SYMBOL_ROM_0_CODE;
                case RAM16:
                    return ExtendedCharSet.SYMBOL_16K_0_CODE;
                default:
                    LOGGER.error("Unable to get a symbol for game " + game);
                    return ExtendedCharSet.SYMBOL_SPACE;
            }
        }
    }

    private static void printGameNameLine(ZxScreen screen, Game game, int index, int line) {
        screen.setPen(
                isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
        screen.deleteLine(line);
        screen.printLine(String.format("%1d", (index + 1) % DandanatorMiniConstants.SLOT_COUNT), line, 0);
        screen.setPen(ZxColor.BRIGHTWHITE);
        int gameSymbolCode = getGameSymbolCode(game);
        screen.printLine(String.format("%c", gameSymbolCode), line, 1);
        if (isGameCompressed(game)) {
            screen.setPen(ZxColor.BRIGHTYELLOW);
        }
        screen.printLine(String.format("%c", gameSymbolCode + 1), line, 2);
        screen.setPen(isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
        screen.printLine(
                String.format("%s", game.getName()), line, 3);
    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        DandanatorMiniConfiguration configuration = DandanatorMiniConfiguration.getInstance();
        ZxScreen page = menuImages[pageIndex];
        updateBackgroundImage(page);
        page.setCharSet(new ExtendedCharSet(Configuration.getInstance().getCharSet()).getCharSet());

        page.setInk(ZxColor.BLACK);
        page.setPen(ZxColor.BRIGHTMAGENTA);
        for (int line = page.getLines() - 1; line >= 8; line--) {
            page.deleteLine(line);
        }

        printVersionAndPageInfo(page, 8, pageIndex + 1, numPages);
        int line = 10;
        int gameIndex = pageIndex * DandanatorMiniConstants.SLOT_COUNT;
        int gameCount = 0;
        while (gameIndex < gameList.size() && gameCount < DandanatorMiniConstants.SLOT_COUNT) {
            Game game = gameList.get(gameIndex);
            printGameNameLine(page, game, gameCount++, line++);
            gameIndex++;
        }

        page.setPen(ZxColor.BRIGHTWHITE);
        page.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
        page.setPen(ZxColor.BRIGHTRED);
        page.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
    }

    protected MenuItem getExportPokesMenuItem() {
        if (exportPokesMenuItem == null) {
            exportPokesMenuItem = new MenuItem(LocaleUtil.i18n("exportPokesMenuEntry"));

            exportPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+P")
            );
            exportPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            exportPokesMenuItem.setOnAction(f -> {
                try {
                    exportCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Exporting current game pokes", e);
                }
            });
        }
        return exportPokesMenuItem;
    }


    protected MenuItem getImportPokesMenuItem() {
        if (importPokesMenuItem == null) {
            importPokesMenuItem = new MenuItem(LocaleUtil.i18n("importPokesMenuEntry"));

            importPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+L")
            );
            importPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            importPokesMenuItem.setOnAction(f -> {
                try {
                    importCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Importing current game pokes", e);
                }
            });
        }
        return importPokesMenuItem;
    }

    private MenuItem getExportExtraRomMenuItem() {
        if (exportExtraRomMenuItem == null) {
            exportExtraRomMenuItem = new MenuItem(LocaleUtil.i18n("exportExtraRomMenuEntry"));
            exportExtraRomMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+E")
            );

            exportExtraRomMenuItem.setOnAction(f -> {
                try {
                    exportExtraRom();
                } catch (Exception e) {
                    LOGGER.error("Exporting extra Rom", e);
                }
            });
        }
        return exportExtraRomMenuItem;
    }

    public void exportDivIdeTapToFile() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportDivIdeTapMenuEntry"));
        chooser.setInitialFileName("dandanator_divide_romset.tap");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                exportRomSet(bos);
                RomSetUtil.exportToDivideAsTap(new ByteArrayInputStream(bos.toByteArray()), fos);
            } catch (IOException e) {
                LOGGER.error("Exporting to DivIDE TAP", e);
            }
        }
    }

    private void exportExtraRom() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportExtraRomMenuEntry"));
        chooser.setInitialFileName("dandanator_extra_rom.rom");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                fos.write(DandanatorMiniConfiguration.getInstance().getExtraRom());
            } catch (IOException e) {
                LOGGER.error("Exporting Extra ROM", e);
            }
        }
    }

    protected MenuItem getExportDivIdeTapMenuItem() {
        if (exportDivIdeTapMenuItem == null) {
            exportDivIdeTapMenuItem = new MenuItem(LocaleUtil.i18n("exportDivIdeTapMenuEntry"));

            exportDivIdeTapMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+D")
            );
            exportDivIdeTapMenuItem.disableProperty().bind(generationAllowedProperty.not());

            exportDivIdeTapMenuItem.setOnAction(f -> exportDivIdeTapToFile());
        }
        return exportDivIdeTapMenuItem;
    }

    protected MenuItem getExportToWavsMenuItem() {
        if (exportToWavsMenuItem == null) {
            exportToWavsMenuItem = new MenuItem(LocaleUtil.i18n("exportToWavsMenuEntry"));

            exportToWavsMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+W")
            );
            exportToWavsMenuItem.disableProperty().bind(generationAllowedProperty.not());

            exportToWavsMenuItem.setOnAction(f -> exportToWavs());
        }
        return exportToWavsMenuItem;
    }

    public void exportToWavs() {
        DirectoryAwareFileChooser chooser = applicationContext.getFileChooser();
        chooser.setTitle(LocaleUtil.i18n("exportToWavsMenuEntry"));
        chooser.setInitialFileName("dandanator_wav_romset.zip");
        final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
        if (saveFile != null) {
            try (FileOutputStream fos = new FileOutputStream(saveFile)) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                exportRomSet(bos);
                RomSetUtil.exportToZippedWavFiles(new ByteArrayInputStream(bos.toByteArray()), fos);
            } catch (IOException e) {
                LOGGER.error("Exporting to Wavs", e);
            }
        }
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            List<Game> gameList = getApplicationContext().getGameList();
            int numPages = 1 + ((gameList.size() - 1) / DandanatorMiniConstants.SLOT_COUNT);
            for (int i = 0; i < numPages; i++) {
                updateMenuPage(gameList, i, numPages);
            }
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    protected DandanatorMiniFrameController getDandanatorMiniFrameController(ApplicationContext applicationContext) {
        if (dandanatorMiniFrameController == null) {
            dandanatorMiniFrameController = new DandanatorMiniFrameController();
        }
        dandanatorMiniFrameController.setApplicationContext(applicationContext);
        return dandanatorMiniFrameController;
    }

    protected Pane getDandanatorMiniFrame(ApplicationContext applicationContext) {
        try {
            if (dandanatorMiniFrame == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(DandanatorMiniFrameController.class.getResource("dandanatorminiframe.fxml"));
                loader.setController(getDandanatorMiniFrameController(applicationContext));
                loader.setResources(LocaleUtil.getBundle());
                dandanatorMiniFrame = loader.load();
            } else {
                dandanatorMiniFrameController.setApplicationContext(applicationContext);
            }
            return dandanatorMiniFrame;
        } catch (Exception e) {
            LOGGER.error("Creating DandanatorMini frame", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public BooleanProperty generationAllowedProperty() {
        return generationAllowedProperty;
    }

    @Override
    public void bind(ApplicationContext applicationContext) {
        LOGGER.debug("Binding RomSetHandler to ApplicationContext");
        this.applicationContext = applicationContext;
        generationAllowedProperty.bind(getGenerationAllowedBinding(applicationContext));

        applicationContext.getRomSetHandlerInfoPane().getChildren()
                .add(getDandanatorMiniFrame(applicationContext));
        updateMenuPreview();

        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .addListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().backgroundImagePathProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().charSetPathProperty()
                .addListener(updateImageListener);

        applicationContext.getGameList().addListener(updateImageListener);
        applicationContext.getGameList().addListener(updateRomUsageListener);

        applicationContext.getExtraMenu().getItems().addAll(
                getExportPokesMenuItem(), getImportPokesMenuItem(),
                getExportDivIdeTapMenuItem(), getExportExtraRomMenuItem(),
                getExportToWavsMenuItem());

        updateRomUsage();
        previewUpdateTimer.start();
        Configuration.getInstance().setRamGameCompressor(ramGameCompressor);

    }

    @Override
    public void unbind() {
        LOGGER.debug("Unbinding RomSetHandler from ApplicationContext");
        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .removeListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .removeListener(updateImageListener);
        generationAllowedProperty.unbind();
        generationAllowedProperty.set(false);
        applicationContext.getRomSetHandlerInfoPane().getChildren().clear();

        applicationContext.getExtraMenu().getItems().removeAll(
                getExportPokesMenuItem(),
                getImportPokesMenuItem(),
                getExportDivIdeTapMenuItem(),
                getExportExtraRomMenuItem(),
                getExportToWavsMenuItem());
        applicationContext.getGameList().removeListener(updateImageListener);
        applicationContext.getGameList().removeListener(updateRomUsageListener);
        applicationContext = null;
        previewUpdateTimer.stop();
    }
}