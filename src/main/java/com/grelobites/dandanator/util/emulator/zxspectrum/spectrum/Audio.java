package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

/*
 * Audio support for Jasper
 * Author: Jan Bobrowski <jb@wizard.ae.krakow.pl>
 * License: GPL
 */

abstract class Audio {

    static Class audioClass = null;
    public int div = 1125;
    public boolean mutef = false;
    protected byte[] xlat;
    double volume;
    private byte[] buf;
    private int pos;
    private int accT, accV;
    private boolean up = false;
    private int elapsed = -1;
    private int oldStates = 0;
    public Audio() {
        buf = new byte[300];
    }

    public static Audio getAudio() {
        Audio audio = null;
        if (audioClass != null) try {
            System.out.println("Using: " + audioClass.toString());
            audio = (Audio) audioClass.newInstance();
        } catch (Exception e) {
            System.out.println("! " + e.toString());
        } finally {
            return audio;
        }
        System.out.println("Initializing audio...");
        try {
            Class c = Class.forName("javax.sound.sampled.AudioSystem");
            c = Class.forName("j80.spectrum.JavaxAudio");
            System.out.println("Using: " + c.toString());
            audio = (Audio) c.newInstance();
            audioClass = c;
            return audio;
        } catch (Exception e) {
            System.out.println("! " + e.toString());
        }
        try {
//			Class c = Class.forName("com.softsynth.javasonics.core.SonicSystem");
            Class c = Class.forName("com.softsynth.javasonics.natdev.SonicNativeSystem");
            c = Class.forName("j80.spectrum.JavaSonicsAudio");
            System.out.println("Using: " + c.toString());
            audio = (Audio) c.newInstance();
            audioClass = c;
            return audio;
        } catch (Exception e) {
            System.out.println("! " + e.toString());
        }
        try {
            Class c = Class.forName("sun.audio.AudioData");
            c = Class.forName("j80.spectrum.SunAudio");
            System.out.println("Using: " + c.toString());
            audio = (Audio) c.newInstance();
            audioClass = c;
            return audio;
        } catch (Exception e) {
            System.out.println("! " + e.toString());
        }
        System.out.println("No audio found");
        return null;
    }

    public abstract void play(byte[] buf, int n);

    public void setVolume(double vol) {
        volume = vol;
    }

    public void drain() {
        try {
            play(buf, pos);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
        pos = 0;
    }

    private void write(int val, int n) {
        byte v = xlat[val];
        while (n > 0) try {
            buf[pos] = v;
            pos++;
            n--;
        } catch (ArrayIndexOutOfBoundsException e) {
            drain();
        }
    }

    public void reset() {
        accT = accV = 0;
        pos = 0;
        elapsed = -1;
    }

    public void pulse(int w, boolean chg) {

        if (w < 0)
            w = -w;
        if (w > 60000)
            w = 60000;

        if (chg)
            up = !up;

        if (mutef)
            return;
        if (elapsed < 0) {
            if (chg) elapsed = 0;
            return;
        }
        w += elapsed;
        if (w > 60000) {
            if (accT > 0) {
                write(accV + (div - accT) / 2, 1);
                write(div / 2, 1);
            }
            if (pos > 0)
                drain();
            reset();
            elapsed = chg ? 0 : -1;
            return;
        }

        if (!chg || w <= 0) {
            elapsed = w;
            return;
        }

        elapsed = 0;
        w *= 2;

        if (accT > 0) {
            if (accT + w < div) {
                accT += w;
                if (up) accV += w;
                return;
            }
            int n = div - accT;
            if (up) accV += n;
            write(accV, 1);
            w -= n;
        }
        write(up ? div : 0, w / div);
        accT = w % div;
        accV = up ? accT : 0;
    }

    public void mute(boolean v) {
        mutef = v;
        reset();
    }
}
