package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameBlock;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameMapper;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameMapperV5 implements GameMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapperV5.class);

    private static final int COMPRESSED_SLOT_MAXSIZE = Constants.SLOT_SIZE;
    private static final int COMPRESSED_CHUNKSLOT_MAXSIZE = Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE;
    private static final int INVALID_SLOT_ID = DandanatorMiniConstants.FILLER_BYTE;

    private GameHeader gameHeader;
    private String name;
    private boolean isGameCompressed;
    private boolean isGameForce48kMode;
    private HardwareMode hardwareMode;
    private GameType gameType;
    private boolean screenHold;
    private boolean activeRom;
    private byte[] launchCode;
    private GameChunk gameChunk;
    private List<GameBlock> blocks = new ArrayList<>();
    private TrainerList trainerList = new TrainerList(null);
    private int trainerCount;
    private Game game;

    private static boolean isSlotCompressed(int slotIndex, int size) {
        return size > 0 && ((slotIndex != DandanatorMiniConstants.GAME_CHUNK_SLOT) ?
                size < COMPRESSED_SLOT_MAXSIZE :
                size < COMPRESSED_CHUNKSLOT_MAXSIZE);
    }

    public static GameMapperV5 fromRomSet(PositionAwareInputStream is, int minorVersion) throws IOException {
        LOGGER.debug("About to read game data. Offset is " + is.position());
        GameMapperV5 mapper = new GameMapperV5();
        mapper.gameHeader = GameHeaderV5Serializer.deserialize(is);
        mapper.name = Util.getNullTerminatedString(is, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        if (minorVersion < 3) {
            mapper.isGameForce48kMode = is.read() != 0;
        } else {
            mapper.isGameForce48kMode = (mapper.gameHeader.getPort7ffdValue(0) &
                    DandanatorMiniConstants.PORT7FFD_FORCED_48KMODE_BITS) != 0;
            mapper.hardwareMode = HardwareMode.fromIntValueMode(is.read());
        }
        mapper.isGameCompressed = is.read() != 0;
        int gameType = is.read();
        mapper.gameType = GameType.byTypeId(gameType);
        if (minorVersion < 3) {
            mapper.hardwareMode = gameType > 3 ? HardwareMode.HW_PLUS2A : HardwareMode.HW_48K;
            //Reset 1FFD (TRDOS is stored here in 5.n n<3 versions)
            mapper.gameHeader.setPort1ffdValue(null);
        } else {
            mapper.gameHeader.setPort1ffdValue(
                    GameUtil.resetNonAuthentic(mapper.gameHeader.getPort1ffdValue(0)));
            //Zero values makes no sense and could have been injected by some erroneous ROM version
            if (GameUtil.decodeAsAuthentic(mapper.gameHeader.getPort1ffdValue(0)) == 0) {
                mapper.gameHeader.setPort1ffdValue(null);
            }
            mapper.gameHeader.setPort7ffdValue(
                    GameUtil.resetNonAuthentic(mapper.gameHeader.getPort7ffdValue(0)));
        }
        mapper.screenHold = is.read() != 0;
        mapper.activeRom = is.read() != 0;
        mapper.launchCode = Util.fromInputStream(is, DandanatorMiniConstants.GAME_LAUNCH_SIZE);
        mapper.gameChunk = new GameChunk();
        mapper.gameChunk.setAddress(is.getAsLittleEndian());
        mapper.gameChunk.setLength(is.getAsLittleEndian());
        for (int i = 0; i < 8; i++) {
            GameBlock block = new GameBlock();
            block.setInitSlot(is.read());
            block.setStart(is.getAsLittleEndian());
            block.setSize(is.getAsLittleEndian());
            block.setCompressed(mapper.isGameCompressed && isSlotCompressed(i, block.getSize()));
            if (block.getInitSlot() < INVALID_SLOT_ID) {
                LOGGER.debug("Read block for game " + mapper.name + ": " + block);
                mapper.getBlocks().add(block);
            }
        }
        LOGGER.debug("Read game data. Offset is " + is.position());
        return mapper;
    }

    public TrainerList getTrainerList() {
        return trainerList;
    }

    public List<GameBlock> getBlocks() {
        return blocks;
    }

    public GameChunk getGameChunk() {
        return gameChunk;
    }

    public byte[] getLaunchCode() {
        return launchCode;
    }

    public void setTrainerCount(int trainerCount) {
        this.trainerCount = trainerCount;
    }

    private List<byte[]> getGameSlots() {
        List<byte[]> gameSlots = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            LOGGER.debug("Adding game slot for game " + name + ": " + block);
            if (index == DandanatorMiniConstants.GAME_CHUNK_SLOT) {
                gameSlots.add(Util.concatArrays(block.getData(), gameChunk.getData()));
            } else {
                gameSlots.add(block.getData());
            }
        }
        return gameSlots;
    }

    private List<byte[]> getGameCompressedData() {
        List<byte[]> compressedData = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            compressedData.add(block.rawdata);
        }
        return compressedData;
    }

    public int getTrainerCount() {
        return trainerCount;
    }

    @Override
    public GameType getGameType() {
        return gameType;
    }

    @Override
    public Game getGame() {
        if (game == null) {
            switch (gameType) {
                case ROM:
                    game = new RomGame(getGameSlots().get(0));
                    break;
                case RAM16:
                case RAM48:
                case RAM128:
                    RamGame ramGame = new RamGame(gameType, getGameSlots());
                    ramGame.setCompressed(isGameCompressed);
                    ramGame.setHoldScreen(screenHold);
                    ramGame.setRom(activeRom ? DandanatorMiniConstants.EXTRA_ROM_GAME :
                            DandanatorMiniConstants.INTERNAL_ROM_GAME);
                    ramGame.setGameHeader(gameHeader);
                    ramGame.setForce48kMode(isGameForce48kMode);
                    ramGame.setHardwareMode(hardwareMode);
                    ramGame.setTrainerList(trainerList);
                    ramGame.setCompressedData(getGameCompressedData());
                    //Extract the PC from SP
                    ramGame.getGameHeader().setPCRegister(GameUtil.popPC(ramGame));
                    GameUtil.pushPC(ramGame);
                    game = ramGame;
                    break;
                default:
                    LOGGER.error("Unsupported type of game " + gameType.screenName());
                    throw new IllegalArgumentException("Unsupported game type");
            }
            game.setName(name);
        }
        return game;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        //Actually made in SlotZeroV5 since it keeps track of the GameBlocks and
        //it needs to take them in order
        throw new IllegalStateException("Unsupported slot population method in V5");

    }
}
