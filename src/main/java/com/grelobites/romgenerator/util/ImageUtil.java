package com.grelobites.romgenerator.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;

import com.grelobites.romgenerator.Constants;

import com.grelobites.romgenerator.model.Game;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ImageUtil.class);

	private static void writeToImage(byte[] imageBytes, byte[] attributeBytes, 
			PixelWriter writer) {
		for (int y = 0; y < Constants.SPECTRUM_SCREEN_HEIGHT; y++) {
			int ypos = (y & Constants.SPECTRUM_SCREEN_HEIGHT) 
					+ ((y & 7) << 3) + ((y & 56) >> 3);
			for (int xchar = 0; xchar < 32; xchar++) {
				int attrpos = ((ypos >> 3) << 5) + xchar;
				byte attrbyte = attributeBytes[attrpos];
				int ink = ZxColor.byIndex((attrbyte & 7) + ((attrbyte & 64) >> 3));
				int paper = ZxColor.byIndex(((attrbyte & 56) >> 3) + ((attrbyte & 64) >> 3));
				int mask = 0x80;
				int imageByte = imageBytes[(y << 5) + xchar];
				for (int i = 0; i < 8; i++) {
					int xpos = (xchar << 3) + i;
					if ((imageByte & mask) == 0) {
						writer.setArgb(xpos, ypos, paper);
					} else {
						writer.setArgb(xpos, ypos,  ink);
					}
					mask >>= 1;
				}
			}	
		}
	}
		
	public static <T extends WritableImage> T scrLoader(T image, InputStream in) 
			throws IOException {
		PixelWriter writer = image.getPixelWriter();
		final byte[] imageBytes = new byte[Constants.SPECTRUM_SCREEN_SIZE];
		final byte[] attributeBytes = new byte[Constants.SPECTRUM_COLORINFO_SIZE];
		in.read(imageBytes, 0, imageBytes.length);
		in.read(attributeBytes, 0, attributeBytes.length);
		writeToImage(imageBytes, attributeBytes, writer);
		return image;
	}
	
	public static byte[] streamToByteArray(InputStream stream) throws IOException {
		byte[] buffer = new byte[1024];
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			int length;
			while ((length = stream.read(buffer)) != -1) {
				os.write(buffer, 0, length);
			}
			os.flush();
			return os.toByteArray();
		}
	}
	
	public static WritableImage newScreenshot() {
		return new WritableImage(Constants.SPECTRUM_SCREEN_WIDTH,
				Constants.SPECTRUM_SCREEN_HEIGHT);
	}

	public static byte[] fillZxImage(byte[] screen, byte[] attributes) {
		byte[] image = new byte[Constants.SPECTRUM_SCREEN_SIZE + Constants.SPECTRUM_COLORINFO_SIZE];
		System.arraycopy(screen, 0, image, 0, screen.length);
		System.arraycopy(attributes, 0, image, Constants.SPECTRUM_SCREEN_SIZE, attributes.length);
		return image;
	}

    public static int attribute2pixelOffset(int attrOffset) {
        int col = attrOffset % 0x20;
        int line = attrOffset >> 5;
        return ((line & 0x18) << 8) | ((line << 5) & 0xe0) | (col & 0x1f);
    }


    public static Optional<Integer> getHiddenDisplayOffset(byte[] displayData, int requiredSize) {
        int attributeBaseOffset = Constants.SPECTRUM_SCREEN_SIZE;
        int zoneSize = 0, i = 0;
        do {
            byte value = displayData[i + attributeBaseOffset];
            //Attribute byte with pen == ink
            if ((value & 0x7) == ((value >> 3) & 0x7)) {
                zoneSize++;
            } else {
                zoneSize = 0;
            }
            i++;
        } while (i < Constants.SPECTRUM_COLORINFO_SIZE && zoneSize < requiredSize);

        if (zoneSize == requiredSize) {
            LOGGER.debug("Found hidden attribute at " + (i - requiredSize));
            return Optional.of(attribute2pixelOffset(i - requiredSize));
        } else {
            LOGGER.debug("Unable to find hidding screen area");
            return Optional.empty();
        }
    }
}
