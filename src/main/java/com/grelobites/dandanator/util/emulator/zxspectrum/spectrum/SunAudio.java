package com.grelobites.dandanator.util.emulator.zxspectrum.spectrum;

/*
 * Audio support for Jasper
 * Author: Jan Bobrowski <jb@wizard.ae.krakow.pl>
 * License: GPL
 */

import sun.audio.AudioData;
import sun.audio.AudioDataStream;
import sun.audio.AudioPlayer;

class SunAudio extends Audio {
    public void play(byte[] buf, int len) {
        if (buf.length != len) {
            byte[] b = new byte[len];
            System.arraycopy(buf, 0, b, 0, len);
            buf = b;
        }
        AudioData playable = new AudioData(buf);
        AudioDataStream stream = new AudioDataStream(playable);
        AudioPlayer.player.start(stream);
    }

    public void setVolume(double vol) {
        double m = vol * 32635 / div;
        super.setVolume(vol);
        xlat = new byte[div + 1];
        for (int i = 0; i <= div; i++) {
            int x = 2 * i - div;
            x *= m;
            int s = 0;
            if (x < 0) {
                s = 0x80;
                x = -x;
            }
            x += 132;
            while ((x & 0x7F00) != 0) {
                s += 0x10;
                x >>>= 1;
            }
            s |= x >>> 3 & 0xF;
            xlat[i] = (byte) ~s;
        }
    }

    public String toString() {
        return "Sun sound system $Revision: 330 $";
    }

/*	public void setVolume(double vol) {
		super.setVolume(vol);
		xlat = new byte[div+1];
		for(int i=0; i<=div; i++) {
			int v = (int)(vol*CLIP*(2.0*i/div - 1));
			xlat[i] = toUlaw(v);
		}
	}

    private static final int[] expLut = {
	0, 0, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 
	4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 
	5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 
	5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 
	6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 
	6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 
	6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 
	6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 
	7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7
	};
    private static final int CLIP = 32635;
    private static final int BIAS = 0x84;

    private static byte toUlaw( int linear )
	{
	int sign = 0, exponent, mantissa;

	// Get the sample into sign-magnitude.
	if ( linear < 0 ) {
	    sign = 0x80;
	    linear = -linear;
	}
        if ( linear > CLIP )
	    linear = CLIP;	// clip the magnitude

	// Convert from 16 bit linear to ulaw.
	linear = linear + BIAS;
	exponent = expLut[linear>>7 & 0xFF];
	mantissa = linear>>exponent+3 & 0x0F;
	return (byte) ~( sign | exponent<<4 | mantissa );
    }*/
}
