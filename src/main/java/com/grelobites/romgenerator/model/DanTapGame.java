package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConfiguration;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.Pair;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.compress.Compressor;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DanTapGame implements RamGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanTapGame.class);
    private Compressor compressor = DandanatorMiniConfiguration.getInstance().getCompressor();

    protected StringProperty name;
    protected GameType gameType;
    private HardwareMode hardwareMode;
    private Image screenshot;
    private IntegerProperty size;
    private List<byte[]> tapBlocks;
    private List<byte[]> slots;
    private DanTapTable tapTable;

    private Pair<byte[], Boolean> getCompressedBlock(byte[] block) {
        try {
            byte[] compressedBlock = Util.compress(block);
            if (compressedBlock.length < block.length) {
                return new Pair<>(compressedBlock, true);
            }
        } catch (Exception e) {
            LOGGER.error("Compressing block", e);
        }
        return new Pair<>(block, false);
    }

    private void prepareSlots() throws IOException {
        int tapTableSize = (tapBlocks.size() + 1) * DanTapConstants.TAP_TABLE_ENTRY_SIZE;
        ByteArrayOutputStream slot = new ByteArrayOutputStream();
        slot.write(DanTapConstants.getCommonCode());
        slot.write(DanTapConstants.getTableCode());
        int tapTableOffset = slot.size();
        slot.write(new byte[tapTableSize]);
        int slotDelta = 0;

        for (byte[] block : tapBlocks) {
            Pair<byte[], Boolean> compressResult = getCompressedBlock(block);
            byte[] data = compressResult.left();
            tapTable.addEntry(DanTapTableEntry.builder()
                    .withSlot(slotDelta)
                    .withCompressedSize(block.length)
                    .withSize(data.length)
                    .withOffset(slot.size())
                    .withCompressed(compressResult.right())
                    .build());
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            while (slot.size() < Constants.SLOT_SIZE && bis.available() > 0) {
                int segmentSize = Math.min(data.length, Constants.SLOT_SIZE - slot.size());
                slot.write(Util.fromInputStream(bis, segmentSize));
                if (slot.size() == Constants.SLOT_SIZE) {
                    slots.add(slot.toByteArray());
                    slotDelta++;
                    slot.reset();
                    slot.write(DanTapConstants.getCommonCode());
                }
            }
        }
        if (slot.size() > 0) {
            slots.add(slot.toByteArray());
        }
        byte[] tapTableBytes = tapTable.toByteArray();
        System.arraycopy(tapTableBytes, 0, slots.get(0), tapTableOffset,
                tapTableBytes.length);
    }

    public DanTapGame(List<byte[]> tapBlocks) throws IOException {
        this.gameType = GameType.DAN_TAP;
        this.tapBlocks = tapBlocks;
        this.hardwareMode = HardwareMode.HW_48K;
        this.name = new SimpleStringProperty();

        this.slots = new ArrayList<>();
        prepareSlots();
        this.size = new SimpleIntegerProperty(this.slots.size() * Constants.SLOT_SIZE);
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
        return slots.get(slot);
    }

    @Override
    public int getSlotCount() {
        return slots.size();
    }

    @Override
    public Observable[] getObservable() {
        return new Observable[] {name};
    }

    @Override
    public boolean isSlotZeroed(int slot) {
        return false;
    }

    public Image getScreenshot() {
        if (screenshot == null) {
            try {
                //TODO: See how to provide an image (maybe from a TAP entry with screen size)
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
        return "DanTapGame{" +
                "name=" + name +
                ", gameType=" + gameType +
                ", hardwareMode=" + hardwareMode +
                ", size=" + size +
                '}';
    }
}
