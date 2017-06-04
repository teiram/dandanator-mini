package com.grelobites.romgenerator.util.compress.zx7;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;

import java.io.IOException;

public class AlterEgoCompressTest {

    public void loadAndCompareAlterEgoSlots() throws IOException {

        Game game = new Z80GameImageLoader().load(
            AlterEgoCompressTest.class.getResourceAsStream("/alter-ego.z80"));


    }

}
