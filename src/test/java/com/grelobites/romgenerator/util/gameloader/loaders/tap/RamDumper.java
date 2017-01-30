package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class RamDumper {


    public static void main(String[] args) throws Exception {
        Game game = new Z80GameImageLoader().load(
                new FileInputStream("/Users/mteira/Desktop/Dandanator/tap/128/addams-rom41.z80"));
        FileOutputStream dump = new FileOutputStream("/Users/mteira/Desktop/Dandanator/tap/128/dump41.bin");
        for (int i = 0; i < 8; i++) {
            dump.write(game.getSlot(i));
        }
        dump.flush();
        dump.close();

    }
}
