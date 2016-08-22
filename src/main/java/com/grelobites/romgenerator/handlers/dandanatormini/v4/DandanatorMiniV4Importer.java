package com.grelobites.romgenerator.handlers.dandanatormini.v4;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.PositionAwareInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.Future;

public class DandanatorMiniV4Importer implements DandanatorMiniImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV4Importer.class);

    @Override
    public void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        try {
            slotZero.parse();

            PositionAwareInputStream is = new PositionAwareInputStream(payload);

            slotZero.populateGameSlots(is);

            //If we reached this far, we have all the data and it's safe to replace the game list
            LOGGER.debug("Clearing game list with recovered games count " + slotZero.getGameMappers().size());
            Collection<Game> games = applicationContext.getGameList();
            games.clear();

            applicationContext.addBackgroundTask(() -> {
                slotZero.getGameMappers().forEach(gameMapper -> {
                    Future<OperationResult> result = applicationContext.getRomSetHandler()
                            .addGame(gameMapper.createGame());
                    try {
                        result.get();
                    } catch (Exception e) {
                        LOGGER.warn("While waiting for background operation result", e);
                    }
                });
                return OperationResult.successResult();
            });

            byte[] extraRom = is.getAsByteArray(Constants.SLOT_SIZE);

            //Update preferences only if everything was OK
            Configuration globalConfiguration = Configuration.getInstance();
            DandanatorMiniConfiguration dandanatorMiniConfiguration = DandanatorMiniConfiguration.getInstance();

            //Keep this order, first the image and then the path, to avoid listeners to
            //enter before the image is set
            //Initially we don't want to use the RomSet provided base ROM
            //dandanatorMiniConfiguration.setDandanatorRom(baseRom);
            //dandanatorMiniConfiguration.setDandanatorRomPath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setCharSet(slotZero.getCharSet());
            globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

            //The PIC firmware is discarded so far
            //dandanatorMiniConfiguration.setDandanatorPicFirmware(dandanatorPicFirmware);
            //dandanatorMiniConfiguration.setDandanatorPicFirmwarePath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setBackgroundImage(ImageUtil.fillZxImage(
                    slotZero.getScreen(),
                    slotZero.getScreenAttributes()));
            globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRom(extraRom);
            dandanatorMiniConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRomMessage(slotZero.getExtraRomMessage());
            dandanatorMiniConfiguration.setTogglePokesMessage(slotZero.getTogglePokesMessage());
            dandanatorMiniConfiguration.setLaunchGameMessage(slotZero.getLaunchGameMessage());
            dandanatorMiniConfiguration.setSelectPokesMessage(slotZero.getSelectPokesMessage());
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    @Override
    public void mergeRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        try {
            slotZero.parse();
            slotZero.populateGameSlots(new PositionAwareInputStream(payload));

            applicationContext.addBackgroundTask(() -> {
                slotZero.getGameMappers().forEach(gameMapper -> {
                    Future<OperationResult> result = applicationContext.getRomSetHandler()
                            .addGame(gameMapper.createGame());
                    try {
                        result.get();
                    } catch (Exception e) {
                        LOGGER.warn("While waiting for background operation result", e);
                    }
                });
                return OperationResult.successResult();
            });

         } catch (Exception e) {
            LOGGER.error("Merging RomSet", e);
        }
    }
}
