package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.daad.DAADData;
import com.grelobites.romgenerator.util.daad.DAADGenerator;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class DAADGameImageLoader extends MLDGameImageLoader implements GameImageLoader {

    @Override
    public Game load(InputStream is) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(is)) {
            DAADGenerator generator = new DAADGenerator(DAADData.fromZipStream(zip));
            return super.load(generator.generate());
        }
    }

}
