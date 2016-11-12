package com.grelobites.romgenerator.media;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;

public class SoundLineTest {

        public static void main(String[] args) throws Exception {
            SourceDataLine soundLine = null;
            int BUFFER_SIZE = 64*1024;  // 64 KB

            DataLine.Info desiredLineInfo = new DataLine.Info(SourceDataLine.class,
                    new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 44100, 16, 2, 2, 44100, false));

            Mixer myMixer = null;

            Mixer.Info[] mixerInfo =  AudioSystem.getMixerInfo();
            for (Mixer.Info info : mixerInfo) {
                System.out.println("- Mixer " + info.getName() + " " + info.getClass());
                Mixer mixer = AudioSystem.getMixer(info);
                Line.Info[] lineInfo = mixer.getSourceLineInfo();
                System.out.println("Mixer " + info + " is format capable " +
                mixer.isLineSupported(desiredLineInfo));
                if (lineInfo.length > 0) {
                    System.out.println("Mixer " + info.getName() + " is output capable");
                    myMixer = mixer;
                    break;
                }
            }

            // Set up an audio input stream piped from the sound file.
            try {
                File soundFile = new File("/Users/mteira/Desktop/test.wav");
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);
                AudioFormat audioFormat = audioInputStream.getFormat();
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
                soundLine = (SourceDataLine) myMixer.getLine(info);
                //soundLine = (SourceDataLine) AudioSystem.getLine(info);
                soundLine.open(audioFormat);
                soundLine.start();
                int nBytesRead = 0;
                byte[] sampledData = new byte[BUFFER_SIZE];
                while (nBytesRead != -1) {
                    nBytesRead = audioInputStream.read(sampledData, 0, sampledData.length);
                    if (nBytesRead >= 0) {
                        // Writes audio data to the mixer via this source data line.
                        soundLine.write(sampledData, 0, nBytesRead);
                    }
                }
            } catch (UnsupportedAudioFileException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (LineUnavailableException ex) {
                ex.printStackTrace();
            } finally {
                if (soundLine != null) {
                    soundLine.drain();
                    soundLine.close();
                }
            }
        }

}
