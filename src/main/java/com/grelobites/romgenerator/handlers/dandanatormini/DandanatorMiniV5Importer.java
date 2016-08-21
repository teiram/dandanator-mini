package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.model.RomGame;
import com.grelobites.romgenerator.model.Trainer;
import com.grelobites.romgenerator.model.TrainerList;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.TrackeableInputStream;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.view.ApplicationContext;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DandanatorMiniV5Importer implements DandanatorMiniImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV5Importer.class);
    private static final int GAME_STRUCT_SIZE = 136;

    private static Compressor getCompressor() {
        return DandanatorMiniConfiguration.getInstance()
                .getCompressor();
    }

    private static byte[] uncompress(TrackeableInputStream is, int offset, int size) throws IOException {
        LOGGER.debug("Uncompress with offset " + offset + " and size " + size);
        LOGGER.debug("Skipping " + (offset - is.position()) + " to start of compressed data");
        is.skip(offset - is.position());
        byte[] compressedData = Util.fromInputStream(is, size);
        InputStream uncompressedStream = getCompressor().getUncompressingInputStream(
                new ByteArrayInputStream(compressedData));
        return Util.fromInputStream(uncompressedStream);
    }

    private static byte[] copy(TrackeableInputStream is, int offset, int size) throws IOException {
        is.skip(offset - is.position());
        return Util.fromInputStream(is, size);
    }

    @Override
    public void importRomSet(SlotZero slotZero, InputStream payload, ApplicationContext applicationContext)
    throws IOException {
        TrackeableInputStream zis = new TrackeableInputStream(slotZero.data());
        zis.skip(DandanatorMiniConstants.BASEROM_SIZE);
        int gameCount = zis.read();
        LOGGER.debug("Read number of games: " + gameCount);
        List<GameDataHolder> recoveredGames = new ArrayList<>();
        List<GameCBlock> gameCBlocks = new ArrayList<>();
        for (int i = 0; i < gameCount; i++) {
            GameDataHolder gameDataHolder = GameDataHolder.fromRomSet(zis);
            gameCBlocks.addAll(gameDataHolder.getCBlocks());
            recoveredGames.add(gameDataHolder);
        }

        zis.skip(GAME_STRUCT_SIZE * (DandanatorMiniConstants.MAX_GAMES - gameCount));

        int compressedScreenOffset = zis.getAsLittleEndian();
        int compressedScreenBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed screen located at " + compressedScreenOffset + ", blocks "
                + compressedScreenBlocks);
        int compressedTextDataOffset = zis.getAsLittleEndian();
        int compressedTextDataBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed text data located at " + compressedTextDataOffset + ", blocks "
                + compressedTextDataBlocks);
        int compressedPokeStructOffset = zis.getAsLittleEndian();
        int compressedPokeStructBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed poke data located at " + compressedPokeStructOffset + ", blocks "
                + compressedPokeStructBlocks);
        int compressedPicFwAndCharsetOffset = zis.getAsLittleEndian();
        int compressedPicFwAndCharsetBlocks = zis.getAsLittleEndian();
        LOGGER.debug("Compressed PIC FW and Charset located at " + compressedPicFwAndCharsetOffset
                + ", blocks " + compressedPicFwAndCharsetBlocks);

        byte[] screen = uncompress(zis, compressedScreenOffset, compressedScreenBlocks);
        byte[] textData = uncompress(zis, compressedTextDataOffset, compressedTextDataBlocks);
        byte[] pokeData = uncompress(zis, compressedPokeStructOffset, compressedPokeStructBlocks);
        byte[] picFwAndCharset = uncompress(zis, compressedPicFwAndCharsetOffset, compressedPicFwAndCharsetBlocks);

        ByteArrayInputStream textDataStream = new ByteArrayInputStream(textData);
        String extraRomMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        String togglePokesMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        String launchGameMessage = Util.getNullTerminatedString(textDataStream, 3, DandanatorMiniConstants.GAMENAME_SIZE);
        String selectPokesMessage = Util.getNullTerminatedString(textDataStream, DandanatorMiniConstants.GAMENAME_SIZE);

        byte[] charSet = Arrays.copyOfRange(picFwAndCharset, 0, Constants.CHARSET_SIZE);

        //Poke data
        ByteArrayInputStream pokeDataStream = new ByteArrayInputStream(pokeData);
        for (int i = 0; i < gameCount; i++) {
            LOGGER.debug("Reading poke data for game " + i);
            GameDataHolder holder = recoveredGames.get(i);
            holder.setTrainerCount(pokeDataStream.read());
        }
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES - gameCount);
        pokeDataStream.skip(DandanatorMiniConstants.MAX_GAMES * 2);

        for (int i = 0; i < gameCount; i++) {
            GameDataHolder holder = recoveredGames.get(i);
            int trainerCount = holder.getTrainerCount();
            if (trainerCount > 0) {
                LOGGER.debug("Importing " + trainerCount + " trainers");
                for (int j = 0; j < trainerCount; j++) {
                    int pokeCount = pokeDataStream.read();
                    String trainerName = Util.getNullTerminatedString(pokeDataStream, 3, 24);
                    Optional<Trainer> trainer = holder.getTrainerList().addTrainerNode(trainerName);
                    if (trainer.isPresent() && pokeCount > 0) {
                        LOGGER.debug("Importing " + pokeCount + " pokes on trainer " + trainerName);
                        for (int k = 0; k < pokeCount; k++) {
                            int address = Util.asLittleEndian(pokeDataStream);
                            int value = pokeDataStream.read();
                            trainer.map(t -> {
                                t.addPoke(address, value);
                                return true;
                            });
                        }
                    }
                }
            }
        }

        for (int i = 0; i < gameCount; i++) {
            GameDataHolder holder = recoveredGames.get(i);
            holder.gameChunk.data = uncompress(zis, holder.gameChunk.addr, holder.gameChunk.length);
        }

        zis.skip(Constants.SLOT_SIZE - zis.position());
        LOGGER.debug("After version. Position " + zis.position());

        TrackeableInputStream is = new TrackeableInputStream(payload);
        //Order and uncompress CBlocks
        gameCBlocks.sort(Comparator.comparingInt(GameCBlock::getInitSlot)
                .thenComparingInt(GameCBlock::getStart));
        for (GameCBlock block : gameCBlocks) {
            if (block.getInitSlot() < 0xff) {
                if (block.compressed) {
                    LOGGER.debug("Uncompressing GameCBlock with initSlot "
                            + block.getInitSlot() + ", start " + block.getStart()
                            + ", size " + block.size);
                    block.data = uncompress(is, (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
                } else {
                    block.data = copy(is, (block.getInitSlot() - 1) * Constants.SLOT_SIZE + block.getStart(), block.size);
                }
            }
        }

        //If we reached this far, we have all the data and it's safe to replace the game list
        LOGGER.debug("Clearing game list with recovered games count " + recoveredGames.size());
        Collection<Game> games = applicationContext.getGameList();
        games.clear();
        applicationContext.addBackgroundTask(() -> {
            recoveredGames.forEach(holder -> {
                final Game game = holder.createGame();
                Platform.runLater(() -> applicationContext.getGameList().add(game));
            });
            return OperationResult.successResult();
        });


        LOGGER.debug("Added " + games.size() + " to the list of games");

        byte[] extraRom = is.getAsByteArray(Constants.SLOT_SIZE);

        //Update preferences only if everything was OK
        Configuration globalConfiguration = Configuration.getInstance();
        DandanatorMiniConfiguration dandanatorMiniConfiguration = DandanatorMiniConfiguration.getInstance();

        //Keep this order, first the image and then the path, to avoid listeners to
        //enter before the image is set
        globalConfiguration.setCharSet(charSet);
        globalConfiguration.setCharSetPath(Constants.ROMSET_PROVIDED);

        globalConfiguration.setBackgroundImage(screen);
        globalConfiguration.setBackgroundImagePath(Constants.ROMSET_PROVIDED);

        dandanatorMiniConfiguration.setExtraRom(extraRom);
        dandanatorMiniConfiguration.setExtraRomPath(Constants.ROMSET_PROVIDED);

        dandanatorMiniConfiguration.setExtraRomMessage(extraRomMessage);
        dandanatorMiniConfiguration.setTogglePokesMessage(togglePokesMessage);
        dandanatorMiniConfiguration.setLaunchGameMessage(launchGameMessage);
        dandanatorMiniConfiguration.setSelectPokesMessage(selectPokesMessage);

    }

    static class GameChunk {
        public int addr;
        public int length;
        public byte[] data;
    }

    static class GameCBlock {
        public int initSlot;
        public int start;
        public int size;
        public boolean compressed;
        public byte[] data;
        public int getInitSlot() {
            return initSlot;
        }
        public int getStart() {
            return start;
        }
    }

    static class GameDataHolder {
        private SNAHeader snaHeader;
        private String name;
        private boolean isGameCompressed;
        private int gameType;
        private boolean screenHold;
        private boolean activeRom;
        private byte[] launchCode;
        private int ramAddr;
        private GameChunk gameChunk;
        private List<GameCBlock> cBlocks = new ArrayList<>();
        private TrainerList trainerList = new TrainerList(null);
        private int trainerCount;

        public static GameDataHolder fromRomSet(TrackeableInputStream is) throws IOException {
            LOGGER.debug("About to read game data. Offset is " + is.position());
            GameDataHolder holder = new GameDataHolder();
            holder.snaHeader = SNAHeader.fromInputStream(is, DandanatorMiniCompressedRomSetHandler.SNA_HEADER_SIZE);
            holder.name = Util.getNullTerminatedString(is, 3, DandanatorMiniConstants.GAMENAME_SIZE);
            holder.isGameCompressed = is.read() != 0;
            holder.gameType = is.read();
            holder.screenHold = is.read() != 0;
            holder.activeRom = is.read() != 0;
            holder.launchCode = Util.fromInputStream(is, DandanatorMiniCompressedRomSetHandler.GAME_LAUNCH_SIZE);
            holder.ramAddr = is.getAsLittleEndian();
            holder.gameChunk = new GameChunk();
            holder.gameChunk.addr = is.getAsLittleEndian();
            holder.gameChunk.length = is.getAsLittleEndian();
            for (int i = 0; i < 9; i++) {
                GameCBlock cblock = new GameCBlock();
                cblock.initSlot = is.read();
                cblock.start = is.getAsLittleEndian();
                cblock.size = is.getAsLittleEndian();
                cblock.compressed = holder.isGameCompressed;
                if (cblock.initSlot < 0xFF) {
                    holder.getCBlocks().add(cblock);
                }
            }
            LOGGER.debug("Read game data. Offset is " + is.position());
            return holder;
        }

        public TrainerList getTrainerList() {
            return trainerList;
        }

        public List<GameCBlock> getCBlocks() {
            return cBlocks;
        }

        public void setTrainerCount(int trainerCount) {
            this.trainerCount = trainerCount;
        }

        public List<byte[]> getGameSlots() {
            List<byte[]> gameSlots = new ArrayList<>();
            int index = 0;
            for (GameCBlock cBlock: cBlocks) {
                if (index == DandanatorMiniConstants.GAME_CHUNK_SLOT) {
                    gameSlots.add(Util.concatArrays(cBlock.data, gameChunk.data));
                } else {
                    gameSlots.add(cBlock.data);
                }
                index++;
            }
            return gameSlots;
        }

        public int getTrainerCount() {
            return trainerCount;
        }

        public void exportTrainers(RamGame game) {
            trainerList.setOwner(game);
            game.setTrainerList(trainerList);
        }

        public Game createGame() {
            GameType type = GameType.byTypeId(gameType);
            Game game;
            switch (type) {
                case ROM:
                    game = new RomGame(getGameSlots().get(0));
                    break;
                case RAM16:
                case RAM48:
                case RAM128_LO:
                case RAM128_HI:
                    RamGame ramGame = new RamGame(type, getGameSlots());
                    ramGame.setHoldScreen(screenHold);
                    ramGame.setRom(activeRom);
                    ramGame.setSnaHeader(snaHeader);
                    ramGame.setTrainerList(trainerList);
                    game = ramGame;
                    break;
                default:
                    LOGGER.error("Unsupported type of game " + type.screenName());
                    throw new IllegalArgumentException("Unsupported game type");
            }
            game.setName(name);
            return game;
        }

    }
}
