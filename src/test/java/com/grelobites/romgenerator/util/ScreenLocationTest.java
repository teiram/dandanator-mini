package com.grelobites.romgenerator.util;

import com.grelobites.romgenerator.Constants;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

public class ScreenLocationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenLocationTest.class);


    private boolean equalImages(Image first, Image second) {
        PixelReader r1 = first.getPixelReader();
        PixelReader r2 = second.getPixelReader();
        for (int x = 0; x < Constants.SPECTRUM_SCREEN_WIDTH; x++) {
            for (int y = 0; y < Constants.SPECTRUM_SCREEN_HEIGHT; y++) {
                if (r1.getArgb(x, y) != r2.getArgb(x, y)) {
                    LOGGER.error("Difference in " + x + ", " + y + "(" + Integer.toHexString(r1.getArgb(x, y))
                                    + ", " + Integer.toHexString(r2.getArgb(x, y)) + ")");
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testHiddenScreenLocation() throws Exception {
        final int zoneLength = 6;
        byte[] screenData = Util.fromInputStream(ScreenLocationTest.class.getResourceAsStream("/image/pingpong.scr"),
                Constants.SPECTRUM_FULLSCREEN_SIZE);
        WritableImage originalImage = ImageUtil
                .scrLoader(ImageUtil.newScreenshot(),
                        new ByteArrayInputStream(screenData,
                                0,
                                Constants.SPECTRUM_FULLSCREEN_SIZE));

        Optional<Integer> offset = ImageUtil.getHiddenDisplayOffset(screenData, zoneLength);
        if (offset.isPresent()) {
            LOGGER.debug("Calculated offset is " + offset);
            int i = 0;
            int base =  offset.get();
            screenData[base] = Z80Opcode.PUSH_HL;
            screenData[base + 1] = Z80Opcode.POP_HL;
            screenData[base + 2] = Z80Opcode.PUSH_HL;
            screenData[base + 3] = Z80Opcode.POP_HL;
            screenData[base + 4] = Z80Opcode.DI;
            screenData[base + 5] = Z80Opcode.RET;

        } else {
            fail("No proper zone found");
        }
        assertTrue(equalImages(originalImage, ImageUtil.scrLoader(ImageUtil.newScreenshot(),
                new ByteArrayInputStream(screenData,
                        0,
                        Constants.SPECTRUM_FULLSCREEN_SIZE))));

    }

}
