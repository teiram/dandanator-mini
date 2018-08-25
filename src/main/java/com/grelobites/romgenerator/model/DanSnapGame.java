package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class DanSnapGame extends MLDGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(DanSnapGame.class);
    private static final int MLDINFO_SLOT = 0;
    private static final int GAMETYPE_OFFSET = 16363;
    private static final byte SIGNATURE_48K = (byte) 0xc3;
    private static final byte SIGNATURE_128K = (byte) 0xc8;

    private BooleanProperty reservationSize48K;


    public DanSnapGame(MLDInfo mldInfo, List<byte[]> data) {
        super(mldInfo, data);
        LOGGER.debug("New DanSnapGame");
        reservationSize48K = new SimpleBooleanProperty(true);
        setSize(calculateSize());
        reservationSize48K.addListener((observable, oldValue, newValue) -> {
            setSize(calculateSize());
            LOGGER.debug("Size recalculated as {}", getSize());
            getSlot(MLDINFO_SLOT)[GAMETYPE_OFFSET] = newValue ? SIGNATURE_48K : SIGNATURE_128K;
        });
    }

    public int calculateSize() {
        return (getSlotCount() + getReservedSlots()) * Constants.SLOT_SIZE;
    }

    public boolean isReservationSize48K() {
        return reservationSize48K.get();
    }

    public BooleanProperty reservationSize48KProperty() {
        return reservationSize48K;
    }

    public void setReservationSize48K(boolean reservationSize48K) {
        this.reservationSize48K.set(reservationSize48K);
    }

    public int getReservedSlots() {
        return isReservationSize48K() ? 3 : 8;
    }

    @Override
    public Observable[] getObservable() {
        return new Observable[]{name, sizeProperty()};
    }

}
