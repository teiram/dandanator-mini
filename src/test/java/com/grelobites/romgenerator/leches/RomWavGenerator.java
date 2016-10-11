package com.grelobites.romgenerator.leches;


import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.player.ChannelType;
import com.grelobites.romgenerator.util.player.CompressedWavOutputStream;
import com.grelobites.romgenerator.util.player.CompressedWavOutputFormat;

import java.io.File;
import java.io.FileOutputStream;


public class RomWavGenerator {

    public static void main(String[] args) throws Exception {
        byte[] romData = Util.fromInputStream(RomWavGenerator.class.getResourceAsStream("/48.rom"));
        File outputFile = new File(args[0]);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            CompressedWavOutputStream wos = new CompressedWavOutputStream(
                    fos,
                    CompressedWavOutputFormat.builder()
                            .withSampleRate(CompressedWavOutputFormat.SRATE_44100)
                            .withChannelType(ChannelType.STEREOINV)
                            .withFlagByte(CompressedWavOutputFormat.DATA_FLAG_BYTE)
                            .withSpeed(4)
                            .withOffset(0)
                            .withPilotDurationMillis(2000)
                            .withFinalPauseDurationMillis(500)
                            .build());
            wos.write(romData);
            wos.write(romData);
            wos.write(1);
            wos.close();
    }

}
}
