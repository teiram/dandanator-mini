package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;


import com.grelobites.dandanator.util.emulator.zxspectrum.Z80VirtualMachine;
import com.grelobites.dandanator.util.emulator.zxspectrum.OutputPort;
import com.grelobites.dandanator.util.emulator.zxspectrum.Peripheral;

public class Speaker implements Peripheral, OutputPort {
    private Audio audio = null;
    private boolean beeper = false;
    private int beepStates = 0;
    private Z80VirtualMachine cpu;

    public void onCpuReset(Z80VirtualMachine cpu) {
    }

    public void unbind(Z80VirtualMachine cpu) {
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

    public void bind(Z80VirtualMachine cpu) throws Exception {
        this.cpu = cpu;

        // Sound
        audio = Audio.getAudio();

        if (audio != null) {
            audio.setVolume(0.5);


            cpu.addOutPort(254, this);

            cpu.addStepper(c -> {
                if (audio != null) {
                    int t = c.getCycle();
                    audio.pulse(t - beepStates, false);
                    beepStates = t;
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
