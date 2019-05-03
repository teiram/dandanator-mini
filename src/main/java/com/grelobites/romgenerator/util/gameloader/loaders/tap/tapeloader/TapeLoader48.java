package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

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

public class TapeLoader48 extends NonBankedMemoryTapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapeLoader48.class);

    public TapeLoader48() {
        super(0x10000);
    }

    @Override
    protected List<byte[]> getRamBanks() {
        List<byte[]> banks = new ArrayList<>();
        for (int i = 1; i < 4; i++) {
            byte[] bank = Arrays.copyOfRange(z80Ram.asByteArray(), i * BANK_SIZE, (i + 1) * BANK_SIZE);
            banks.add(bank);
        }
        return banks;
    }

    @Override
    void prepareForLoading() {
        try (InputStream loaderStream = NonBankedMemoryTapeLoader.class
                .getResourceAsStream("/loader/loader.48.z80")) {
            SnapshotGame game = (SnapshotGame) new Z80GameImageLoader().load(loaderStream);
            GameUtil.popPC(game); //Since games are loaded with the PC pushed

            Z80State z80state = getStateFromHeader(game.getGameHeader());

            int slot = 0;
            for (int i : new int[] {0x4000, 0x8000, 0xc000}) {
                z80Ram.load(game.getSlot(slot++), 0, i, BANK_SIZE);
            }
            LOGGER.debug("Calculated Z80State as " + z80state);
            z80.setZ80State(z80state);
        } catch (IOException ioe) {
            LOGGER.debug("Loading Tape Loader", ioe);
        }
    }

    @Override
    protected SnapshotGame contextAsGame() {
        SnapshotGame game =  new SnapshotGame(GameType.RAM48, getRamBanks());
        prepareSnapshot(game);
        game.setHardwareMode(HardwareMode.HW_48K);
        return game;
    }

}
