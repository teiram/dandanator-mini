package com.grelobites.romgenerator.handlers.dandanatormini.v6;

import com.grelobites.romgenerator.handlers.dandanatormini.DandanatorMiniConstants;
import com.grelobites.romgenerator.model.GameHeader;
import com.grelobites.romgenerator.model.GameType;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class GameHeaderV6Serializer {

    public static void serialize(SnapshotGame game, OutputStream os) throws IOException {
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
        if (game.getType() == GameType.RAM48) {
            os.write(DandanatorMiniConstants.PORT7FFD_DEFAULT_VALUE |
                    (game.getForce48kMode() ? DandanatorMiniConstants.PORT7FFD_FORCED_48KMODE_BITS : 0));
            os.write(GameUtil.encodeAsAuthentic(header.getPort1ffdValue(),
                    DandanatorMiniConstants.PORT1FFD_DEFAULT_VALUE));
        } else {
            os.write(GameUtil.encodeAsAuthentic(header.getPort7ffdValue(),
                    DandanatorMiniConstants.PORT7FFD_DEFAULT_VALUE));
            os.write(GameUtil.encodeAsAuthentic(header.getPort1ffdValue(),
                    DandanatorMiniConstants.PORT1FFD_DEFAULT_VALUE));
        }
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
        header.setPort7ffdValue(is.read());
        header.setPort1ffdValue(is.read());
        return header;
    }

}
