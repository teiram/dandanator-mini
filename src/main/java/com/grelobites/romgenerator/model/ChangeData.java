package com.grelobites.romgenerator.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeData {
    private final RomId version;
    private final Map<Integer, List<ChangeValue>> changeValuesMap;

    public ChangeData(RomId version) {
        this.version = version;
        this.changeValuesMap = new HashMap<>();
    }

    public RomId getVersion() {
        return version;
    }

    public Map<Integer, List<ChangeValue>> getChangeValuesMap() {
        return changeValuesMap;
    }

    public void addChangeValue(int bank, ChangeValue changeValue) {
        List<ChangeValue> changeValuesList = changeValuesMap.get(bank);
        if (changeValuesList == null) {
            changeValuesList = new ArrayList<>();
            changeValuesMap.put(bank, changeValuesList);
        }
        changeValuesList.add(changeValue);
    }

    @Override
    public String toString() {
        return "ChangeData{" +
                "version=" + version +
                ", changeValuesMap=" + changeValuesMap +
                '}';
    }
}
