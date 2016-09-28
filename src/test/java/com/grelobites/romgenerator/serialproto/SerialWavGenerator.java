package com.grelobites.romgenerator.serialproto;


import com.grelobites.romgenerator.util.player.WavOutputFormat;

import java.io.File;
import java.io.FileOutputStream;

public class SerialWavGenerator {

    public static void main(String[] args) throws Exception {
        File outputFile = new File(args[0]);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                SerialWavOutputStream wos = new SerialWavOutputStream(
                        fos,
                        WavOutputFormat.defaultDataFormat());
                for (int i = 0; i < 256; i++) {
                    wos.write(i);
                }
                wos.close();
            }

    }
}
