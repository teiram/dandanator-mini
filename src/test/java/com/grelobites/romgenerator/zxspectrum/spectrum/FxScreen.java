package com.grelobites.romgenerator.zxspectrum.spectrum;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ZxColor;
import com.grelobites.romgenerator.zxspectrum.Peripheral;
import com.grelobites.romgenerator.zxspectrum.PollingTarget;
import com.grelobites.romgenerator.zxspectrum.Z80VirtualMachine;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FxScreen implements Peripheral, PollingTarget {
    private static final Logger LOGGER = LoggerFactory.getLogger(FxScreen.class);

    private Z80VirtualMachine cpu;
    private boolean flash = false;
    public static final byte ATTRIBUTE_FLASH = (byte) 0x80;
    public static final byte ATTRIBUTE_BRIGHT = (byte) 0x40;

    private byte[] screenMemory = null;
    private int screenOffset;

    private ImageView frame;
    private int currentFrame;
    private int frameCount = 2;
    private final int changed[] = new int[SpectrumConstants.SCREEN_MEMORY_SIZE * 4];
    private int changedWrite = 0;
    private boolean screenChanged = false;

    private static ImageView initializeImageView() {
        ImageView imageView = new ImageView();
        imageView.setImage(new WritableImage(Constants.SPECTRUM_SCREEN_WIDTH,
                Constants.SPECTRUM_SCREEN_HEIGHT));
        return imageView;
    }

    public FxScreen() {
        frame = initializeImageView();
    }

    @Override
    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;
        cpu.addPollingTarget(320, this);
    }

    @Override
    public void unbind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = null;
    }

    @Override
    public void onCpuReset(Z80VirtualMachine cpu) throws Exception {
        flash = false;
    }

    @Override
    public void poll(Z80VirtualMachine cpu) {
        flash = !flash;
        for (int i = 0; i <= SpectrumConstants.SCREEN_ATTRIBUTE_SIZE; i++) {
            if ((getMemory(i + SpectrumConstants.SCREEN_MEMORY_SIZE) & ATTRIBUTE_FLASH) != 0) {
                repaintAttribute(i);
            }
        }
    }

    public void setScreenMemory(byte memory[], int offset) {
        screenMemory = memory;
        screenOffset = offset;
        for (int i = 0; i < SpectrumConstants.SCREEN_MEMORY_SIZE; i++) {
            repaintScreen(i);
        }
    }

    private int getMemory(int offset) {
        if (screenMemory == null) {
            return 0;
        }
        return screenMemory[screenOffset + offset] & 0xff;
    }

    void repaintAttribute(int addr) {
        int scrAddr = ((addr & 0x300) << 3) | (addr & 0xff);

        for (int i = 0; i < 8; i++) {
            repaintScreen(scrAddr);
            // Next address in memory
            scrAddr += 256;
        }
    }

    void repaintScreen(int add) {
        //LOGGER.debug("repaintScreen at " + add);
        synchronized (changed) {
            if (changedWrite < changed.length) {
                screenChanged = true;
                changed[changedWrite++] = add;
            }
        }
    }

    private void drawByte(int address, PixelWriter writer) {
        int pixel = getMemory(address);
        int x = ((address & 0x1f) << 3);
        int y = (( (address & 0x00e0)) >> 2) +
                (( (address & 0x0700)) >> 8) +
                (( (address & 0x1800)) >> 5);
        int attributeByte = getMemory(SpectrumConstants.SCREEN_MEMORY_SIZE + (address & 0x1f) + ((y >> 3) * 32)) & 0xff;
        int ink = ZxColor.byIndex((attributeByte & 7) + ((attributeByte & 64) >> 3));
        int paper = ZxColor.byIndex(((attributeByte & 56) >> 3) + ((attributeByte & 64) >> 3));
        int attributeMask = 0x80;
        for (int i = 0; i < 8; i++) {
            boolean foreground = (pixel & attributeMask) != 0;
            writer.setArgb(x + i, y, foreground ? ink : paper);
            attributeMask >>= 1;
        }
    }

    public ImageView nextFrame() {
        if (!screenChanged) {
            return null;
        }
        synchronized (changed) {
            PixelWriter writer = ((WritableImage)frame.getImage()).getPixelWriter();
            for (int i = 0; i < changedWrite; i++) {
                drawByte(changed[i], writer);
            }
            screenChanged = false;
            changedWrite = 0;
        }
        return frame;
    }

}
