package com.grelobites.romgenerator.model;

import java.util.Collections;

public class RomGame extends BaseGame implements Game {

    public RomGame(byte[] data) {
        super(GameType.ROM, Collections.singletonList(data));
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

}
