package com.grelobites.romgenerator.handlers.dandanatormini.v4;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.TrackeableInputStream;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

public class DandanatorMiniV4Importer implements DandanatorMiniImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV4Importer.class);

    protected static final int SCREEN_THIRD_PIXEL_SIZE = 2048;
    protected static final int SCREEN_THIRD_ATTRINFO_SIZE = 256;

    @Override
    public void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext) throws IOException {
        try {
            TrackeableInputStream slotZeroIs = new TrackeableInputStream(slotZero.data());

            byte[] dandanatorPicFirmware = new byte[DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE];
            byte[] baseRom = slotZeroIs.getAsByteArray(DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("After reading the base rom. Offset " + slotZeroIs.position());
            slotZeroIs.skip(DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.length());
            slotZeroIs.read(dandanatorPicFirmware, 0, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0);
            LOGGER.debug("After reading 1st section of PIC firmware. Offset " + slotZeroIs.position());

            byte[] charSet = slotZeroIs.getAsByteArray(Constants.CHARSET_SIZE);
            LOGGER.debug("After reading the charset. Offset " + slotZeroIs.position());
            byte[] screen = slotZeroIs.getAsByteArray(SCREEN_THIRD_PIXEL_SIZE);
            byte[] attributes = slotZeroIs.getAsByteArray(SCREEN_THIRD_ATTRINFO_SIZE);
            LOGGER.debug("After reading the screen. Offset " + slotZeroIs.position());

            String extraRomMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String togglePokesMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String launchGameMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE)
                    .substring(3);
            String selectPokesMessage = slotZeroIs.getNullTerminatedString(DandanatorMiniConstants.GAMENAME_SIZE);

            slotZeroIs.skip(1);     //Game count
            LOGGER.debug("Skipped head. Position is " + slotZeroIs.position());

            ArrayList<GameDataHolder> recoveredGames = new ArrayList<>();
            //SNA Headers and flags
            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading data from game " + i + " from position " + slotZeroIs.position());
                GameDataHolder gameData = new GameDataHolder();
                gameData.readName(slotZeroIs);
                gameData.readHeader(slotZeroIs);
                gameData.setHoldScreen(slotZeroIs.read());
                gameData.setActiveRom(slotZeroIs.read());
                recoveredGames.add(gameData);
                slotZeroIs.skip(6 + 2 + 64 + 256); //Skip launchCode + RAMAddr + Reserved + Saved Chunk
            }
            //Poke area
            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading poke data for game " + i + " from position " + slotZeroIs.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.setTrainerCount(slotZeroIs.read());
            }
            long beforePokesPosition = slotZeroIs.position();
            slotZeroIs.skip(20); //Skip poke start addresses

            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                GameDataHolder holder = recoveredGames.get(i);
                int trainerCount = holder.getTrainerCount();
                if (trainerCount > 0) {
                    LOGGER.debug("Importing " + trainerCount + " trainers");
                    for (int j = 0; j < trainerCount; j++) {
                        int pokeCount = slotZeroIs.read();
                        String trainerName = Util.getNullTerminatedString(slotZeroIs, 3, 24);
                        Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                        if (trainer.isPresent() && pokeCount > 0) {
                            LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
                            for (int k = 0; k < pokeCount; k++) {
                                int address = Util.asLittleEndian(slotZeroIs);
                                int value = slotZeroIs.read();
                                trainer.map(t -> {
                                    t.addPoke(address, value);
                                    return true;
                                });
                            }
                        }
                    }
                }
            }
            slotZeroIs.skip(DandanatorMiniConstants.POKE_ZONE_SIZE - (slotZeroIs.position() - beforePokesPosition));
            slotZeroIs.read(dandanatorPicFirmware, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0, DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_1);

            LOGGER.debug("After pic firmware. Position " + slotZeroIs.position());

            slotZeroIs.skip(Constants.SLOT_SIZE - slotZeroIs.position());
            LOGGER.debug("After version. Position " + slotZeroIs.position());

            TrackeableInputStream is = new TrackeableInputStream(payload);

            for (int i = 0; i < DandanatorMiniConstants.SLOT_COUNT; i++) {
                LOGGER.debug("Reading game " + i + " data from " + is.position());
                GameDataHolder holder = recoveredGames.get(i);
                holder.readGameSlots(is);
            }
            //If we reached this far, we have all the data and it's safe to replace the game list
            LOGGER.debug("Clearing game list with recovered games count " + recoveredGames.size());
            Collection<Game> games = applicationContext.getGameList();
            games.clear();

            applicationContext.addBackgroundTask(() -> {
                recoveredGames.forEach(holder -> {
                    Future<OperationResult> result = applicationContext.getRomSetHandler()
                            .addGame(holder.createGame());
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
            dandanatorMiniConfiguration.setDandanatorRom(baseRom);
            dandanatorMiniConfiguration.setDandanatorRomPath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setCharSet(charSet);
            globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setDandanatorPicFirmware(dandanatorPicFirmware);
            dandanatorMiniConfiguration.setDandanatorPicFirmwarePath(Constants.ROMSET_PROVIDED);

            globalConfiguration.setBackgroundImage(ImageUtil.fillZxImage(screen, attributes));
            globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRom(extraRom);
            dandanatorMiniConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);

            dandanatorMiniConfiguration.setExtraRomMessage(extraRomMessage);
            dandanatorMiniConfiguration.setTogglePokesMessage(togglePokesMessage);
            dandanatorMiniConfiguration.setLaunchGameMessage(launchGameMessage);
            dandanatorMiniConfiguration.setSelectPokesMessage(selectPokesMessage);
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    private static class GameDataHolder {
        private boolean holdScreen;
        private boolean activeRom;
        private String name;
        private byte[] snaHeader;
        private List<byte[]> gameSlots;
        private TrainerList trainerList = new TrainerList(null);
        private int trainerCount = 0;

        void setHoldScreen(int holdScreenByte) {
            holdScreen = holdScreenByte != 0;
        }

        void setActiveRom(int activeRomByte) {
            activeRom = activeRomByte != 0;
        }

        void readHeader(InputStream is) throws IOException {
            this.snaHeader = new byte[Constants.SNA_HEADER_SIZE];
            is.read(snaHeader);
        }

        void readGameSlots(InputStream is) throws IOException {
            gameSlots = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                gameSlots.add(Util.fromInputStream(is, Constants.SLOT_SIZE));
            }
        }

        void readName(InputStream is) throws IOException {
            name = Util.getNullTerminatedString(is, 3, 33);
            LOGGER.debug("Name read as " + name);
        }

        TrainerList getTrainerList() {
            return trainerList;
        }

        public int getTrainerCount() {
            return trainerCount;
        }

        public void setTrainerCount(int trainerCount) {
            this.trainerCount = trainerCount;
        }

        public List<byte[]> getGameSlots() {
            if (gameSlots != null) {
                return gameSlots;
            } else {
                throw new IllegalStateException("Game slots not set");
            }
        }

        public byte[] getSnaHeader() {
            if (snaHeader != null) {
                return snaHeader;
            } else {
                throw new IllegalStateException("SNA Header not set");
            }
        }

        public void exportTrainers(RamGame game) {
            trainerList.setOwner(game);
            game.setTrainerList(trainerList);
        }

        public Game createGame() {
            final RamGame game = new RamGame(GameType.RAM48, getGameSlots());
            game.setName(name);
            game.setHoldScreen(holdScreen);
            game.setRom(activeRom);
            game.setSnaHeader(SNAHeader.from48kSNAGameByteArray(getSnaHeader()));
            exportTrainers(game);
            return game;
        }
    }
}
