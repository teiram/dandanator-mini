package com.grelobites.romgenerator.handlers.dandanatormini.v4;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZeroBase;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SlotZeroV4 extends SlotZeroBase implements SlotZero {
    private static final Logger LOGGER = LoggerFactory.getLogger(SlotZeroV4.class);

    protected static final int SCREEN_THIRD_PIXEL_SIZE = 2048;
    protected static final int SCREEN_THIRD_ATTRINFO_SIZE = 256;

    private byte[] screen;
    private byte[] screenAttributes;
    private byte[] charSet;
    private List<GameMapperV4> gameMappers;
    private String extraRomMessage;
    private String togglePokesMessage;
    private String launchGameMessage;
    private String selectPokesMessage;

    public SlotZeroV4(byte[] data) {
        super(data);
    }

    private static List<GameMapperV4> getGameData(PositionAwareInputStream stream) throws IOException {

        ArrayList<GameMapperV4> gameMappers = new ArrayList<>();
        //SNA Headers and flags
        for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
            LOGGER.debug("Reading data from game " + i + " from position " + stream.position());
            GameMapperV4 gameData = new GameMapperV4();
            gameData.readName(stream);
            gameData.readHeader(stream);
            gameData.setHoldScreen(stream.read());
            gameData.setActiveRom(stream.read());
            gameMappers.add(gameData);
            stream.safeSkip(6 + 2 + 64 + 256); //Skip launchCode + RAMAddr + Reserved + Saved Chunk
        }
        //Poke area
        for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
            LOGGER.debug("Reading poke data for game " + i + " from position " + stream.position());
            GameMapperV4 holder = gameMappers.get(i);
            holder.setTrainerCount(stream.read());
        }
        long beforePokesPosition = stream.position();
        stream.safeSkip(20); //Skip poke start addresses

        for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
            GameMapperV4 holder = gameMappers.get(i);
            int trainerCount = holder.getTrainerCount();
            if (trainerCount > 0) {
                LOGGER.debug("Importing " + trainerCount + " trainers");
                for (int j = 0; j < trainerCount; j++) {
                    int pokeCount = stream.read();
                    String trainerName = Util.getNullTerminatedString(stream, 3, 24);
                    Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent() && pokeCount > 0) {
                        LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
                        for (int k = 0; k < pokeCount; k++) {
                            int address = Util.asLittleEndian(stream);
                            int value = stream.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }
        stream.safeSkip(DandanatorMiniConstants.POKE_ZONE_SIZE - (stream.position() - beforePokesPosition));
        return gameMappers;
    }

    @Override
    public void parse() throws IOException {
        PositionAwareInputStream slotZeroIs = new PositionAwareInputStream(data());

        slotZeroIs.safeSkip(DandanatorMiniConstants.BASEROM_SIZE);
        slotZeroIs.safeSkip(DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.length());
        slotZeroIs.safeSkip(DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0);
        LOGGER.debug("After passing 1st section of PIC firmware. Offset " + slotZeroIs.position());

        charSet = slotZeroIs.getAsByteArray(Constants.CHARSET_SIZE);
        LOGGER.debug("After reading the charset. Offset " + slotZeroIs.position());
        screen = slotZeroIs.getAsByteArray(SCREEN_THIRD_PIXEL_SIZE);
        screenAttributes = slotZeroIs.getAsByteArray(SCREEN_THIRD_ATTRINFO_SIZE);
        LOGGER.debug("After reading the screen. Offset " + slotZeroIs.position());

        extraRomMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                .substring(3);
        togglePokesMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                .substring(3);
        launchGameMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                .substring(3);
        selectPokesMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE);

        slotZeroIs.safeSkip(1);     //Game count
        LOGGER.debug("Skipped head. Position is " + slotZeroIs.position());

        gameMappers = getGameData(slotZeroIs);

        slotZeroIs.safeSkip(DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_1);

        LOGGER.debug("After pic firmware. Position " + slotZeroIs.position());
    }

    @Override
    public boolean validate() {
        LOGGER.debug("Validating RomSet");
        try {
            return getMajorVersion() == 4;
        } catch (Exception e) {
            LOGGER.debug("Validation failed", e);
            return false;
        }
    }

    public byte[] getScreen() {
        return screen;
    }

    public byte[] getScreenAttributes() {
        return screenAttributes;
    }

    @Override
    public void populateGameSlots(PositionAwareInputStream is) throws IOException {
        for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
            gameMappers.get(i).populateGameSlots(is);
        }
    }

    public List<GameMapperV4> getGameMappers() {
        return gameMappers;
    }

    @Override
    public byte[] getCharSet() {
        return charSet;
    }

    @Override
    public DandanatorMiniImporter getImporter() {
        return new DandanatorMiniV4Importer();
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
