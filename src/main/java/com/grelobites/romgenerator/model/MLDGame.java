package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.ImageUtil;
import com.grelobites.romgenerator.util.compress.zx7.Zx7InputStream;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;

public class MLDGame extends BaseGame implements RamGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotGame.class);

    private HardwareMode hardwareMode;
    private Image screenshot;
    private MLDInfo mldInfo;

    public MLDGame(MLDInfo mldInfo, List<byte[]> data) {
        super(mldInfo.getGameType(), data);
        this.mldInfo = mldInfo;
        hardwareMode = mldInfo.getHardwareMode();
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    public MLDInfo initializeMldInfo() {
        if (mldInfo == null) {
            Optional<MLDInfo> mldInfoOpt = MLDInfo.fromGameByteArray(data);
            if (!mldInfoOpt.isPresent()) {
                throw new IllegalArgumentException("Unable to extract MLD data from file");
            } else {
                mldInfo = mldInfoOpt.get();
            }
        }
        return mldInfo;
    }

    public MLDInfo getMLDInfo() {
        return mldInfo;
    }

    public Image getScreenshot() {
        if (screenshot == null) {
            try {
                screenshot = ImageUtil
                        .scrLoader(ImageUtil.newScreenshot(),
                                new Zx7InputStream(
                                        new ByteArrayInputStream(
                                                data.get(0),
                                                mldInfo.getCompressedScreenOffset(),
                                                mldInfo.getCompressedScreenSize()
                                        )));
            } catch (Exception e) {
                LOGGER.error("Loading screenshot", e);
            }
        }
        return screenshot;
    }

    @Override
    public HardwareMode getHardwareMode() {
        return hardwareMode;
    }

    @Override
    public void setHardwareMode(HardwareMode hardwareMode) {
        this.hardwareMode = hardwareMode;
    }

    @Override
    public void setScreenshot(Image screenshot) {
        this.screenshot = screenshot;
    }

    public MLDInfo getMldInfo() {
        return mldInfo;
    }

    public void setMldInfo(MLDInfo mldInfo) {
        this.mldInfo = mldInfo;
    }

    @Override
    public int getSize() {
        if (size == null) {
            size = super.getSize();
            size += mldInfo.getRequiredSectors() * Constants.SECTOR_SIZE;
        }
        return size;
    }

    @Override
    public String toString() {
        return "MLDGame{" +
                " name=" + name +
                ", hardwareMode=" + hardwareMode +
                ", mldInfo=" + mldInfo +
                '}';
    }

}
