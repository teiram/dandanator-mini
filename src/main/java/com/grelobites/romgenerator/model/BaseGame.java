package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BaseGame {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseGame.class);


	protected StringProperty name;
	protected List<byte[]> data;
    protected GameType gameType;
    protected Integer size;

    private static boolean checkZeroedSlot(byte[] slot) {
        for (byte aSlot : slot) {
            if (aSlot != Constants.B_00) {
                return false;
            }
        }
        LOGGER.debug("Slot detected as zeroed");
        return true;
    }

    private static List<byte[]> reduceZeroedSlots(List<byte[]> data) {
        List<byte[]> reduced = new ArrayList<>();
        for (byte[] slot: data) {
            reduced.add(checkZeroedSlot(slot) ? null : slot);
        }
        return reduced;
    }

	public BaseGame(GameType gameType, List<byte[]> data) {
	    this.gameType = gameType;
        this.data = reduceZeroedSlots(data);
		name = new SimpleStringProperty();
	}

	public String getName() {
		return name.get();
	}

	public void setName(String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return this.name;
	}

    @Override
    public String toString() {
        return "BaseGame{" +
                "name=" + name.get() +
                ", gameType=" + gameType +
                '}';
    }

    public boolean isSlotZeroed(int slot) {
        return data.get(slot) == null;
    }

    public GameType getType() {
	    return gameType;
    }

    public int getSlotCount() {
        return data.size();
    }

    public byte[] getSlot(int slot) {
        return isSlotZeroed(slot) ? Constants.ZEROED_SLOT : data.get(slot);
    }

    public int getSize() {
        if (size == null) {
            int gameSize = 0;
            for (byte[] slot: data) {
                gameSize += slot == null ? 0 : slot.length;
            }
            size = gameSize;
        }
        return size;
    }

    public Observable[] getObservable() {
        return new Observable[] {name};
    }

}
