package com.grelobites.dandanator.emulator;

import com.grelobites.dandanator.util.emulator.zxspectrum.J80;
import com.grelobites.dandanator.util.emulator.zxspectrum.spectrum.Spectrum48K;
import org.junit.Test;

public class EmulatorTest {

        @Test
        public void testJ80Cpu() throws Exception {
            J80 cpu = new J80();
            Spectrum48K spectrum = new Spectrum48K();
            cpu.addPeripheral(spectrum);
            cpu.load(EmulatorTest.class.getResourceAsStream("/spectrum.rom"), 0);
            cpu.init();
            cpu.start();


        }


}
