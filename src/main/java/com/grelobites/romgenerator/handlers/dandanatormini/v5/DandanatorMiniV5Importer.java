package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import com.grelobites.romgenerator.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.Future;

public class DandanatorMiniV5Importer implements DandanatorMiniImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV5Importer.class);

    @Override
    public void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
            throws IOException {

        slotZero.parse();
        PositionAwareInputStream is = new PositionAwareInputStream(payload);
        slotZero.populateGameSlots(is);

        //If we reached this far, we have all the data and it's safe to replace the game list
        LOGGER.debug("Clearing game list with recovered games count " + slotZero.getGameMappers().size());
        Collection<Game> games = applicationContext.getGameList();
        games.clear();
        applicationContext.addBackgroundTask(() -> {
            slotZero.getGameMappers().forEach(holder -> {
                final Game game = holder.createGame();
                Future<OperationResult> result = applicationContext.getRomSetHandler().addGame(game);
                try {
                    result.get();
                } catch (Exception e) {
                    LOGGER.warn("While waiting for background operation result", e);
                }
            });
            return OperationResult.successResult();
        });

        //ExtraRom is located in the last slot
        is.safeSkip(DandanatorMiniConstants.GAME_SLOTS * Constants.SLOT_SIZE - is.position());
        byte[] extraRom = is.getAsByteArray(Constants.SLOT_SIZE);

        //Update preferences only if everything was OK
        Configuration globalConfiguration = Configuration.getInstance();
        DandanatorMiniConfiguration dandanatorMiniConfiguration = DandanatorMiniConfiguration.getInstance();

        //Keep this order, first the image and then the path, to avoid listeners to
        //enter before the image is set
        globalConfiguration.setCharSet(slotZero.getCharSet());
        globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

        globalConfiguration.setBackgroundImage(slotZero.getScreen());
        globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

        dandanatorMiniConfiguration.setExtraRom(extraRom);
        dandanatorMiniConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);

        dandanatorMiniConfiguration.setExtraRomMessage(slotZero.getExtraRomMessage());
        dandanatorMiniConfiguration.setTogglePokesMessage(slotZero.getTogglePokesMessage());
        dandanatorMiniConfiguration.setLaunchGameMessage(slotZero.getLaunchGameMessage());
        dandanatorMiniConfiguration.setSelectPokesMessage(slotZero.getSelectPokesMessage());

    }

    @Override
    public void mergeRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        slotZero.parse();
        PositionAwareInputStream is = new PositionAwareInputStream(payload);
        slotZero.populateGameSlots(is);

        applicationContext.addBackgroundTask(() -> {
            slotZero.getGameMappers().forEach(holder -> {
                final Game game = holder.createGame();
                Future<OperationResult> futureResult = applicationContext.getRomSetHandler().addGame(game);
                try {
                    futureResult.get();
                } catch (Exception e) {
                    LOGGER.warn("While waiting for background operation result", e);
                }
            });
            return OperationResult.successResult();
        });
    }

}
