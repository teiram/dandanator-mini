package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;


import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.OutPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;
import com.grelobites.dandanator.util.emulator.zxspectrum.Stepper;

public class Speaker implements Peripheral, OutPort {
    private Audio audio = null;
    private boolean beeper = false;
    private int beepStates = 0;
    private Z80VirtualMachine cpu;

    public void resetCPU(Z80VirtualMachine cpu) {
    }

    public void disconnectCPU(Z80VirtualMachine cpu) {
    }

    public void outb(int port, int value, int tstates) {
        switch (port) {
            case 0xfe:
                boolean v = (value & 0x10) != 0;
                if (v != beeper) {
                    beeper = v;
                    if (audio != null) {
                        int t = cpu.getCycle();
                        audio.pulse(t - beepStates, true);
                        beepStates = t;
                    }
                }
                break;
        }
    }

    public void connectCPU(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;

        // Sound
        audio = Audio.getAudio();

        if (audio != null) {
            audio.setVolume(0.5);


            cpu.addOutPort(254, this);

            cpu.addStepper(new Stepper() {
                public void step(Z80VirtualMachine cpu) {
                    if (audio != null) {
                        int t = cpu.getCycle();
                        audio.pulse(t - beepStates, false);
                        beepStates = t;
                    }
                }
            });
        }
    }


    public String toString() {
        String driver;
        if (audio != null)
            driver = audio.toString();
        else
            driver = "Not installed";

        return "Speaker $Revision: 330 $  driver : " + driver;
    }

}
