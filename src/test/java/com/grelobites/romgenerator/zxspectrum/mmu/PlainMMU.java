package com.grelobites.romgenerator.zxspectrum.mmu;


import com.grelobites.romgenerator.zxspectrum.Z80VirtualMachine;
import com.grelobites.romgenerator.zxspectrum.MMU;

public class PlainMMU implements MMU {
    private int memory[] = new int[0x10000];

    @Override
    public int peekb(int add) {
        return memory[add & 0xffff] & 0xff;
    }

    @Override
    public void pokeb(int add, int value) {
        memory[add & 0xffff] = value & 0xff;
    }

    @Override
    public void unbind(Z80VirtualMachine cpu) {
    }

    @Override
    public void bind(Z80VirtualMachine cpu) {
    }

    @Override
    public void onCpuReset(Z80VirtualMachine cpu) {
    }

}


