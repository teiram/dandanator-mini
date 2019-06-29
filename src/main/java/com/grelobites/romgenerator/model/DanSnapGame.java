package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DanSnapGame extends MLDGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanSnapGame.class);
    private static final int MLDINFO_SLOT = 0;
    private static final int GAMETYPE_OFFSET = 16363;
    private static final byte SIGNATURE_16K = (byte) 0xc1;
    private static final byte SIGNATURE_48K = (byte) 0xc3;
    private static final byte SIGNATURE_128K = (byte) 0xc8;
    private static final int SLOTS_16K = 1;
    private static final int SLOTS_48K = 3;
    private static final int SLOTS_128K = 8;

    private IntegerProperty reservedSlots;

    public DanSnapGame(MLDInfo mldInfo, List<byte[]> data) {
        super(mldInfo, data);
        LOGGER.debug("New DanSnapGame");
        reservedSlots = new SimpleIntegerProperty();
        switch (mldInfo.getMldType()) {
            case SIGNATURE_16K:
                reservedSlots.set(1);
                break;
            case SIGNATURE_48K:
                reservedSlots.set(3);
                break;
            case SIGNATURE_128K:
                reservedSlots.set(8);
                break;
        }
        setSize(calculateSize());
        reservedSlots.addListener((observable, oldValue, newValue) -> {
            switch (newValue.intValue()) {
                case SLOTS_16K:
                    getSlot(MLDINFO_SLOT)[GAMETYPE_OFFSET] = SIGNATURE_16K;
                    getMldInfo().setMldType(SIGNATURE_16K);
                    setHardwareMode(HardwareMode.HW_16K);
                    gameType = GameType.DAN_SNAP16;
                    break;
                case SLOTS_48K:
                    getSlot(MLDINFO_SLOT)[GAMETYPE_OFFSET] = SIGNATURE_48K;
                    getMldInfo().setMldType(SIGNATURE_48K);
                    setHardwareMode(HardwareMode.HW_48K);
                    gameType = GameType.DAN_SNAP;
                    break;
                case SLOTS_128K:
                    getSlot(MLDINFO_SLOT)[GAMETYPE_OFFSET] = SIGNATURE_128K;
                    getMldInfo().setMldType(SIGNATURE_128K);
                    setHardwareMode(HardwareMode.HW_128K);
                    gameType = GameType.DAN_SNAP128;
                    break;
                default:
                    LOGGER.warn("Trying to setup invalid MLD size", newValue);
            }
            setSize(calculateSize());
            LOGGER.debug("Size recalculated as {}", getSize());
        });
    }

    public int getReservedSlots() {
        return reservedSlots.get();
    }

    public IntegerProperty reservedSlotsProperty() {
        return reservedSlots;
    }

    public void setReservedSlots(int reservedSlots) {
        this.reservedSlots.set(reservedSlots);
    }

    public int calculateSize() {
        return (getSlotCount() + reservedSlots.getValue()) * Constants.SLOT_SIZE;
    }

    @Override
    public Observable[] getObservable() {
        return new Observable[]{name, sizeProperty()};
    }

}
