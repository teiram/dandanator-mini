package com.grelobites.romgenerator.util.gameloader.loaders.tap;

import java.util.ArrayList;
import java.util.List;

public class BankedMemory implements Memory {

    private List<MemoryBank> memoryBanks;
    private int[] bankMappings;
    private int bankSize;
    private int topAddress;

    public BankedMemory(int[] bankMappings, int bankSize, int romBankCount, int ramBankCount) {
        this.bankSize = bankSize;
        memoryBanks = new ArrayList<>();

        for (int i = romBankCount + ramBankCount; i >= 0; i--) {
            memoryBanks.add(new MemoryBank(romBankCount-- > 0 ? MemoryBankType.ROM : MemoryBankType.RAM,
                    bankSize));
        }
        setBankMappings(bankMappings);
    }

    public void setBankMappings(int[] bankMappings) {
        this.bankMappings = bankMappings;
        topAddress = bankSize * bankMappings.length;
    }

    @Override
    public int peek8(int address) {
        if (address < topAddress) {
            int bankIndex = address / bankSize;
            int offset = address % bankSize;
            return memoryBanks.get(bankMappings[bankIndex]).getData()[offset];
        } else {
            throw new IllegalArgumentException("Address exceeds boundaries");
        }
    }

    @Override
    public void poke8(int address, int value) {
        if (address < topAddress) {
            int bankIndex = address / bankSize;
            int offset = address % bankSize;
            MemoryBank bank = memoryBanks.get(bankMappings[bankIndex]);
            if (bank.getType() == MemoryBankType.RAM) {
                bank.getData()[offset] = Integer.valueOf(value).byteValue();
            }
        } else {
            throw new IllegalArgumentException("Address exceeds boundaries");
        }
    }

    @Override
    public int peek16(int address) {
        int lsb = peek8(address);
        int msb = peek8(address + 1);
        return (msb << 8) | lsb;
    }

    @Override
    public void poke16(int address, int word) {
        poke8(address, word);
        poke8(address + 1, word >>> 8);
    }

    @Override
    public void load(byte[] data, int srcPos, int address, int size) {
        if (address + size < topAddress) {
            int copied = 0;
            while (copied < size) {
                int bankIndex = address / bankSize;
                int offset = address % bankSize;
                int chunk = bankSize - offset;
                System.arraycopy(data, copied, memoryBanks.get(bankMappings[bankIndex]).getData(), offset, chunk);
                copied += chunk;
                address += chunk;
            }
        } else {
            throw new IllegalArgumentException("Address exceeds boundaries");
        }
    }

    @Override
    public byte[] asByteArray() {
        return new byte[0];
    }
}
