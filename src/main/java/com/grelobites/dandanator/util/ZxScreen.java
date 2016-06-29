package com.grelobites.dandanator.util;

import java.io.IOException;

import com.grelobites.dandanator.Constants;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZxScreen extends WritableImage {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZxScreen.class);

	private static final String DEFAULT_CHARSETPATH = "/charset.rom";
	
	private String charSetPath;
	
	private byte[] charSet;

    private int lines;
    private int columns;

	private ZxColor pen = ZxColor.BLACK;
	private ZxColor ink = ZxColor.WHITE;
	
	public ZxScreen() {
        this(Constants.SPECTRUM_SCREEN_WIDTH,
                Constants.SPECTRUM_SCREEN_HEIGHT);
	}

	public ZxScreen(int width, int height) {
        super(width, height);
        this.lines = height >> 3;
        this.columns = width >> 3;
    }

	public String getCharSetPath() {
		if (charSetPath == null) {
			charSetPath = DEFAULT_CHARSETPATH;
		}
		return charSetPath;
	}
	
	public byte[] getCharSet() {
		if (charSet == null) {
			try {
				charSet = ImageUtil.streamToByteArray(ZxScreen.class
						.getResourceAsStream(getCharSetPath()));
			} catch (IOException e) {
				throw new IllegalStateException("Unable to load charset", e);
			}
		}
		return charSet;
	}

	public void setCharSetPath(String charSetPath) {
		this.charSetPath = charSetPath;
	}

	public void setCharSet(byte[] charSet) {
		this.charSet = charSet;
	}
	
	public ZxColor getPen() {
		return pen;
	}

	public void setPen(ZxColor pen) {
		this.pen = pen;
	}

	public ZxColor getInk() {
		return ink;
	}

	public void setInk(ZxColor ink) {
		this.ink = ink;
	}

	private byte charRasterLine(char c, int rasterLine) {
		int index = (c - 32) * 8 + rasterLine;
		return getCharSet()[index];
	}
	
	public void deleteChar(int line, int column) {
        if (line < lines && column < columns) {
            int xpos = column * 8;
            int ypos = line * 8;
            PixelWriter writer = getPixelWriter();
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    writer.setArgb(xpos + x, ypos + y, ink.argb());
                }
            }
        } else {
            LOGGER.debug("Out of bounds access to screen");
        }
	}
	
	public void printChar(char c, int line, int column) {
        if (line < lines && column < columns) {
            int xpos = column * 8;
            int ypos = line * 8;
            PixelWriter writer = getPixelWriter();
            for (int y = 0; y < 8; y++) {
                int mask = 0x80;
                byte charRasterLine = charRasterLine(c, y);
                for (int x = 0; x < 8; x++) {
                    int color = (charRasterLine & mask) != 0 ?
                            pen.argb() : ink.argb();
                    writer.setArgb(xpos + x, ypos + y, color);
                    mask >>= 1;
                }
            }
        } else {
            LOGGER.debug("Out of bounds access to screen");
        }
	}

	public void printLine(String text, int line, int column) {
		for (int i = 0; i < text.length(); i++) {
			printChar(text.charAt(i), line, column++);
		}
	}
	
	public void deleteLine(int line) {
		for (int column = 0; column < columns; column++) {
			deleteChar(line, column);
		}
	}

    public void clearScreen() {
        for (int line = 0; line < lines; line++) {
            deleteLine(line);
        }
    }
}
