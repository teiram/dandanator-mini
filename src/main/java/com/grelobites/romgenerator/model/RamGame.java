package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.compress.Compressor;
import com.grelobites.romgenerator.util.compress.CompressorType;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class RamGame extends BaseGame implements Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(RamGame.class);

	private BooleanProperty rom;
	private BooleanProperty holdScreen;
	private BooleanProperty compressed;
	private Image screenshot;
    private SNAHeader snaHeader;
	private TrainerList trainerList;
    private CompressorType lastCompressor;
    private List<byte[]> compressedData;
    private Integer compressedSize;

    public RamGame(GameType gameType, List<byte[]> data) {
        super(gameType, data);
		rom = new SimpleBooleanProperty();
		holdScreen = new SimpleBooleanProperty();
        compressed = new SimpleBooleanProperty(true);
	}

    public SNAHeader getSnaHeader() {
        return snaHeader;
    }

    public void setSnaHeader(SNAHeader snaHeader) {
        this.snaHeader = snaHeader;
    }

    public boolean getRom() {
		return rom.get();
	}
	
	public void setRom(boolean rom) {
		this.rom.set(rom);
	}
	
	public BooleanProperty romProperty() {
		return this.rom;
	}

	public boolean getCompressed() {
		return compressed.get();
	}

	public void setCompressed(boolean compressed) {
		this.compressed.set(compressed);
	}

	public BooleanProperty compressedProperty() {
		return compressed;
	}

	public boolean getHoldScreen() {
		return holdScreen.get();
	}
	
	public void setHoldScreen(boolean holdScreen) {
		this.holdScreen.set(holdScreen);
	}
	
	public BooleanProperty holdScreenProperty() {
		return holdScreen;
	}
	
	public Image getScreenshot() {
		if (screenshot == null) {
			try {
				screenshot = ImageUtil
						.scrLoader(ImageUtil.newScreenshot(), 
								new ByteArrayInputStream(getSlot(0),
										0,
										Constants.SPECTRUM_FULLSCREEN_SIZE));
			} catch (Exception e) {
				LOGGER.error("Loading screenshot", e);
			}
		}
		return screenshot;
	}
	
	public void setScreenshot(Image screenshot) {
		this.screenshot = screenshot;
	}

	public TrainerList getTrainerList() {
        if (trainerList == null) {
            trainerList = new TrainerList(this);
        }
        return trainerList;
    }

	public void setTrainerList(TrainerList trainerList) {
		this.trainerList = trainerList;
	}

	public void addTrainer(String pokeName) {
		getTrainerList().addTrainerNode(pokeName);
	}

    public boolean hasPokes() {
        return trainerList != null && trainerList.getChildren().size() > 0;
    }

	@Override
	public boolean isCompressible() {
		return true;
	}

	@Override
    public Observable[] getObservable() {
	    return new Observable[]{name, rom, holdScreen, compressed};
    }

	public List<byte[]> getCompressedData(Compressor compressor) throws IOException {
	    if (compressedData == null || lastCompressor != compressor.getCompressorType()) {
            compressedData = new ArrayList<>();
            for (int i = 0; i < getSlotCount(); i++) {
                LOGGER.debug("Compressing game slot " + i);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                OutputStream compressingStream = compressor.getCompressingOutputStream(os);
                compressingStream.write(getSlot(i));
                compressingStream.flush();
                compressedData.add(os.toByteArray());
            }
            lastCompressor = compressor.getCompressorType();
        }
        return compressedData;
    }

    public int getCompressedSize() throws IOException {
        return getCompressedSize(null);
    }

    public int getCompressedSize(Compressor compressor) throws IOException {
        if (compressedSize == null) {
            if (compressor != null) {
                int size = 0;
                for (byte[] compressedSlot : getCompressedData(compressor)) {
                    size += compressedSlot.length;
                }
                compressedSize = size;
            } else {
                throw new IllegalStateException("Compressed size not calculated yet");
            }
        }
        return compressedSize;
    }
}
