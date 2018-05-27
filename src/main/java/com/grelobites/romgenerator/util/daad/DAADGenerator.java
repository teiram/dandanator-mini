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
                                                      List<RelocatableItem> items) {
        List<SlotContainer> slots = new ArrayList<>();
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

        int slotId = 1;
        for (SlotContainer slot: slots) {
            int currentOffset = 0;
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
        slots.add(0, slot0);
        return slots;
    }

    private void writeSlot0Parts(byte[] slotData, DAADData data) throws IOException {
        byte[] metadata = MLDMetadata.newBuilder()
                .withDAADBinaries(data.getBinaryParts())
                .withDAADResources(data.getDAADResources())
                .withDAADScreen(data.getScreen())
                .withAllocatedSectors(DAADConstants.MLD_ALLOCATED_SECTORS)
                .build().toByteArray();
        LOGGER.debug("Writing metadata to slot of size {} with size {} at offset {}",
                slotData.length,
                metadata.length, DAADConstants.METADATA_OFFSET);
        System.arraycopy(metadata, 0, slotData,
                DAADConstants.METADATA_OFFSET, metadata.length);

        byte[] loader = DAADConstants.getDAADLoader();

        System.arraycopy(loader, 0, slotData, 0, loader.length);
        if (data.getScreen() != null) {
            System.arraycopy(data.getScreen().getData(), 0,
                    slotData,
                    data.getScreen().getSlotOffset(),
                    data.getScreen().getData().length);
        }
    }

    private void fillSlotData(SlotContainer slot, byte[] slotData) {
        for (RelocatableItem item : slot.items) {
            LOGGER.debug("Writing Item of size {} at {}",
                    item.getSize(), item.getSlotOffset());
            System.arraycopy(item.getData(), 0, slotData,
                    item.getSlotOffset(), item.getSize());
        }
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
        List<RelocatableItem> items = new ArrayList<>();
        for (int i = 0; i < DAADConstants.BINARY_PARTS; i++) {
            DAADBinary part = data.getBinaryPart(i);
            part.setData(Util.compress(part.getData()));
            items.add(part);
        }
        items.addAll(data.getDAADResources());

        List<SlotContainer> slots = performBinPack(slot0, items);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int slotId = 0;
        for (SlotContainer slot : slots) {
            byte[] slotData = new byte[Constants.SLOT_SIZE];
            if (slotId++ == 0) {
                writeSlot0Parts(slotData, data);
            } else {
                fillSlotData(slot, slotData);
            }
            output.write(slotData);
        }
        return new ByteArrayInputStream(output.toByteArray());
    }
}
