package com.grelobites.romgenerator.handlers.dandanatormini.v5;

import com.grelobites.romgenerator.model.Game;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.RamGame;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.sun.tools.doclets.formats.html.markup.HtmlStyle.header;

public class GameHeaderV5Serializer {

    private static final int DEFAULT_PORT1FFD_VALUE = 0x04;
    private static final int SYNTHETIC_PORT_FLAG = 0x80;
    private static final int DEFAULT_PORT7FFD_VALUE_NON_FORCED = 0x10;
    private static final int DEFAULT_PORT7FFD_VALUE_FORCED = 0x30;

    public static void serialize(RamGame game, OutputStream os) throws IOException {
        GameHeader header = game.getGameHeader();
        os.write(header.getIRegister());
        Util.writeAsLittleEndian(os, header.getAlternateHLRegister());
        Util.writeAsLittleEndian(os, header.getAlternateDERegister());
        Util.writeAsLittleEndian(os, header.getAlternateBCRegister());
        Util.writeAsLittleEndian(os, header.getAlternateAFRegister());
        Util.writeAsLittleEndian(os, header.getHLRegister());
        Util.writeAsLittleEndian(os, header.getDERegister());
        Util.writeAsLittleEndian(os, header.getBCRegister());
        Util.writeAsLittleEndian(os, header.getIYRegister());
        Util.writeAsLittleEndian(os, header.getIXRegister());
        os.write(header.getInterruptEnable());
        os.write(header.getRRegister());
        Util.writeAsLittleEndian(os, header.getAFRegister());
        Util.writeAsLittleEndian(os, header.getSPRegister());
        os.write(header.getInterruptMode());
        os.write(header.getBorderColor());
        Util.writeAsLittleEndian(os, header.getSavedStackData(0));
        os.write(header.getPort7ffdValue(SYNTHETIC_PORT_FLAG |
                (game.getForce48kMode() ? DEFAULT_PORT7FFD_VALUE_FORCED :
                DEFAULT_PORT7FFD_VALUE_NON_FORCED)));
        os.write(header.getPort1ffdValue(DEFAULT_PORT1FFD_VALUE | SYNTHETIC_PORT_FLAG));
    }

    private static Integer filterSynthetic(int value) {
        return (value & SYNTHETIC_PORT_FLAG) == 0 ? null : value;
    }

    public static GameHeader deserialize(InputStream is) throws IOException {
        GameHeader header = new GameHeader();
        header.setIRegister(is.read());
        header.setAlternateHLRegister(Util.readAsLittleEndian(is));
        header.setAlternateDERegister(Util.readAsLittleEndian(is));
        header.setAlternateBCRegister(Util.readAsLittleEndian(is));
        header.setAlternateAFRegister(Util.readAsLittleEndian(is));
        header.setHLRegister(Util.readAsLittleEndian(is));
        header.setDERegister(Util.readAsLittleEndian(is));
        header.setBCRegister(Util.readAsLittleEndian(is));
        header.setIYRegister(Util.readAsLittleEndian(is));
        header.setIXRegister(Util.readAsLittleEndian(is));
        header.setInterruptEnable(is.read());
        header.setRRegister(is.read());
        header.setAFRegister(Util.readAsLittleEndian(is));
        header.setSPRegister(Util.readAsLittleEndian(is));
        header.setInterruptMode(is.read());
        header.setBorderColor(is.read());
        header.setSavedStackData(Util.readAsLittleEndian(is));
        header.setPort7ffdValue(filterSynthetic(is.read()));
        header.setPort1ffdValue(filterSynthetic(is.read()));
        return header;
    }

}
