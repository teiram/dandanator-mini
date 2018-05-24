package com.grelobites.romgenerator.util.daad;

import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.tap.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DAADData {
    private static final Logger LOGGER = LoggerFactory.getLogger(DAADData.class);
    private List<DAADResource> daadResources = new ArrayList<>();
    private DAADBinary[] binaryParts;
    private DAADScreen screen;

    private static Pattern zxsNamePattern = Pattern.compile("\\d{3}");

    private static int getZxsIndexFromName(String entryName) {
        int value = -1;
        String name = Util.stripFileExtension(entryName);
        if (zxsNamePattern.matcher(name).matches()) {
            value = Integer.valueOf(name);
        } else {
            LOGGER.warn("Entry name {} doesn't match expected ZXS name", name);
        }
        return value < 256 ? value : -1;
    }

    public static DAADData fromZipStream(ZipInputStream zipInputStream) throws IOException {
        DAADData data = new DAADData();
        ZipEntry entry;
        boolean tapEntryFound = false;
        boolean screenEntryFound = false;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            LOGGER.debug("Got zip entry {} with size {}/{}",
                    entry.getName(), entry.getCompressedSize(), entry.getSize());
            byte[] entryData = Util.fromInputStream(zipInputStream);
            Optional<String> fileExtension = Util.getFileExtension(entry.getName());
            if ("TAP".equalsIgnoreCase(fileExtension.orElse(""))) {
                if (!tapEntryFound) {
                    data.setTapItems(entryData);
                    tapEntryFound = true;
                } else {
                    throw new IllegalArgumentException("Multiple TAP entries found");
                }
            } else if ("ZXS".equalsIgnoreCase(fileExtension.orElse(""))) {
                LOGGER.debug("Processing zxs entry {}", entry.getName());
                int zxsIndex = getZxsIndexFromName(entry.getName());
                if (zxsIndex > -1) {
                    data.addZxScreen(zxsIndex, entryData);
                } else {
                    LOGGER.warn("Ignoring ZXS part with name {}", entry.getName());
                }
            } else if ("SCR".equalsIgnoreCase(fileExtension.orElse(""))) {
                if (!screenEntryFound) {
                    data.setScreenData(entryData);
                    screenEntryFound = true;
                } else {
                    throw new IllegalArgumentException("Multiple SCR entries found");
                }
            }
            zipInputStream.closeEntry();
        }
        if (!tapEntryFound) {
            throw new IllegalArgumentException("No TAP found in ZIP stream");
        } else {
            return data;
        }
    }

    private void setTapItems(byte[] tapByteArray) throws IOException {
        TapInputStream tis = new TapInputStream(new ByteArrayInputStream(tapByteArray));
        Optional<TapBlock> tapHeaderBlockOpt;
        int binaryBlockIndex = 0;
        List<DAADBinary> items = new ArrayList<>();
        while ((tapHeaderBlockOpt = tis.next()).isPresent()) {
            LOGGER.debug("Read TAP Header Block {}", tapHeaderBlockOpt.get());
            if (tapHeaderBlockOpt.get().getType() == TapBlockType.CODE) {
                CodeTapBlock headerBlock = (CodeTapBlock) tapHeaderBlockOpt.get();

                Optional<TapBlock> dataBlockOpt = tis.next();
                if (dataBlockOpt.isPresent()) {
                    TapBlock block = dataBlockOpt.get();
                    if (block.getType() == TapBlockType.DATA) {
                        DataTapBlock dataBlock = (DataTapBlock) block;
                        LOGGER.debug("Adding binary part with size {} and startAddress {}",
                                dataBlock.getData().length,
                                String.format("0x%04x", headerBlock.getStartAddress()));
                        items.add(DAADBinary.newBuilder()
                                .withData(dataBlock.getData())
                                .withLoadAddress(headerBlock.getStartAddress())
                                .build());
                    } else {
                        LOGGER.warn("No Data type block type found after Code Header block");
                    }
                } else {
                    LOGGER.warn("No block found after Code Header block");
                }
            }

        }
        if (items.size() == 3) {
            binaryParts = items.toArray(new DAADBinary[0]);
        } else {
            throw new IllegalArgumentException("Invalid number of code blocks found :" +
                items.size());
        }
    }

    private void addZxScreen(int index, byte[] data) {
        LOGGER.debug("Adding ZXScreen entry at index {} with size {}", index, data.length);
        daadResources.add(new DAADResource(index, data));
    }

    public List<DAADResource> getDAADResources() {
        return daadResources;
    }

    public DAADScreen getScreen() {
        return screen;
    }

    public DAADBinary getBinaryPart(int index) {
        return binaryParts[index];
    }

    public DAADBinary[] getBinaryParts() {
        return binaryParts;
    }

    private void setScreenData(byte[] data) {
        LOGGER.debug("Setting screen data with size {}", data.length);
        screen = new DAADScreen(data);
    }

}
