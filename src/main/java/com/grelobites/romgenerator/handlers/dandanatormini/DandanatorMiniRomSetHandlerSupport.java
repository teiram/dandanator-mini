package com.grelobites.romgenerator.handlers.dandanatormini;

import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.Poke;
import com.grelobites.romgenerator.model.PokeViewable;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

public class DandanatorMiniRomSetHandlerSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniRomSetHandlerSupport.class);
    protected ApplicationContext applicationContext;

    protected ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("RomSetHandler is currently not bound to any Application");
        } else {
            return applicationContext;
        }
    }

    protected static void updateBackgroundImage(WritableImage image) throws IOException {
        ImageUtil.scrLoader(image,
                new ByteArrayInputStream(Configuration.getInstance()
                        .getBackgroundImage()));
    }

    protected static byte[] asNullTerminatedByteArray(String name, int arrayLength) {
        String trimmedName =
                name.length() < arrayLength ?
                        name : name.substring(0, arrayLength - 1);
        byte[] result = new byte[arrayLength];
        System.arraycopy(trimmedName.getBytes(StandardCharsets.ISO_8859_1), 0, result, 0, trimmedName.length());
        result[trimmedName.length()] = 0;
        return result;
    }

    protected static int getGamePokeCount(Game game) {
        if (game instanceof SnapshotGame) {
            return ((SnapshotGame) game).getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static int pokeRequiredSize(Game game) {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            int headerSize = 25; //Fixed size required per poke
            //Sum of all the addressValues * 3 (address + value)
            int size = snapshotGame.getTrainerList().getChildren().stream()
                    .map(p -> p.getChildren().size() * 3).reduce(0, (a, b) -> a + b);
            return size + headerSize * snapshotGame.getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static void dumpGamePokeData(OutputStream os, Game game) throws IOException {
        if (game instanceof SnapshotGame) {
            SnapshotGame snapshotGame = (SnapshotGame) game;
            int index = 1;
            for (PokeViewable trainer : snapshotGame.getTrainerList().getChildren()) {
                os.write((byte) trainer.getChildren().size());
                os.write(asNullTerminatedByteArray(String.format("%d. %s",
                        index++, trainer.getViewRepresentation()), 24));
                for (PokeViewable viewable : trainer.getChildren()) {
                    Poke poke = (Poke) viewable;
                    os.write(poke.addressBytes());
                    os.write(poke.valueBytes());
                }
            }
        }
    }

    protected static String getVersionInfo() {
        return String.format("v%s", Util.stripSnapshotVersion(Constants.currentVersion()));
    }

    protected static boolean isGameScreenHold(Game game) {
        return game instanceof SnapshotGame && ((SnapshotGame) game).getHoldScreen();
    }

    protected static boolean isGameRomActive(Game game) {
        return game instanceof SnapshotGame && !((SnapshotGame) game).getRom().equals(
                DandanatorMiniConstants.INTERNAL_ROM_GAME);
    }

    protected static boolean isGameCompressed(Game game) {
        return game instanceof SnapshotGame && ((SnapshotGame) game).getCompressed();
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        String gameName = String.format("%d%c %s", (index + 1) % DandanatorMiniConstants.SLOT_COUNT,
                isGameRomActive(game) ? 'r' : '.',
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorMiniConstants.GAMENAME_SIZE));
    }

    protected static byte[] asLittleEndianWord(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)};
    }

    protected static void dumpScreenTexts(OutputStream os, DandanatorMiniConfiguration configuration) throws IOException {
        os.write(asNullTerminatedByteArray(String.format("R. %s", configuration.getExtraRomMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("P. %s", configuration.getTogglePokesMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(String.format("0. %s", configuration.getLaunchGameMessage()),
                DandanatorMiniConstants.GAMENAME_SIZE));
        os.write(asNullTerminatedByteArray(configuration.getSelectPokesMessage(), DandanatorMiniConstants.GAMENAME_SIZE));
    }

    public void exportCurrentGamePokes() {
        Game game = getApplicationContext().selectedGameProperty().get();
        if (game != null && game instanceof SnapshotGame) {
            if (GameUtil.gameHasPokes(game)) {
                DirectoryAwareFileChooser chooser = getApplicationContext().getFileChooser();
                chooser.setTitle(LocaleUtil.i18n("exportCurrentGamePokes"));
                chooser.setInitialFileName(game.getName() + ".pok");
                final File saveFile = chooser.showSaveDialog(applicationContext.getApplicationStage());
                if (saveFile != null) {
                    try {
                        GameUtil.exportPokesToFile((SnapshotGame) game, saveFile);
                    } catch (IOException e) {
                        LOGGER.error("Exporting Game Pokes", e);
                    }
                }
            } else {
                DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGamePokesErrorTitle"),
                        LocaleUtil.i18n("exportCurrentGamePokesErrorHeader"),
                        LocaleUtil.i18n("exportCurrentGamePokesErrorContentNoPokesInGame")).showAndWait();
            }
        } else {
            DialogUtil.buildWarningAlert(LocaleUtil.i18n("exportCurrentGamePokesErrorTitle"),
                    LocaleUtil.i18n("exportCurrentGamePokesErrorHeader"),
                    LocaleUtil.i18n("exportCurrentGamePokesErrorContentNoGameSelected")).showAndWait();
        }
    }

    public void importCurrentGamePokes() {
        Game game = getApplicationContext().selectedGameProperty().get();
        if (game != null && game instanceof SnapshotGame) {
            DirectoryAwareFileChooser chooser = getApplicationContext().getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("importCurrentGamePokes"));
            final File loadFile = chooser.showOpenDialog(applicationContext.getApplicationStage());
            if (loadFile != null) {
                try {
                    ImportContext ctx = new ImportContext(loadFile);
                    GameUtil.importPokesFromFile((SnapshotGame) game, ctx);
                    if (ctx.hasErrors()) {
                        LOGGER.debug("Detected errors in pokes import operation");
                        DialogUtil.buildWarningAlert(LocaleUtil.i18n("importPokesWarning"),
                                LocaleUtil.i18n("importPokesWarningHeader"),
                                ctx.getImportErrors().stream()
                                        .distinct()
                                        .collect(Collectors.joining("\n"))).showAndWait();
                    }
                } catch (Exception e) {
                    LOGGER.error("During poke import", e);
                }
            }
        }
    }

    public void importRomSet(InputStream stream) {
        try {
            Optional<SlotZero> slotZero = SlotZero.getImplementation(Util.fromInputStream(stream, Constants.SLOT_SIZE));
            if (slotZero.isPresent()) {
                DandanatorMiniImporter importer = slotZero.get().getImporter();
                importer.importRomSet(slotZero.get(), stream, applicationContext);
            } else {
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("fileImportError"),
                        LocaleUtil.i18n("fileImportErrorHeader"),
                        LocaleUtil.i18n("fileImportErrorContent"))
                        .showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Importing RomSet", e);
        }
    }

    public void mergeRomSet(InputStream stream) {
        try {
            Optional<SlotZero> slotZero = SlotZero.getImplementation(Util.fromInputStream(stream, Constants.SLOT_SIZE));
            if (slotZero.isPresent()) {
                DandanatorMiniImporter importer = slotZero.get().getImporter();
                importer.mergeRomSet(slotZero.get(), stream, applicationContext);
            } else {
                DialogUtil.buildErrorAlert(
                        LocaleUtil.i18n("fileImportError"),
                        LocaleUtil.i18n("fileImportErrorHeader"),
                        LocaleUtil.i18n("fileImportErrorContent"))
                        .showAndWait();
            }
        } catch (Exception e) {
            LOGGER.error("Merging RomSet", e);
        }
    }
}