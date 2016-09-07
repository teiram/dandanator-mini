package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.ExtendedCharSet;
import com.grelobites.romgenerator.handlers.dandanatormini.IfromConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.IfromConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorConfigurationSetter;
import com.grelobites.romgenerator.handlers.dandanatormini.view.DandanatorMiniFrameController;
import com.grelobites.romgenerator.handlers.dandanatormini.view.IfromFrameController;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.util.ZxScreen;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public class IfromRomSetHandler extends DandanatorMiniV5RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(IfromRomSetHandler.class);

    private IfromFrameController iFromFrameController;
    private Pane iFromFrame;

    public IfromRomSetHandler() throws IOException {}

    @Override
    protected int dumpGameLaunchCode(OutputStream os, Game game, int index) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;

            int baseAddress = GAME_STRUCT_OFFSET + GAME_STRUCT_SIZE * index;
            os.write(Z80Opcode.LD_IX_NN(baseAddress + SNAHeader.REG_IX));
            os.write(Z80Opcode.LD_SP_NN(baseAddress + SNAHeader.REG_SP));
            os.write(Z80Opcode.PUSH_AF);
            boolean interruptDisable = (ramGame.getGameHeader().getInterruptEnable() & 0x04) == 0;

            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
            os.write(interruptDisable ? Z80Opcode.DI : Z80Opcode.EI);
            os.write(Z80Opcode.JP_NN(IfromConstants.ROM_RET_JUMP_LOCATION));
            os.write(Z80Opcode.NOP);
            os.write(Z80Opcode.NOP);
        } else {
            os.write(new byte[GAME_LAUNCH_SIZE]);
        }
        return GAME_LAUNCH_SIZE;
    }

    protected static void dumpScreenTexts(OutputStream os, IfromConfiguration configuration) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", configuration.getLaunchCustomRomMessage()),
                IfromConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("P. %s", configuration.getTogglePokesMessage()),
                IfromConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("0. %s", configuration.getLaunchGameMessage()),
                IfromConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(configuration.getSelectPokesMessage(), IfromConstants.GAMENAME_SIZE));
    }

    private static byte[] getScreenTexts(IfromConfiguration configuration) throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            dumpScreenTexts(os, configuration);
            return os.toByteArray();
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            IfromConfiguration ifromConfiguration = IfromConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            List<Game> games = getApplicationContext().getGameList();
            os.write(ifromConfiguration.getBaseRom(), 0, IfromConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            ByteArrayOutputStream cBlocksTable = new ByteArrayOutputStream();
            int cBlockOffset = CBLOCKS_TABLE_OFFSET + CBLOCKS_TABLE_SIZE;

            byte[] compressedScreen = compress(getScreenThirdSection(configuration.getBackgroundImage()));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreen.length));
            cBlockOffset += compressedScreen.length;

            byte[] compressedScreenTexts = compress(getScreenTexts(ifromConfiguration));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedScreenTexts.length));
            cBlockOffset += compressedScreenTexts.length;

            byte[] compressedPokeData = compress(getPokeStructureData(games));
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedPokeData.length));
            cBlockOffset += compressedPokeData.length;

            ExtendedCharSet extendedCharset = new ExtendedCharSet(configuration.getCharSet());
            byte[] compressedCharSet = compress(extendedCharset.getCharSet());
            cBlocksTable.write(asLittleEndianWord(cBlockOffset));
            cBlocksTable.write(asLittleEndianWord(compressedCharSet.length));
            cBlockOffset += compressedCharSet.length;

            GameChunk[] gameChunkTable = calculateGameChunkTable(games, cBlockOffset);

            dumpGameHeaders(os, gameChunkTable);

            os.write(cBlocksTable.toByteArray());
            LOGGER.debug("Dumped CBlocks table. Offset " + os.size());
            os.write(compressedScreen);
            os.write(compressedScreenTexts);
            os.write(compressedPokeData);
            os.write(compressedCharSet);

            LOGGER.debug("Dumped compressed data. Offset: " + os.size());

            for (GameChunk gameChunk : gameChunkTable) {
                os.write(gameChunk.getData());
                LOGGER.debug("Dumped game chunk. Offset: " + os.size());
            }
            LOGGER.debug("Dumped all game chunks. Offset: " + os.size());

            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - VERSION_SIZE);
            LOGGER.debug("Dumped padding zone. Offset: " + os.size());

            dumpVersionInfo(os);
            LOGGER.debug("Dumped version info. Offset: " + os.size());

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

            //Uncompressed data goes at the end minus the custom ROM size
            //and grows backwards
            int uncompressedOffset = Constants.SLOT_SIZE * (IfromConstants.GAME_SLOTS + 1)
                    - uncompressedStream.size();
            int gapSize = uncompressedOffset - os.size();
            LOGGER.debug("Gap to uncompressed zone: " + gapSize);
            fillWithValue(os, Constants.B_00, gapSize);

            os.write(uncompressedStream.toByteArray());
            LOGGER.debug("Dumped uncompressed game data. Offset: " + os.size());

            os.write(ifromConfiguration.getCustomRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }


    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.IFROM;
    }

    protected IfromFrameController getIfromFrameController(ApplicationContext applicationContext) {
        if (iFromFrameController == null) {
            iFromFrameController = new IfromFrameController();
        }
        iFromFrameController.setApplicationContext(applicationContext);
        return iFromFrameController;
    }

    protected Pane getIfromFrame(ApplicationContext applicationContext) {
        try {
            if (iFromFrame == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(DandanatorMiniFrameController.class.getResource("ifromframe.fxml"));
                loader.setController(getIfromFrameController(applicationContext));
                loader.setResources(LocaleUtil.getBundle());
                iFromFrame = loader.load();
            } else {
                iFromFrameController.setApplicationContext(applicationContext);
            }
            return iFromFrame;
        } catch (Exception e) {
            LOGGER.error("Creating IFrom frame", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void bindSpecificConfiguration() {
        LOGGER.debug("Binding specific configuration");
        IfromConfiguration.getInstance().togglePokesMessageProperty()
                .addListener(updateImageListener);
        IfromConfiguration.getInstance().launchCustomRomMessageProperty()
                .addListener(updateImageListener);
        getApplicationContext().getRomSetHandlerInfoPane().getChildren()
                .add(getIfromFrame(getApplicationContext()));
        try {
            Configuration.getInstance().setDefaultBackgroundImage(IfromConstants.getDefaultBackgroundImage());
        } catch (Exception e) {
            LOGGER.error("Binding default image", e);
        }
    }

    @Override
    protected void unbindSpecificConfiguration() {
        LOGGER.debug("Unbinding specific configuration");
        IfromConfiguration.getInstance().togglePokesMessageProperty()
                .removeListener(updateImageListener);
        IfromConfiguration.getInstance().launchCustomRomMessageProperty()
                .removeListener(updateImageListener);
        getApplicationContext().getRomSetHandlerInfoPane().getChildren().clear();

    }

    private void updateMenuPage(List<Game> gameList, int pageIndex, int numPages) throws IOException {
        IfromConfiguration configuration = IfromConfiguration.getInstance();
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
        int gameIndex = pageIndex * IfromConstants.SLOT_COUNT;
        int gameCount = 0;
        while (gameIndex < gameList.size() && gameCount < IfromConstants.SLOT_COUNT) {
            Game game = gameList.get(gameIndex);
            printGameNameLine(page, game, gameCount++, line++);
            gameIndex++;
        }

        page.setPen(ZxColor.BRIGHTWHITE);
        page.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
        page.setPen(ZxColor.BRIGHTRED);
        page.printLine(String.format("R. %s", configuration.getLaunchCustomRomMessage()), 23, 0);
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            List<Game> gameList = getApplicationContext().getGameList();
            int numPages = 1 + ((gameList.size() - 1) / IfromConstants.SLOT_COUNT);
            for (int i = 0; i < numPages; i++) {
                updateMenuPage(gameList, i, numPages);
            }
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    @Override
    protected DandanatorConfigurationSetter getConfigurationSetter() {
        return IfromConfiguration.getInstance();
    }


}