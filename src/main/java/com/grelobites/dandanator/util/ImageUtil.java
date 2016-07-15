package com.grelobites.dandanator.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.grelobites.dandanator.Constants;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public class ImageUtil {
				
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
}
