package com.grelobites.romgenerator.util.gameloader.loaders;

import com.grelobites.romgenerator.model.DanTapGame;
import com.grelobites.romgenerator.model.DanTapTable;
import com.grelobites.romgenerator.model.DanTapTableEntry;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.gameloader.GameImageLoader;
import com.grelobites.romgenerator.util.tap.TapInputStream;
import com.grelobites.romgenerator.util.tap.TapOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DanTapGameImageLoader implements GameImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanTapGameImageLoader.class);

    @Override
    public Game load(InputStream is) throws IOException {
        TapInputStream tis = new TapInputStream(is);
        List<byte[]> tapBlocks = new ArrayList<>();
        Optional<byte[]> tapBlockOpt;
        while ((tapBlockOpt = tis.nextRaw()).isPresent()) {
            byte[] block = tapBlockOpt.get();
            tapBlocks.add(block);
        }
        DanTapGame game = new DanTapGame(tapBlocks);
        return game;
    }

    @Override
    public void save(Game game, OutputStream os) throws IOException {
        DanTapGame danTapGame = (DanTapGame) game;
        try (TapOutputStream tos = new TapOutputStream(os)) {
            List<byte[]> tapBlocks = danTapGame.getTapBlocks();
            for (int i = 0; i < tapBlocks.size(); i++) {
                tos.addPreparedBlock(tapBlocks.get(i));
            }
        }

    }

}
