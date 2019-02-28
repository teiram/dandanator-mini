package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.daad.DAADConstants;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;

public class DanTapGame implements RamGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanTapGame.class);

    protected StringProperty name;
    protected List<byte[]> data;
    protected GameType gameType;
    private HardwareMode hardwareMode;
    private Image screenshot;
    private IntegerProperty size;
    private List<byte[]> tapBlocks;

    private int calculateSize() {
        int slot = 0;
        int tapTableSize = (tapBlocks.size() + 1) * DanTapConstants.TAP_TABLE_ENTRY_SIZE;
        for (byte[] block : tapBlocks) {


        }
        return 0;
    }
    public DanTapGame(List<byte[]> tapBlocks) {
        this.gameType = GameType.DAN_TAP;
        this.tapBlocks = tapBlocks;
        this.size = new SimpleIntegerProperty(calculateSize());
        this.hardwareMode = HardwareMode.HW_48K;
        this.name = new SimpleStringProperty();

    }

    @Override
    public GameType getType() {
        return null;
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public void setName(String name) {
        this.name.set(name);
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    @Override
    public byte[] getSlot(int slot) {
        return new byte[0];
    }

    @Override
    public int getSlotCount() {
        return 0;
    }

    @Override
    public Observable[] getObservable() {
        return new Observable[0];
    }

    @Override
    public boolean isSlotZeroed(int slot) {
        return false;
    }

    public Image getScreenshot() {
        if (screenshot == null) {
            try {
                //Use default image for DAAD games
                screenshot = ImageUtil
                        .scrLoader(ImageUtil.newScreenshot(),
                                new ByteArrayInputStream(DAADConstants.getDefaultScreen()));
            } catch (Exception e) {
                LOGGER.error("Loading screenshot", e);
            }
        }
        return screenshot;
    }

    @Override
    public HardwareMode getHardwareMode() {
        return hardwareMode;
    }

    @Override
    public void setHardwareMode(HardwareMode hardwareMode) {
        this.hardwareMode = hardwareMode;
    }

    @Override
    public void setScreenshot(Image screenshot) {
        this.screenshot = screenshot;
    }

    @Override
    public int getSize() {
        return size.get();
    }

    public IntegerProperty sizeProperty() {
        return size;
    }

    public void setSize(int size) {
        this.size.set(size);
    }

    @Override
    public String toString() {
        return "DanSnapGame{" +
                " name=" + name +
                ", hardwareMode=" + hardwareMode +
                '}';
    }

}
