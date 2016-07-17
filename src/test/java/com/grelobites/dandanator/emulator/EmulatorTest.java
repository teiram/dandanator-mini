package com.grelobites.dandanator.emulator;

import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.spectrum.Spectrum48K;
import org.junit.Test;

public class EmulatorTest {

        @Test
        public void testJ80Cpu() throws Exception {
            Z80VirtualMachine cpu = new Z80VirtualMachine();
            Spectrum48K spectrum = new Spectrum48K();
            cpu.addPeripheral(spectrum);
            cpu.load(EmulatorTest.class.getResourceAsStream("/spectrum.rom"), 0);
            cpu.run();


        }


}
