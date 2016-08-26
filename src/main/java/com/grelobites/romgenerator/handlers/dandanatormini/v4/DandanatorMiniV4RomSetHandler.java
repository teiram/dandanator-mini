package com.grelobites.romgenerator.handlers.dandanatormini.v4;

import com.grelobites.romgenerator.Configuration;
import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.handlers.dandanatormini.model.DandanatorMiniImporter;
import com.grelobites.romgenerator.handlers.dandanatormini.model.SlotZero;
import com.grelobites.romgenerator.handlers.dandanatormini.view.DandanatorMiniFrameController;
import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.Poke;
import com.grelobites.romgenerator.model.PokeViewable;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.LocaleUtil;
import com.grelobites.romgenerator.util.OperationResult;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.Z80Opcode;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.util.ZxScreen;
import com.grelobites.romgenerator.util.pokeimporter.ImportContext;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandler;
import com.grelobites.romgenerator.ApplicationContext;
import com.grelobites.romgenerator.util.CompletedTask;
import com.grelobites.romgenerator.util.romsethandler.RomSetHandlerType;
import com.grelobites.romgenerator.view.util.DialogUtil;
import com.grelobites.romgenerator.view.util.DirectoryAwareFileChooser;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Pane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DandanatorMiniV4RomSetHandler implements RomSetHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DandanatorMiniV4RomSetHandler.class);

    protected static final int VERSION_SIZE = 32;

    private static final int SAVEDGAMECHUNK_SIZE = 256;
    private static final int POKE_SPACE_SIZE = 3230;
    private static final int RESERVED_GAMETABLE_SIZE = 64;

    protected static final int SCREEN_THIRD_PIXEL_SIZE = 2048;
    protected static final int SCREEN_THIRD_ATTRINFO_SIZE = 256;

    private ZxScreen menuImage;
    private BooleanProperty generationAllowedProperty = new SimpleBooleanProperty(false);
    protected ApplicationContext applicationContext;
    protected DandanatorMiniFrameController dandanatorMiniFrameController;
    protected Pane dandanatorMiniFrame;
    protected MenuItem exportPokesMenuItem;
    protected MenuItem importPokesMenuItem;

    private InvalidationListener updateImageListener =
            (c) -> updateMenuPreview();

    private InvalidationListener updateRomUsage =
            (c) -> updateRomUsage();

    public DandanatorMiniV4RomSetHandler() throws IOException {
        menuImage = new ZxScreen();
        updateBackgroundImage(menuImage);
    }

    protected static void updateBackgroundImage(WritableImage image) throws IOException {
        ImageUtil.scrLoader(image,
                new ByteArrayInputStream(Configuration.getInstance()
                        .getBackgroundImage()));
    }

    protected double calculateRomUsage() {
        return (double) getApplicationContext().getGameList().size() / DandanatorMiniConstants.SLOT_COUNT;
    }

    protected String generateRomUsageDetail() {
        return String.format(LocaleUtil.i18n("romUsageV4Detail"),
                getApplicationContext().getGameList().size(),
                DandanatorMiniConstants.SLOT_COUNT);
    }

    private void updateRomUsage() {
        getApplicationContext().setRomUsage(calculateRomUsage());
        getApplicationContext().setRomUsageDetail(generateRomUsageDetail());
    }

    protected ApplicationContext getApplicationContext() {
        if (applicationContext == null) {
            throw new IllegalStateException("RomSetHandler is currently not bound to any Application");
        } else {
            return applicationContext;
        }
    }

    public void exportCurrentGamePokes() {
        Game game = getApplicationContext().selectedGameProperty().get();
        if (game != null && game instanceof RamGame) {
            if (GameUtil.gameHasPokes(game)) {
                DirectoryAwareFileChooser chooser = getApplicationContext().getFileChooser();
                chooser.setTitle(LocaleUtil.i18n("exportCurrentGamePokes"));
                final File saveFile = chooser.showSaveDialog(dandanatorMiniFrame.getScene().getWindow());
                if (saveFile != null) {
                    try {
                        GameUtil.exportPokesToFile((RamGame) game, saveFile);
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
        if (game != null && game instanceof RamGame) {
            DirectoryAwareFileChooser chooser = getApplicationContext().getFileChooser();
            chooser.setTitle(LocaleUtil.i18n("importCurrentGamePokes"));
            final File loadFile = chooser.showOpenDialog(dandanatorMiniFrame.getScene().getWindow());
            if (loadFile != null) {
                try {
                    ImportContext ctx = new ImportContext(loadFile);
                    GameUtil.importPokesFromFile((RamGame) game, ctx);
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

    protected MenuItem getExportPokesMenuItem() {
        if (exportPokesMenuItem == null) {
            exportPokesMenuItem = new MenuItem(LocaleUtil.i18n("exportPokesMenuEntry"));

            exportPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+P")
            );
            exportPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            exportPokesMenuItem.setOnAction(f -> {
                try {
                    exportCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Exporting current game pokes", e);
                }
            });
        }
        return exportPokesMenuItem;
    }

    protected MenuItem getImportPokesMenuItem() {
        if (importPokesMenuItem == null) {
            importPokesMenuItem = new MenuItem(LocaleUtil.i18n("importPokesMenuEntry"));

            importPokesMenuItem.setAccelerator(
                    KeyCombination.keyCombination("SHORTCUT+L")
            );
            importPokesMenuItem.disableProperty().bind(applicationContext
                    .gameSelectedProperty().not());
            importPokesMenuItem.setOnAction(f -> {
                try {
                    importCurrentGamePokes();
                } catch (Exception e) {
                    LOGGER.error("Importing current game pokes", e);
                }
            });
        }
        return importPokesMenuItem;
    }

    private static byte[] asNullTerminatedByteArray(String name, int arrayLength) {
        String trimmedName =
                name.length() < arrayLength ?
                        name : name.substring(0, arrayLength - 1);
        byte[] result = new byte[arrayLength];
        System.arraycopy(trimmedName.getBytes(), 0, result, 0, trimmedName.length());
        result[trimmedName.length()] = 0;
        return result;
    }

    protected static void dumpGameName(OutputStream os, Game game, int index) throws IOException {
        String gameName = String.format("%d%c %s", (index + 1) % DandanatorMiniConstants.SLOT_COUNT,
                isGameRom(game) ? 'r' : '.',
                game.getName());
        os.write(asNullTerminatedByteArray(gameName, DandanatorMiniConstants.GAMENAME_SIZE));
    }

    private static void dumpGameSnaHeader(OutputStream os, RamGame game) throws IOException {
        os.write(Arrays.copyOfRange(game.getSnaHeader().asByteArray(), 0, Constants.SNA_HEADER_SIZE));
    }

    protected static int dumpGameLaunchCode(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            if (game.getType() == GameType.RAM48) {
                RamGame ramGame = (RamGame) game;

                os.write(Z80Opcode.PUSH_HL);
                os.write(Z80Opcode.POP_HL);
                os.write(Z80Opcode.PUSH_HL);
                os.write(Z80Opcode.POP_HL);
                os.write((ramGame.getSnaHeader().asByteArray()[SNAHeader.INTERRUPT_ENABLE] & 0x04) == 0 ?
                        Z80Opcode.DI : Z80Opcode.EI);
                os.write(Z80Opcode.RET);
                return 6;
            } else {
                throw new IllegalArgumentException("Not implemented yet");
            }
        } else {
            throw new IllegalStateException("Unsupported game type");
        }
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


    protected static byte[] asLittleEndianWord(int value) {
        return new byte[]{
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)};
    }

    protected static void dumpGameRamCodeLocation(OutputStream os, Game game, int requiredSize) throws IOException {
        int ramAddress = ImageUtil.getHiddenDisplayOffset(game.getSlot(0), requiredSize)
                .orElse(Constants.SPECTRUM_SCREEN_SIZE - requiredSize);
        ramAddress += Constants.SPECTRUM_SCREEN_OFFSET;
        os.write(asLittleEndianWord(ramAddress));
        LOGGER.debug(String.format("RAM Address calculated as 0x%04X", ramAddress));
    }

    private static void dumpGameSavedChunk(OutputStream os, Game game) throws IOException {
        byte[] lastSlot = game.getSlot(game.getSlotCount() - 1);
        os.write(Arrays.copyOfRange(lastSlot, lastSlot.length - SAVEDGAMECHUNK_SIZE, lastSlot.length));
    }

    private void dumpGameTable(OutputStream os, RamGame game, int index) throws IOException {
        dumpGameName(os, game, index);
        dumpGameSnaHeader(os, game);
        os.write(isGameScreenHold(game) ? Constants.B_01 : Constants.B_00);
        os.write(isGameRom(game) ? Constants.B_10 : Constants.B_00);
        int codeSize = dumpGameLaunchCode(os, game);
        dumpGameRamCodeLocation(os, game, codeSize);
        fillWithValue(os, (byte) 0, RESERVED_GAMETABLE_SIZE);
        dumpGameSavedChunk(os, game);
    }

    protected static int pokeRequiredSize(Game game) {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            int headerSize = 25; //Fixed size required per poke
            //Sum of all the addressValues * 3 (address + value)
            int size = ramGame.getTrainerList().getChildren().stream()
                    .map(p -> p.getChildren().size() * 3).reduce(0, (a, b) -> a + b);
            return size + headerSize * ramGame.getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static int getGamePokeCount(Game game) {
        if (game instanceof RamGame) {
            return ((RamGame) game).getTrainerList().getChildren().size();
        } else {
            return 0;
        }
    }

    protected static void dumpGamePokeData(OutputStream os, Game game) throws IOException {
        if (game instanceof RamGame) {
            RamGame ramGame = (RamGame) game;
            int index = 1;
            for (PokeViewable trainer : ramGame.getTrainerList().getChildren()) {
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

    protected static void fillWithValue(OutputStream os, byte value, int size) throws IOException {
        for (int i = 0; i < size; i++) {
            os.write(value);
        }
    }

    protected void dumpVersionInfo(OutputStream os) throws IOException {
        os.write(asNullTerminatedByteArray(getVersionInfo(), VERSION_SIZE));
    }

    private void dumpGameData(OutputStream os, Game game) throws IOException {
        for (int i = 0; i < 3; i++) {
            os.write(game.getSlot(i));
        }
    }

    @Override
    public void exportRomSet(OutputStream stream) {
        try {
            Configuration configuration = Configuration.getInstance();
            DandanatorMiniConfiguration dmConfiguration = DandanatorMiniConfiguration.getInstance();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            //Only RamGame type supported in this RomSetHandler
            Collection<RamGame> games = Util.collectionUpcast(getApplicationContext().getGameList());
            os.write(dmConfiguration.getDandanatorRom(), 0, DandanatorMiniConstants.BASEROM_SIZE);
            LOGGER.debug("Dumped base ROM. Offset: " + os.size());

            int firmwareHeaderLength = DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.length();
            os.write(DandanatorMiniConstants.DANDANATOR_PIC_FW_HEADER.getBytes(), 0, firmwareHeaderLength);
            os.write(dmConfiguration.getDandanatorPicFirmware(), 0,
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0);
            LOGGER.debug("Dumped first chunk of PIC firmware. Offset: " + os.size());

            os.write(configuration.getCharSet(), 0, Constants.CHARSET_SIZE);
            LOGGER.debug("Dumped charset. Offset: " + os.size());

            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), 0, SCREEN_THIRD_PIXEL_SIZE));
            os.write(Arrays.copyOfRange(configuration.getBackgroundImage(), Constants.SPECTRUM_SCREEN_SIZE,
                    Constants.SPECTRUM_SCREEN_SIZE + SCREEN_THIRD_ATTRINFO_SIZE));
            LOGGER.debug("Dumped screen. Offset: " + os.size());

            dumpScreenTexts(os, dmConfiguration);
            LOGGER.debug("Dumped TextData. Offset: " + os.size());

            os.write((byte) games.size());
            LOGGER.debug("Dumped game count. Offset: " + os.size());

            int index = 0;
            for (RamGame game : games) {
                dumpGameTable(os, game, index++);
            }
            LOGGER.debug("Dumped game table. Offset: " + os.size());

            int pokeStartMark = os.size(); //Mark position before start of poke zone
            for (RamGame game : games) {
                byte pokeCount = (byte) game.getTrainerList().getChildren().size();
                os.write(pokeCount);
            }
            LOGGER.debug("Dumped poke main header. Offset: " + os.size());
            int basePokeAddress = os.size() + 20; //Add the address header

            for (RamGame game : games) {
                os.write(asLittleEndianWord(basePokeAddress));
                basePokeAddress += pokeRequiredSize(game);
            }
            LOGGER.debug("Dumped poke headers. Offset: " + os.size());
            for (RamGame game : games) {
                dumpGamePokeData(os, game);
            }
            fillWithValue(os, (byte) 0, POKE_SPACE_SIZE - (os.size() - pokeStartMark));
            LOGGER.debug("Dumped poke data. Offset: " + os.size());

            os.write(dmConfiguration.getDandanatorPicFirmware(),
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_0,
                    DandanatorMiniConstants.DANDANATOR_PIC_FW_SIZE_1);
            LOGGER.debug("Dumped second chunk of PIC firmware. Offset: " + os.size());

            fillWithValue(os, (byte) 0, Constants.SLOT_SIZE - os.size() - VERSION_SIZE);
            LOGGER.debug("Dumped padding zone. Offset: " + os.size());

            dumpVersionInfo(os);
            LOGGER.debug("Dumped version info. Offset: " + os.size());

            for (Game game : games) {
                dumpGameData(os, game);
                LOGGER.debug("Dumped game. Offset: " + os.size());
            }

            os.write(dmConfiguration.getExtraRom());
            LOGGER.debug("Dumped custom rom. Offset: " + os.size());

            os.flush();
            LOGGER.debug("All parts dumped and flushed. Offset: " + os.size());

            stream.write(os.toByteArray());
        } catch (Exception e) {
            LOGGER.error("Creating RomSet", e);
        }
    }

    @Override
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

    @Override
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

    protected DandanatorMiniFrameController getDandanatorMiniFrameController(ApplicationContext applicationContext) {
        if (dandanatorMiniFrameController == null) {
            dandanatorMiniFrameController = new DandanatorMiniFrameController();
        }
        dandanatorMiniFrameController.setApplicationContext(applicationContext);
        return dandanatorMiniFrameController;
    }

    protected Pane getDandanatorMiniFrame(ApplicationContext applicationContext) {
        try {
            if (dandanatorMiniFrame == null) {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(DandanatorMiniFrameController.class.getResource("dandanatorminiframe.fxml"));
                loader.setController(getDandanatorMiniFrameController(applicationContext));
                loader.setResources(LocaleUtil.getBundle());
                dandanatorMiniFrame = loader.load();
            } else {
                dandanatorMiniFrameController.setApplicationContext(applicationContext);
            }
            return dandanatorMiniFrame;
        } catch (Exception e) {
            LOGGER.error("Creating DandanatorMini frame", e);
            throw new RuntimeException(e);
        }
    }

    protected BooleanBinding getGenerationAllowedBinding(ApplicationContext context) {
        return Bindings.size(applicationContext.getGameList())
                .isEqualTo(DandanatorMiniConstants.SLOT_COUNT);
    }

    @Override
    public RomSetHandlerType type() {
        return RomSetHandlerType.DDNTR_V4;
    }

    public void bind(ApplicationContext applicationContext) {
        LOGGER.debug("Binding RomSetHandler to ApplicationContext");
        this.applicationContext = applicationContext;
        generationAllowedProperty.bind(getGenerationAllowedBinding(applicationContext));

        applicationContext.getRomSetHandlerInfoPane().getChildren()
                .add(getDandanatorMiniFrame(applicationContext));
        updateMenuPreview();
        applicationContext.getMenuPreview().setImage(menuImage);

        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .addListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().backgroundImagePathProperty()
                .addListener(updateImageListener);
        Configuration.getInstance().charSetPathProperty()
                .addListener(updateImageListener);

        applicationContext.getGameList().addListener(updateImageListener);
        applicationContext.getGameList().addListener(updateRomUsage);

        applicationContext.getExtraMenu().getItems().addAll(
                getExportPokesMenuItem(), getImportPokesMenuItem());

        updateRomUsage();

    }

    public void unbind() {
        LOGGER.debug("Unbinding RomSetHandler from ApplicationContext");
        DandanatorMiniConfiguration.getInstance().togglePokesMessageProperty()
                .removeListener(updateImageListener);
        DandanatorMiniConfiguration.getInstance().extraRomMessageProperty()
                .removeListener(updateImageListener);
        generationAllowedProperty.unbind();
        generationAllowedProperty.set(false);
        applicationContext.getRomSetHandlerInfoPane().getChildren().clear();

        applicationContext.getExtraMenu().getItems().removeAll(
                getExportPokesMenuItem(),
                getImportPokesMenuItem());
        applicationContext.getGameList().removeListener(updateImageListener);
        applicationContext.getGameList().removeListener(updateRomUsage);
        applicationContext = null;
    }


    protected static boolean isGameScreenHold(Game game) {
        return game instanceof RamGame && ((RamGame) game).getHoldScreen();
    }

    protected static boolean isGameRom(Game game) {
        return game instanceof RamGame && ((RamGame) game).getRom();
    }

    protected static boolean isGameCompressed(Game game) {
        return game instanceof RamGame && ((RamGame) game).getCompressed();
    }

    protected static boolean isGameForce128kMode(Game game) {
        return game instanceof RamGame && ((RamGame) game).isForce128kMode();
    }

    @Override
    public void updateMenuPreview() {
        LOGGER.debug("updateMenuPreview");
        try {
            DandanatorMiniConfiguration configuration = DandanatorMiniConfiguration.getInstance();

            updateBackgroundImage(menuImage);
            menuImage.setCharSet(Configuration.getInstance().getCharSet());

            menuImage.setInk(ZxColor.BLACK);
            menuImage.setPen(ZxColor.BRIGHTMAGENTA);
            for (int line = menuImage.getLines() - 1; line >= 8; line--) {
                menuImage.deleteLine(line);
            }

            menuImage.printLine(getVersionInfo(), 8, 0);

            int line = 10;
            int index = 1;

            for (Game game : getApplicationContext().getGameList()) {
                menuImage.setPen(
                        isGameScreenHold(game) ? ZxColor.BRIGHTCYAN : ZxColor.BRIGHTGREEN);
                menuImage.deleteLine(line);
                menuImage.printLine(
                        String.format("%d%c %s", index % DandanatorMiniConstants.SLOT_COUNT,
                                isGameRom(game) ? 'r' : '.',
                                game.getName()),
                        line++, 0);
                index++;
            }
            while (index <= DandanatorMiniConstants.SLOT_COUNT) {
                menuImage.deleteLine(line);
                menuImage.setPen(ZxColor.WHITE);
                menuImage.printLine(String
                        .format("%d.", index % DandanatorMiniConstants.SLOT_COUNT), line++, 0);
                index++;
            }

            menuImage.setPen(ZxColor.BRIGHTBLUE);
            menuImage.printLine(String.format("P. %s", configuration.getTogglePokesMessage()), 21, 0);
            menuImage.setPen(ZxColor.BRIGHTRED);
            menuImage.printLine(String.format("R. %s", configuration.getExtraRomMessage()), 23, 0);
        } catch (Exception e) {
            LOGGER.error("Updating background screen", e);
        }
    }

    @Override
    public BooleanProperty generationAllowedProperty() {
        return generationAllowedProperty;
    }

    @Override
    public Future<OperationResult> addGame(Game game) {
        Future<OperationResult> completedTask = CompletedTask.successTask();
        if (game.getType() == GameType.RAM48) {
            if (game instanceof RamGame) {
                int numGames = getApplicationContext().getGameList().size();
                if (numGames < DandanatorMiniConstants.SLOT_COUNT) {
                    getApplicationContext().getGameList().add(game);
                }
            } else {
                LOGGER.warn("Non RAM games are not supported by this RomSethandler");
                completedTask = new CompletedTask(OperationResult.errorResult("Adding Game", "Unsupported Game"));
            }
        } else {
            LOGGER.warn("Non RAM48 games are not supported by this RomSetHandler");
            completedTask = new CompletedTask(OperationResult.errorResult("Adding Game", "Unsupported Game"));
        }
        return completedTask;
    }
}

