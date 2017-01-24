package com.grelobites.romgenerator.util.gameloader.loaders.tap.memory;

public class SpectrumPlus2Memory extends BankedMemory {
    private static final int BANK_SIZE = 0x4000;
    public static final int RAM_1STBANK = 4;

    private static final int[][] RAM_BANKS = new int[][] {
            new int[] {0, 1, 2, 3},
            new int[] {4, 5, 6, 7},
            new int[] {4, 5, 6, 3},
            new int[] {4, 7, 6, 3}
    };

    private int last7ffd = 0;
    private int last1ffd = 0;

    private static int[] calculateBankMappings(int last7ffd, int last1ffd) {
        boolean normalBanking = (last1ffd & 0x1) == 0;
        int[] extendedMappingIndexes = RAM_BANKS[(last1ffd >> 1) & 0x3];
        return new int[] {
                normalBanking ? ((last1ffd & 0x4) >> 1) | ((last7ffd & 0x10) >> 4) :
                        RAM_1STBANK + extendedMappingIndexes[0],
                RAM_1STBANK + (normalBanking ? 5 : extendedMappingIndexes[1]),
                RAM_1STBANK + (normalBanking ? 2 : extendedMappingIndexes[2]),
                RAM_1STBANK + (normalBanking ? last7ffd & 0x7 : extendedMappingIndexes[3])
        };
    }

    public SpectrumPlus2Memory(int last7ffd, int last1ffd) {
        super(calculateBankMappings(last7ffd, last1ffd), BANK_SIZE, RAM_1STBANK, 8);
        this.last7ffd = last7ffd;
        this.last1ffd = last1ffd;
    }

    public void setLast7ffd(int last7ffd) {
        this.last7ffd = last7ffd;
        setBankMappings(calculateBankMappings(last7ffd, last1ffd));
    }

    public void setLast1ffd(int last1ffd) {
        this.last1ffd = last1ffd;
        setBankMappings(calculateBankMappings(last7ffd, last1ffd));
    }

    public Integer getRamBankAddress(int ramBank) {
        Integer address = BANK_SIZE;
        for (int i : bankMappings) {
            if (i == ramBank + RAM_1STBANK) {
                return address;
            }
            address += BANK_SIZE;
        }
        return null;
    }
}
