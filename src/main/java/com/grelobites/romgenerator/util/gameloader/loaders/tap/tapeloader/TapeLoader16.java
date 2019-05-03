package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.HardwareMode;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.Z80State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TapeLoader16 extends NonBankedMemoryTapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader16.class);

    public TapeLoader16() {
        super(0x8000);
    }

    @Override
    protected List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        banks.add(Arrays.copyOfRange(z80Ram.asByteArray(), 0x4000, 0x4000 + BANK_SIZE));
        return banks;
    }

    @Override
    void prepareForLoading() {
        try (InputStream loaderStream = NonBankedMemoryTapeLoader.class
                .getResourceAsStream("/loader/loader.16.z80")) {
            SnapshotGame game = (SnapshotGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game); //Since games are loaded with the PC pushed

            Z80State z80state = getStateFromHeader(game.getGameHeader());

            z80Ram.load(game.getSlot(0), 0, 0x4000, BANK_SIZE);

            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Tape Loader", ioe);
        }
    }

    @Override
    protected SnapshotGame contextAsGame() {
        SnapshotGame game =  new SnapshotGame(GameType.RAM16, getRamBanks());
        prepareSnapshot(game);
        game.setHardwareMode(HardwareMode.HW_16K);
        return game;
    }

    @Override
    public void poke8(int address, int value) {
        clock.addTstates(3);
        z80Ram.poke8(address, value);
    }

}
