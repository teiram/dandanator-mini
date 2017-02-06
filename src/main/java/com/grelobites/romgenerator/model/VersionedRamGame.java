package com.grelobites.romgenerator.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersionedRamGame extends RamGame implements Game {

    private Map<RomId, ChangeData> changeDataMap;
    private RomId version;

    public VersionedRamGame(GameType gameType, List<byte[]> data) {
        super(gameType, data);
    }

    public Map<RomId, ChangeData> getChangeDataMap() {
        if (changeDataMap == null) {
            changeDataMap = new HashMap<>();
        }
        return changeDataMap;
    }

    public void setVersion(RomId version) {
        this.version = version;
    }

    public RomId getVersion() {
        return version;
    }

}
