package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.*;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.TapeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CompositePlus2ATapeLoader implements TapeLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositePlus2ATapeLoader.class);
    private static final String[] ROM40_ROMS = new String[]{
            "/loader/plus23-40-en-0.rom",
            "/loader/plus23-40-en-1.rom",
            "/loader/plus23-40-en-2.rom",
            "/loader/plus23-40-en-3.rom"};

    private static final String[] ROM41_ROMS = new String[]{
            "/loader/plus23-41-en-0.rom",
            "/loader/plus23-41-en-1.rom",
            "/loader/plus23-41-en-2.rom",
            "/loader/plus23-41-en-3.rom"};

    private static final String ROM40_LOADER = "/loader/loader.+2a-40.z80";
    private static final String ROM41_LOADER = "/loader/loader.+2a-41.z80";

    private static int compareSlot(ChangeData changeData, byte[] bank0, byte[] bank1, int bank) {
        int differences = 0;
        for (int i = 0; i < bank0.length; i++) {
            if (bank0[i] != bank1[i]) {
                differences++;
                changeData.addChangeValue(bank, new ChangeValue(i, bank1[i]));
            }
        }
        LOGGER.debug("Slots have " + differences + " differences");
        return differences;
    }

    private static ChangeData getGameRomDifferences(Game game40, Game game41) {
        int differences = 0;
        ChangeData changeData = new ChangeData(RomId.ROM_PLUS2A_41);
        for (int i = 0; i < game40.getSlotCount(); i++) {
            differences += compareSlot(changeData, game40.getSlot(i), game41.getSlot(i), i);
        }
        LOGGER.debug("Games have " + differences + " differences");
        return changeData;
    }

    private static void saveGameSlots(Game game, String baseName) {
        for (int i = 0; i < game.getSlotCount(); i++) {
            try (FileOutputStream fos = new FileOutputStream(baseName + i + ".bin")) {
                fos.write(game.getSlot(i));
            } catch (Exception e) {
                LOGGER.debug("Writing game slot", e);
            }
        }
    }

    private static void saveGameAsZ80(Game game, String name) {
        GameUtil.popPC((SnapshotGame) game);
        try (FileOutputStream fout = new FileOutputStream(name)) {
            new Z80GameImageLoader().save(
                    game, fout);
        } catch (Exception e) {
            LOGGER.error("Saving game as Z80", e);
        } finally {
            GameUtil.pushPC((SnapshotGame) game);
        }

    }
    @Override
    public Game loadTape(InputStream tapeFile) {
        try {
            TapeLoaderPlus2A tapeLoader = new TapeLoaderPlus2A();


            byte[] tapeByteArray = Util.fromInputStream(tapeFile);

            tapeLoader.setRomResources(ROM40_ROMS);
            VersionedSnapshotGame game40 = (VersionedSnapshotGame) tapeLoader
                    .loadTape(new ByteArrayInputStream(tapeByteArray));
            game40.setVersion(RomId.ROM_PLUS2A_40);

            tapeLoader.setRomResources(ROM41_ROMS);
            Game game41 = tapeLoader.loadTape(new ByteArrayInputStream(tapeByteArray));

            ChangeData version41Data = getGameRomDifferences(game40, game41);
            game40.getChangeDataMap().put(version41Data.getVersion(), version41Data);

            return game40;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
