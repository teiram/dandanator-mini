package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DAADGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DAADGenerator.class);

    private static class SlotContainer {
        public int remaining = Constants.SLOT_SIZE;
        public List<RelocatableItem> items = new ArrayList<>();
    }

    private DAADData data;

    public DAADGenerator(DAADData data) {
        this.data = data;
    }

    private static void performBinPack(List<SlotContainer> slots, List<RelocatableItem> items) {
        //TODO

    }

    private void writeMldMetadata(byte[] slotData, DAADData data) {

    }

    private byte[] getSlotData(SlotContainer slot) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (RelocatableItem item : slot.items) {
            LOGGER.debug("Writing Item of size {}", item.getSize());
            os.write(item.getData());
        }
        LOGGER.debug("Writing gap at end of size {}", slot.remaining);
        Util.fillWithValue(os, (byte) 0, slot.remaining);
        return os.toByteArray();
    }

    public InputStream generate() throws IOException {
        final List<SlotContainer> slots = new ArrayList<>();
        SlotContainer slot0 = new SlotContainer();
        slot0.remaining -= DAADConstants.getDAADLoader().length;
        slot0.remaining -= DAADConstants.METADATA_SIZE;
        if (data.getScreen() != null) {
            DAADScreen screen = data.getScreen();
            byte[] compressedScreen = Util.compress(screen.getData());
            screen.setSlotOffset(Constants.SLOT_SIZE - slot0.remaining);
            screen.setData(compressedScreen);
            slot0.remaining -= compressedScreen.length;
        }
        LOGGER.debug("Remaining space in slot0: {}", slot0.remaining);
        slots.add(slot0);
        List<RelocatableItem> items = new ArrayList<>();
        for (int i = 0; i < DAADConstants.BINARY_PARTS; i++) {
            DAADBinary part = data.getBinaryPart(i);
            part.setData(Util.compress(part.getData()));
            items.add(part);
        }
        items.addAll(data.getDAADResources());
        performBinPack(slots, items);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int slotId = 0;
        for (SlotContainer slot : slots) {
            byte[] slotData = getSlotData(slot);
            if (slotId++ == 0) {
                writeMldMetadata(slotData, data);
            }
            output.write(slotData);
        }
        return new ByteArrayInputStream(output.toByteArray());
    }
}
