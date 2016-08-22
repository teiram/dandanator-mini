package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.GameMapper;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZeroBase;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.Compressor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SlotZeroV5 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV5.class);
    private static final int GAME_STRUCT_SIZE = 136;

    private byte[] charSet;
    private byte[] screen;
    private byte[] screenAttributes;
    private List<GameMapperV5> gameMappers;
    private String extraRomMessage;
    private String togglePokesMessage;
    private String launchGameMessage;
    private String selectPokesMessage;
    List<GameBlock> gameBlocks;

    public SlotZeroV5(byte[] data) {
        super(data);
    }

    private static Compressor getCompressor() {
        return DandanatorMiniConfiguration.getInstance()
                .getCompressor();
    }

    private static byte[] uncompress(PositionAwareInputStream is, int offset, int size) throws IOException {
        LOGGER.debug("Uncompress with offset " + offset + " and size " + size);
        LOGGER.debug("Skipping " + (offset - is.position()) + " to start of compressed data");
        is.skip(offset - is.position());
        byte[] compressedData = Util.fromInputStream(is, size);
        InputStream uncompressedStream = getCompressor().getUncompressingInputStream(
                new ByteArrayInputStream(compressedData));
        return Util.fromInputStream(uncompressedStream);
    }

    private static byte[] copy(PositionAwareInputStream is, int offset, int size) throws IOException {
        is.skip(offset - is.position());
        return Util.fromInputStream(is, size);
    }

    @Override
    public boolean validate() {
        try {
            return getMajorVersion() == 5;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    @Override
    public void parse() throws IOException {
        PositionAwareInputStream zis = new PositionAwareInputStream(data());
        zis.skip(DandanatorMiniConstants.BASEROM_SIZE);
        int gameCount = zis.read();
        LOGGER.debug("Read number of games: " + gameCount);
        gameMappers = new ArrayList<>();
        gameBlocks = new ArrayList<>();
        for (int i = 0; i < gameCount; i++) {
            GameMapperV5 mapper = GameMapperV5.fromRomSet(zis);
            gameBlocks.addAll(mapper.getBlocks());
            gameMappers.add(mapper);
        }

        zis.skip(GAME_STRUCT_SIZE * (DandanatorMiniConstants.MAX_GAMES - gameCount));

        int compressedScreenOffset = zis.getAsLittleEndian();
        int compressedScreenBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed screen located at " + compressedScreenOffset + ", blocks "
                + compressedScreenBlocks);
        int compressedTextDataOffset = zis.getAsLittleEndian();
        int compressedTextDataBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed text data located at " + compressedTextDataOffset + ", blocks "
                + compressedTextDataBlocks);
        int compressedPokeStructOffset = zis.getAsLittleEndian();
        int compressedPokeStructBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed poke data located at " + compressedPokeStructOffset + ", blocks "
                + compressedPokeStructBlocks);
        int compressedPicFwAndCharsetOffset = zis.getAsLittleEndian();
        int compressedPicFwAndCharsetBlocks = zis.getAsLittleEndian();
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
            GameMapperV5 mapper = gameMappers.get(i);
            mapper.setTrainerCount(pokeDataStream.read());
        }
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES - gameCount);
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES * 2);

        for (int i = 0; i < gameCount; i++) {
            GameMapperV5 mapper = gameMappers.get(i);
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
                            int address = Util.asLittleEndian(pokeDataStream);
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
            GameMapperV5 mapper = gameMappers.get(i);
            GameChunk gameChunk = mapper.getGameChunk();
            gameChunk.setData(uncompress(zis, gameChunk.getAddress(), gameChunk.getLength()));
        }

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
        //Order gameBlocks from minor to major
        gameBlocks.sort(Comparator.comparingInt(GameBlock::getInitSlot)
                .thenComparingInt(GameBlock::getStart));
        for (GameBlock block : gameBlocks) {
            if (block.getInitSlot() < 0xff) {
                if (block.compressed) {
                    LOGGER.debug("Uncompressing GameCBlock with initSlot "
                            + block.getInitSlot() + ", start " + block.getStart()
                            + ", size " + block.size);
                    block.data = uncompress(is, (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
                } else {
                    block.data = copy(is, (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
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
        return new DandanatorMiniV5Importer();
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

}
