package com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.RamGame;
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
            "/loader/plus23-40-0.rom",
            "/loader/plus23-40-1.rom",
            "/loader/plus23-40-2.rom",
            "/loader/plus23-40-3.rom"};

    private static final String[] ROM41_ROMS = new String[]{
            "/loader/plus23-41-0.rom",
            "/loader/plus23-41-1.rom",
            "/loader/plus23-41-2.rom",
            "/loader/plus23-41-3.rom"};

    private static final String ROM40_LOADER = "/loader/loader.+2a-40.z80";
    private static final String ROM41_LOADER = "/loader/loader.+2a-41.z80";

    private static int compareSlot(byte[] slot0, byte[] slot1) {
        int differences = 0;
        for (int i = 0; i < slot0.length; i++) {
            if (slot0[i] != slot1[i]) {
                differences++;
            }
        }
        LOGGER.debug("Slots have " + differences + " differences");
        return differences;
    }

    private static void compareGames(Game game0, Game game1) {
        int differences = 0;
        for (int i = 0; i < game0.getSlotCount(); i++) {
            differences += compareSlot(game0.getSlot(i), game1.getSlot(i));
        }
        LOGGER.debug("Games have " + differences + " differences");
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
        GameUtil.popPC((RamGame) game);
        try (FileOutputStream fout = new FileOutputStream(name)) {
            new Z80GameImageLoader().save(
                    game, fout);
        } catch (Exception e) {
            LOGGER.error("Saving game as Z80", e);
        } finally {
            GameUtil.pushPC((RamGame) game);
        }

    }
    @Override
    public Game loadTape(InputStream tapeFile) {
        try {
            TapeLoaderPlus2A tapeLoader = new TapeLoaderPlus2A();


            byte[] tapeByteArray = Util.fromInputStream(tapeFile);

            tapeLoader.setRomResources(ROM40_ROMS);
            tapeLoader.setTapeLoaderResource(ROM40_LOADER);
            Game game40 = tapeLoader.loadTape(new ByteArrayInputStream(tapeByteArray));

            tapeLoader.setRomResources(ROM41_ROMS);
            tapeLoader.setTapeLoaderResource(ROM41_LOADER);
            Game game41 = tapeLoader.loadTape(new ByteArrayInputStream(tapeByteArray));

            compareGames(game40, game41);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }



        return null;
    }
}
