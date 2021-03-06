package com.grelobites.romgenerator.handlers.dandanatormini.v6;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameChunk;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameMapper;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameBlock;
import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GameMapperV6 implements GameMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameMapperV6.class);

    private static final int COMPRESSED_SLOT_MAXSIZE = Constants.SLOT_SIZE;
    private static final int COMPRESSED_CHUNKSLOT_MAXSIZE = Constants.SLOT_SIZE - DandanatorMiniConstants.GAME_CHUNK_SIZE;
    private static final int INVALID_SLOT_ID = DandanatorMiniConstants.FILLER_BYTE;

    private SlotZeroV6 slotZero;
    private GameHeader gameHeader;
    private String name;
    private boolean isGameCompressed;
    private boolean isGameForce48kMode;
    private HardwareMode hardwareMode;
    private GameType gameType;
    private boolean screenHold;
    private int activeRom;
    private byte[] launchCode;
    private GameChunk gameChunk;
    private List<GameBlock> blocks = new ArrayList<>();
    private TrainerList trainerList = new TrainerList(null);
    private int trainerCount;
    private Game game;

    private GameMapperV6(SlotZeroV6 slotZero) {
        this.slotZero = slotZero;
    }

    private static boolean isSlotCompressed(int slotIndex, int size) {
        return size > 0 && ((slotIndex != DandanatorMiniConstants.GAME_CHUNK_SLOT) ?
                size < COMPRESSED_SLOT_MAXSIZE :
                size < COMPRESSED_CHUNKSLOT_MAXSIZE);
    }

    public static GameMapperV6 fromRomSet(PositionAwareInputStream is, SlotZeroV6 slotZero) throws IOException {
        LOGGER.debug("About to read game data. Offset is " + is.position());
        GameMapperV6 mapper = new GameMapperV6(slotZero);
        mapper.gameHeader = GameHeaderV6Serializer.deserialize(is);
        mapper.name = Util.getNullTerminatedString(is, 3, DandanatorMiniConstants.GAMENAME_SIZE);

        mapper.isGameForce48kMode = (mapper.gameHeader.getPort7ffdValue(0) &
                    DandanatorMiniConstants.PORT7FFD_FORCED_48KMODE_BITS) != 0;
        mapper.hardwareMode = HardwareMode.fromIntValueMode(is.read());

        //Reset 7FFD/1FFD values on no authenticity bit
        mapper.gameHeader.setPort1ffdValue(
                GameUtil.resetNonAuthentic(mapper.gameHeader.getPort1ffdValue(0)));
        mapper.gameHeader.setPort7ffdValue(
                GameUtil.resetNonAuthentic(mapper.gameHeader.getPort7ffdValue(0)));

        //Zero values makes no sense and could have been injected by some erroneous ROM version
        if (GameUtil.decodeAsAuthentic(mapper.gameHeader.getPort1ffdValue(0)) == 0) {
            mapper.gameHeader.setPort1ffdValue(null);
        }

        mapper.isGameCompressed = is.read() != 0;
        mapper.gameType = GameType.byTypeId(is.read());

        mapper.screenHold = is.read() != 0;

        if (slotZero.getMajorVersion() > 6 || slotZero.getMinorVersion() > 0) {
            mapper.activeRom = is.read();
        } else {
            mapper.activeRom = is.read() == 0 ? V6Constants.INTERNAL_ROM_SLOT : V6Constants.EXTRA_ROM_SLOT;
        }
        mapper.launchCode = Util.fromInputStream(is, V6Constants.GAME_LAUNCH_SIZE);
        mapper.gameChunk = new GameChunk();
        mapper.gameChunk.setAddress(is.getAsLittleEndian());
        mapper.gameChunk.setLength(is.getAsLittleEndian());
        for (int i = 0; i < 8; i++) {
            GameBlock block = new GameBlock();
            block.setInitSlot(is.read());
            block.setStart(is.getAsLittleEndian());
            block.setSize(is.getAsLittleEndian());
            block.setGameCompressed(mapper.isGameCompressed);
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

    private List<byte[]> getMLDGameSlots() {
        List<byte[]> gameSlots = new ArrayList<>();
        for (int index = 0; index < blocks.size(); index++) {
            GameBlock block = blocks.get(index);
            int slots = block.size / Constants.SLOT_SIZE;
            for (int slot = 0; slot < slots; slot++) {
                LOGGER.debug("Adding game slot for MLD game " + name + ": " + block);
                gameSlots.add(Arrays.copyOfRange(block.data, slot * Constants.SLOT_SIZE, (slot + 1) * Constants.SLOT_SIZE));
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

    private Game getRomFromSlot(int slot) {
        LOGGER.debug("getRomFromSlot " + slot);
        Game activeRom;
        //Compatibility with 6.n n < 1
        if (slot == 0) {
            slot = V6Constants.INTERNAL_ROM_SLOT;
        }

        if (slot >= V6Constants.EXTRA_ROM_SLOT) {
            activeRom =  slot == V6Constants.EXTRA_ROM_SLOT ? DandanatorMiniConstants.EXTRA_ROM_GAME :
                    DandanatorMiniConstants.INTERNAL_ROM_GAME;
        } else {
            activeRom = slotZero.getGameMappers().stream()
                    .filter(g -> g.getGameType().equals(GameType.ROM))
                    .limit(V6Constants.EXTRA_ROM_SLOT - slot)
                    .reduce((a, b) -> b)
                    .orElseThrow(() -> new RuntimeException("Unable to find assigned ROM"))
                    .getGame();
        }
        LOGGER.debug("Calculated Active ROM as " + activeRom);
        return activeRom;
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
                    SnapshotGame snapshotGame = new SnapshotGame(gameType, getGameSlots());
                    snapshotGame.setCompressed(isGameCompressed);
                    snapshotGame.setHoldScreen(screenHold);
                    snapshotGame.setRom(getRomFromSlot(activeRom));
                    snapshotGame.setGameHeader(gameHeader);
                    snapshotGame.setForce48kMode(isGameForce48kMode);
                    snapshotGame.setHardwareMode(hardwareMode);
                    snapshotGame.setTrainerList(trainerList);
                    if (isGameCompressed) {
                        snapshotGame.setCompressedData(getGameCompressedData());
                    }
                    //Extract the PC from SP
                    snapshotGame.getGameHeader().setPCRegister(GameUtil.popPC(snapshotGame));
                    GameUtil.pushPC(snapshotGame);
                    game = snapshotGame;
                    break;
                case RAM128_MLD:
                case RAM48_MLD:
                    List<byte[]> gameSlots = getMLDGameSlots();
                    Optional<MLDInfo> mldInfo = MLDInfo.fromGameByteArray(gameSlots);
                    if (mldInfo.isPresent()) {
                        MLDGame mldGame = new MLDGame(mldInfo.get(), gameSlots);
                        game = mldGame;
                    } else {
                        LOGGER.error("Unable to restore MLDGame from ROMSet. No MLDInfo found");
                    }
                    break;
                case DAN_SNAP:
                case DAN_SNAP128:
                    gameSlots = getMLDGameSlots();
                    game = MLDInfo.fromGameByteArray(gameSlots)
                            .map(m -> new DanSnapGame(m, gameSlots))
                            .orElseGet(() -> {
                                LOGGER.error("Unable to restore DanSnap Game from ROMSet. No MLDInfo found");
                                return null;
                            });
                    break;
                default:
                    LOGGER.error("Unsupported type of game " + gameType.screenName());
                    throw new IllegalArgumentException("Unsupported game type");
            }
            if (game != null) {
                game.setName(name);
            }
        }
        LOGGER.debug("Game generated as " + game);
        return game;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        //Actually made in SlotZeroV6 since it keeps track of the GameBlocks and
        //it needs to take them in order
        throw new IllegalStateException("Unsupported slot population method in V5");

    }
}
