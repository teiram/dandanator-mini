package com.grelobites.romgenerator.media;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class LineInfo {

    public static void main(String[] args) throws Exception {

        DataLine.Info desiredLineInfo = new DataLine.Info(SourceDataLine.class,
                new AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 44100, 8, 2, 2, 44100, false));

        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
        for (Mixer.Info info : mixerInfo) {
            System.out.println("+ Mixer " + info.getName() + " " + info);
            Mixer mixer = AudioSystem.getMixer(info);
            Line.Info[] lineInfos = mixer.getSourceLineInfo();
            System.out.println(" -   Supports base format " + mixer.isLineSupported(desiredLineInfo));
            for (Line.Info lineInfo : lineInfos) {
                Line line = mixer.getLine(lineInfo);
                System.out.println(" -      Line is " + line + ", for lineInfo " + lineInfo);
                if (line instanceof DataLine) {
                    System.out.println(" -      Line format is " + ((DataLine) line).getFormat());
                }
            }
            if (lineInfos.length > 0) {
                System.out.println(" -    Mixer " + info.getName() + " is output capable");
            }
        }
    }
}
