package com.grelobites.romgenerator.zxspectrum;

public interface Peripheral {

    void bind(Z80VirtualMachine cpu) throws Exception;

    void unbind(Z80VirtualMachine cpu) throws Exception;

    void onCpuReset(Z80VirtualMachine cpu) throws Exception;

}
