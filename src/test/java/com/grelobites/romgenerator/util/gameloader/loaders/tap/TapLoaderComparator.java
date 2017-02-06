package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.loaders.Z80GameImageLoader;
import com.grelobites.romgenerator.util.gameloader.loaders.tap.tapeloader.CompositePlus2ATapeLoader;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class TapLoaderComparator {


    public static void main(String[] args) throws Exception {
        FileInputStream tapStream = new FileInputStream("/Users/mteira/Desktop/Dandanator/tap/128/dizzy4.tap");
        new CompositePlus2ATapeLoader().loadTape(tapStream);

    }
}
