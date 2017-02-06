package com.grelobites.romgenerator.model;

import java.util.ArrayList;
import java.util.List;

public class ChangeData {
    private final RomId version;
    private final List<ChangeValue> changeValues;

    public ChangeData(RomId version) {
        this.version = version;
        this.changeValues = new ArrayList<>();
    }

    public RomId getVersion() {
        return version;
    }

    public List<ChangeValue> getChangeValues() {
        return changeValues;
    }
}
