package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.RamGameCompressor;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RamGame extends BaseGame implements Game {
	private static final Logger LOGGER = LoggerFactory.getLogger(RamGame.class);

	private BooleanProperty rom;
	private BooleanProperty holdScreen;
	private BooleanProperty compressed;
	private BooleanProperty force48kMode;
	private Image screenshot;
    private GameHeader gameHeader;
	private TrainerList trainerList;
    private List<byte[]> compressedData;
    private Integer compressedSize;
    private HardwareMode hardwareMode;

    private static final int[] SLOT_MAP = new int[] {2, 3, 1, 4, 5, 0, 6, 7};

    public RamGame(GameType gameType, List<byte[]> data) {
        super(gameType, data);
		rom = new SimpleBooleanProperty();
		holdScreen = new SimpleBooleanProperty();
        compressed = new SimpleBooleanProperty(true);
        force48kMode = new SimpleBooleanProperty(false);
	}

    public boolean getForce48kMode() {
        return force48kMode.get();
    }

    public BooleanProperty force48kModeProperty() {
        return force48kMode;
    }

    public void setForce48kMode(boolean force48kMode) {
        this.force48kMode.set(force48kMode);
    }

    public GameHeader getGameHeader() {
        return gameHeader;
    }

    public void setGameHeader(GameHeader gameHeader) {
        this.gameHeader = gameHeader;
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

	public int getScreenSlot() {
	    if (gameType == GameType.RAM128) {
            return (gameHeader.getPort7ffdValue(0) & 0x08) != 0 ? 7 : 0;
        } else {
            return 0;
        }
    }

    public void setCompressedData(List<byte[]> compressedData) {
        this.compressedData = compressedData;
    }

    public Image getScreenshot() {
		if (screenshot == null) {
			try {
				screenshot = ImageUtil
						.scrLoader(ImageUtil.newScreenshot(), 
								new ByteArrayInputStream(getSlot(getScreenSlot()),
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
        trainerList.setOwner(this);
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

	public List<byte[]> getCompressedData(RamGameCompressor compressor) throws IOException {
	    if (compressedData == null) {
            compressedData = new ArrayList<>();
            for (int i = 0; i < getSlotCount(); i++) {
                if (!isSlotZeroed(i)) {
                    compressedData.add(compressor.compressSlot(i, getSlot(i)));
                } else {
                    compressedData.add(null);
                }
            }
        }
        return compressedData;
    }

    public int getCompressedSize() throws IOException {
        return getCompressedSize(null);
    }

    public int getCompressedSize(RamGameCompressor compressor) throws IOException {
        if (compressedSize == null) {
            if (compressor != null) {
                int size = 0;
                for (byte[] compressedSlot : getCompressedData(compressor)) {
                    size += compressedSlot != null ? compressedSlot.length : 0;
                }
                compressedSize = size;
            } else {
                throw new IllegalStateException("Compressed size not calculated yet");
            }
        }
        return compressedSize;
    }

    public int getSlotForMappedRam(int offset) {
        int bankPos = offset / Constants.SLOT_SIZE;
        switch (bankPos) {
            case 0:
                throw new IllegalArgumentException("Requested offset in low ROM");
            case 1:
                return 0;
            case 2:
                return 1;
            case 3:
                return (gameType == GameType.RAM128) ?
                    SLOT_MAP[gameHeader.getPort7ffdValue(0) & 0x03] : 2;
            default:
                throw new IllegalArgumentException("Requested offset out of mapped RAM");
        }
    }

    public int getSlotForBank(int bankPos) {
        return SLOT_MAP[bankPos];
    }

    public HardwareMode getHardwareMode() {
        return hardwareMode;
    }

    public void setHardwareMode(HardwareMode hardwareMode) {
        LOGGER.debug("Setting hardware mode " + hardwareMode);
        if (hardwareMode != HardwareMode.HW_UNSUPPORTED) {
            this.hardwareMode = hardwareMode;
        } else {
            throw new IllegalArgumentException("Unsupported Hardware Mode");
        }
    }
}
