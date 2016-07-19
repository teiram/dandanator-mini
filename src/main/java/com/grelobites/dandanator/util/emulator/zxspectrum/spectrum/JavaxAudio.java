package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

/*
 * Audio support for Jasper
 * Author: Jan Bobrowski <jb@wizard.ae.krakow.pl>
 * License: GPL
 */

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

class JavaxAudio extends Audio {

    private SourceDataLine line;

    public JavaxAudio() throws Exception {
        try {
            AudioFormat fmt = new AudioFormat(8000, 8, 1, false, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);
            line.start();
        } catch (LineUnavailableException e) {
            throw new Exception(e.toString());
        }
    }

    public void play(byte[] buf, int len) {
        line.write(buf, 0, len);
    }

    public void setVolume(double vol) {
        super.setVolume(vol);
        xlat = new byte[div + 1];
        vol *= 255;
        for (int i = 0; i <= div; i++)
            xlat[i] = (byte) (vol * i / div);
    }

}
