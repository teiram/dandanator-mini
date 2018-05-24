package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.pack.Container;
import com.grelobites.romgenerator.util.pack.PackAlgorithms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DAADGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(DAADGenerator.class);

    private static class SlotContainer implements Container<RelocatableItem> {
        public int remaining = Constants.SLOT_SIZE;
        public List<RelocatableItem> items = new ArrayList<>();

        @Override
        public int getCapacity() {
            return remaining;
        }

        @Override
        public void addItem(RelocatableItem item) {
            if (item.getSize() > remaining) {
                throw new IllegalStateException("No room to add required item");
            }
            remaining -= item.getSize();
            items.add(item);
        }
    }

    private DAADData data;

    public DAADGenerator(DAADData data) {
        this.data = data;
    }

    private static List<SlotContainer> performBinPack(SlotContainer slot0,
                                                      List<RelocatableItem> items,
                                                      int slot0Offset) {
        List<SlotContainer> slots = new ArrayList<>();
        slots.add(slot0);
        for (int i = 0; i < 29; i++) {
            slots.add(new SlotContainer());
        }
        PackAlgorithms.binBFDPack(slots, items);
        //Remove unused slots
        for (int i = slots.size() - 1; i >= 0; i--) {
            if (slots.get(i).items.size() == 0) {
                slots.remove(i);
            }
        }
        LOGGER.debug("Slot count after pruning {}", slots.size());

        int slotId = 0;
        for (SlotContainer slot: slots) {
            int currentOffset = slotId == 0 ? slot0Offset : 0;
            LOGGER.debug("Allocated {} resources in slot {}", slot.items.size(), slotId);
            for (RelocatableItem item : slot.items) {
                LOGGER.debug("Adding item of size {} at offset {} to slot {}",
                        item.getSize(), currentOffset, slotId);
                item.setSlot(slotId);
                item.setSlotOffset(currentOffset);
                currentOffset += item.getSize();
            }
            slotId++;
        }

        return slots;
    }

    private void writeMldMetadata(byte[] slotData, DAADData data) {
        byte[] metadata = MLDMetadata.newBuilder()
                .withDAADBinaries(data.getBinaryParts())
                .withDAADResources(data.getDAADResources())
                .withDAADScreen(data.getScreen())
                .build().toByteArray();
        LOGGER.debug("Writing metadata with size {} at offset {}",
                metadata.length, DAADConstants.METADATA_OFFSET);
        System.arraycopy(metadata, 0, slotData,
                DAADConstants.METADATA_OFFSET, metadata.length);
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
        int slot0Offset = Constants.SLOT_SIZE - slot0.remaining - DAADConstants.METADATA_SIZE;
        LOGGER.debug("Remaining space in slot0: {}, available offset: {}",
                slot0.remaining, slot0Offset);
        List<RelocatableItem> items = new ArrayList<>();
        for (int i = 0; i < DAADConstants.BINARY_PARTS; i++) {
            DAADBinary part = data.getBinaryPart(i);
            part.setData(Util.compress(part.getData()));
            items.add(part);
        }
        items.addAll(data.getDAADResources());
        //Add a number of slots to accommodate all the data

        List<SlotContainer> slots = performBinPack(slot0, items, slot0Offset);

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
