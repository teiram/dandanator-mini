package com.grelobites.romgenerator.model;

import javafx.beans.Observable;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class VirtualGame implements Game {
    protected GameType gameType;
    protected StringProperty name;
    protected IntegerProperty version;

   public VirtualGame(GameType gameType, String name) {
        this.gameType = gameType;
        this.name = new SimpleStringProperty(name);
        this.version = new SimpleIntegerProperty();
    }

    @Override
    public GameType getType() {
        return gameType;
    }

    @Override
    public String getName() {
        return name.get();
    }

    @Override
    public void setName(String name) {
        this.name.set(name);
    }

    public int getVersion() {
        return version.get();
    }

    public IntegerProperty versionProperty() {
        return version;
    }

    public void setVersion(int version) {
        this.version.set(version);
    }

    public void setNextVersion() {
        this.version.set(this.version.get() + 1);
    }

    @Override
    public StringProperty nameProperty() {
        return name;
    }

    @Override
    public boolean isCompressible() {
        return false;
    }

    @Override
    public byte[] getSlot(int slot) {
        return new byte[0];
    }

    @Override
    public int getSlotCount() {
        return 0;
    }

    @Override
    public Observable[] getObservable() {
        return new Observable[]{ name, version };
    }

    @Override
    public boolean isSlotZeroed(int slot) {
        return false;
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public String toString() {
        return "VirtualGame{" +
                "gameType=" + gameType +
                ", name=" + name.get() +
                ", version=" + version.get() +
                '}';
    }

}
