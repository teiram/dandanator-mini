package com.grelobites.romgenerator.util.gameloader.loaders.tap;

public class Spectrum128KMemory extends BankedMemory {
    private static final int BANK_SIZE = 0x4000;
    private static final int RAM_1STBANK = 2;

    private static int[] calculateBankMappings(int last7ffd) {
        return new int[] {
                (last7ffd & 0x10) >> 5,
                RAM_1STBANK + 5,
                RAM_1STBANK + 2,
                RAM_1STBANK + (last7ffd & 0x7)
        };
    }

    public Spectrum128KMemory(int last7ffd) {
        super(calculateBankMappings(last7ffd), BANK_SIZE, RAM_1STBANK, 8);
    }

    public void setLast7ffd(int last7ffd) {
        setBankMappings(calculateBankMappings(last7ffd));
    }
}
