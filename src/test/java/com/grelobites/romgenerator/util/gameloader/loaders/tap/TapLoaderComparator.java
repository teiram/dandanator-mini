package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.TapeLoaderPlus2A;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class TapLoaderComparator {

    private static void saveGameAsZ80(Game game, String name) {
        GameUtil.popPC((SnapshotGame) game);
        try (FileOutputStream fout = new FileOutputStream(name)) {
            new Z80GameImageLoader().save(
                    game, fout);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            GameUtil.pushPC((SnapshotGame) game);
        }

    }
    private static int compareSlots(byte[] slot0, byte[] slot1) {
        assertEquals(slot0.length, slot1.length);
        int differenceCount = 0;
        for (int i = 0; i < slot0.length; i++) {
            if (slot0[i] != slot1[i]) {
                differenceCount++;
            }
        }
        return differenceCount;
    }

    private static int compareGames(Game game0, Game game1) {
        assertEquals(game0.getSlotCount(), game1.getSlotCount());
        int differences = 0;
        for (int i = 0; i < game0.getSlotCount(); i++) {
            int slotDifferences = compareSlots(game0.getSlot(i), game1.getSlot(i));
            System.out.println(slotDifferences + " in slot " + i);
            differences += slotDifferences;
        }
        System.out.println("Total differences " + differences);
        return differences;
    }

    @Test
    public void testDifferences() throws Exception {
        InputStream tapStream = TapLoaderComparator.class.getResourceAsStream("/loader/addams.tap");

        byte [] tapByteArray = Util.fromInputStream(tapStream);

        TapeLoaderPlus2A tapeLoader = new TapeLoaderPlus2A();
        tapeLoader.setRomResources(new String[]{
                "/loader/plus23-40-en-0.rom",
                "/loader/plus23-40-en-1.rom",
                "/loader/plus23-40-en-2.rom",
                "/loader/plus23-40-en-3.rom"});

        Game game0 = tapeLoader
                .loadTape(new ByteArrayInputStream(tapByteArray));

        saveGameAsZ80(game0, "/var/tmp/addams-40-en.z80");

        tapeLoader.setRomResources(new String[]{
                "/loader/plus23-41-en-0.rom",
                "/loader/plus23-41-en-1.rom",
                "/loader/plus23-41-en-2.rom",
                "/loader/plus23-41-en-3.rom"});
        Game game1 = tapeLoader.loadTape(new ByteArrayInputStream(tapByteArray));

        saveGameAsZ80(game1, "/var/tmp/addams-41-en.z80");

        tapeLoader.setRomResources(new String[]{
                "/loader/plus23-41-0-es.rom",
                "/loader/plus23-41-1-es.rom",
                "/loader/plus23-41-2-es.rom",
                "/loader/plus23-41-3-es.rom"});
        Game game2 = tapeLoader.loadTape(new ByteArrayInputStream(tapByteArray));
        saveGameAsZ80(game2, "/var/tmp/addams-41-es.z80");


        int differences01 = compareGames(game0, game1);
        int differences02 = compareGames(game0, game2);
        int differences03 = compareGames(game1, game2);
        assertEquals(0, differences01 + differences02 + differences03);

    }
}
