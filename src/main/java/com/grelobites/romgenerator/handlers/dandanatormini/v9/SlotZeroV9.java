package com.grelobites.romgenerator.handlers.dandanatormini.v9;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.*;
import com.grelobites.romgenerator.handlers.dandanatormini.v6.DandanatorMiniV6Importer;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

public class SlotZeroV9 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV9.class);
    private byte[] charSet;
    private byte[] screen;
    private byte[] screenAttributes;
    private List<GameMapperV9> gameMappers;
    private String extraRomMessage;
    private String togglePokesMessage;
    private String launchGameMessage;
    private String selectPokesMessage;
    private boolean disableBorderEffect;
    private boolean autoboot;
    List<GameBlock> gameBlocks;

    public SlotZeroV9(byte[] data) {
        super(data);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 9;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    @Override
    public void parse() throws IOException {
        PositionAwareInputStream zis = new PositionAwareInputStream(data());
        zis.safeSkip(V9Constants.BASEROM_SIZE);
        int gameCount = zis.read();
        LOGGER.debug("Read number of games: " + gameCount);
        gameMappers = new ArrayList<>();
        gameBlocks = new ArrayList<>();
        for (int i = 0; i < gameCount; i++) {
            GameMapperV9 mapper = GameMapperV9.fromRomSet(zis, this);
            gameBlocks.addAll(mapper.getBlocks());
            gameMappers.add(mapper);
        }

        zis.safeSkip(V9Constants.GAME_STRUCT_SIZE * (DandanatorMiniConstants.MAX_GAMES - gameCount));


        int compressedScreenOffset = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET);
        int compressedScreenBlocks = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 2);
        LOGGER.debug("Compressed screen located at " + compressedScreenOffset + ", blocks "
                + compressedScreenBlocks);
        int compressedTextDataOffset = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 4);
        int compressedTextDataBlocks = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 6);
        LOGGER.debug("Compressed text data located at " + compressedTextDataOffset + ", blocks "
                + compressedTextDataBlocks);
        int compressedPokeStructOffset = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 8);
        int compressedPokeStructBlocks = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 10);
        LOGGER.debug("Compressed poke data located at " + compressedPokeStructOffset + ", blocks "
                + compressedPokeStructBlocks);
        int compressedPicFwAndCharsetOffset = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 12);
        int compressedPicFwAndCharsetBlocks = Util.readAsLittleEndian(data, V9Constants.CBLOCKS_OFFSET + 14);
        LOGGER.debug("Compressed PIC FW and Charset located at " + compressedPicFwAndCharsetOffset
                + ", blocks " + compressedPicFwAndCharsetBlocks);

        screen = uncompress(zis, compressedScreenOffset, compressedScreenBlocks);
        byte[] textData = uncompress(zis, compressedTextDataOffset, compressedTextDataBlocks);
        byte[] pokeData = uncompress(zis, compressedPokeStructOffset, compressedPokeStructBlocks);
        byte[] picFwAndCharset = uncompress(zis, compressedPicFwAndCharsetOffset, compressedPicFwAndCharsetBlocks);

        ByteArrayInputStream textDataStream = new ByteArrayInputStream(textData);
        extraRomMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        togglePokesMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        launchGameMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        selectPokesMessage = Util.getNullTerminatedString(textDataStream, DandanatorMiniConstants.GAMENAME_SIZE);

        charSet = Arrays.copyOfRange(picFwAndCharset, 0, Constants.CHARSET_SIZE);

        screenAttributes = Arrays.copyOfRange(screen, Constants.SPECTRUM_SCREEN_SIZE,
                Constants.SPECTRUM_FULLSCREEN_SIZE);

        //Poke data
        ByteArrayInputStream pokeDataStream = new ByteArrayInputStream(pokeData);
        for (int i = 0; i < gameCount; i++) {
            LOGGER.debug("Reading poke data for game " + i);
            GameMapperV9 mapper = gameMappers.get(i);
            mapper.setTrainerCount(pokeDataStream.read());
        }
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES - gameCount);
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES * 2);

        for (int i = 0; i < gameCount; i++) {
            GameMapperV9 mapper = gameMappers.get(i);
            int trainerCount = mapper.getTrainerCount();
            if (trainerCount > 0) {
                LOGGER.debug("Importing " + trainerCount + " trainers");
                for (int j = 0; j < trainerCount; j++) {
                    int pokeCount = pokeDataStream.read();
                    String trainerName = Util.getNullTerminatedString(pokeDataStream, 3, 24);
                    Optional<Trainer> trainer = mapper.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent() && pokeCount > 0) {
                        LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
                        for (int k = 0; k < pokeCount; k++) {
                            int address = Util.readAsLittleEndian(pokeDataStream);
                            int value = pokeDataStream.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }

        for (int i = 0; i < gameCount; i++) {
            GameMapperV9 mapper = gameMappers.get(i);
            GameChunk gameChunk = mapper.getGameChunk();
            if (gameChunk.getLength() == DandanatorMiniConstants.GAME_CHUNK_SIZE) {
                gameChunk.setData(copy(zis, gameChunk.getAddress(), gameChunk.getLength()));
            } else if (gameChunk.getLength() > 0) {
                gameChunk.setData(uncompress(zis, gameChunk.getAddress(), gameChunk.getLength()));
            }
        }

        zis.safeSkip(V9Constants.BORDER_EFFECT_OFFSET - zis.position());
        LOGGER.debug("Before version. Position " + zis.position());
        disableBorderEffect = zis.read() == 1;
        autoboot = zis.read() == 1;
        zis.safeSkip(Constants.SLOT_SIZE - zis.position());
        LOGGER.debug("After version. Position " + zis.position());
    }

    @Override
    public byte[] getCharSet() {
        return charSet;
    }

    @Override
    public byte[] getScreen() {
        return screen;
    }

    @Override
    public byte[] getScreenAttributes() {
        return screenAttributes;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        //Order gameBlocks to read them in order from the stream
        gameBlocks.sort(Comparator.comparingInt(GameBlock::getInitSlot)
                .thenComparingInt(GameBlock::getStart));
        for (GameBlock block : gameBlocks) {
            if (block.getInitSlot() < 0xff) {
                LOGGER.debug("Populating game block " + block);
                if (block.getInitSlot() > 0) {
                    if (block.compressed) {
                        int offset = (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart();
                        is.safeSkip(offset - is.position());
                        block.rawdata = Util.fromInputStream(is, block.size);
                        block.data = uncompressByteArray(block.rawdata);
                    } else {
                        block.data = copy(is,
                                (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
                        block.rawdata = block.gameCompressed ? block.data : null;
                    }
                } else {
                    block.data = Constants.ZEROED_SLOT;
                    block.rawdata = null;
                }
            }
        }
    }

    @Override
    public List<? extends GameMapper> getGameMappers() {
        return gameMappers;
    }

    @Override
    public DandanatorMiniImporter getImporter() {
        return new DandanatorMiniV6Importer();
    }

    @Override
    public String getExtraRomMessage() {
        return extraRomMessage;
    }

    @Override
    public String getTogglePokesMessage() {
        return togglePokesMessage;
    }

    @Override
    public String getLaunchGameMessage() {
        return launchGameMessage;
    }

    @Override
    public String getSelectPokesMessage() {
        return selectPokesMessage;
    }

    @Override
    public boolean getDisableBorderEffect() {
        return disableBorderEffect;
    }

    @Override
    public boolean getAutoboot() {
        return autoboot;
    }
}
