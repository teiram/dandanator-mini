package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.util.GameUtil;
import com.grelobites.romgenerator.util.SNAHeader;
import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class GameHeader {
    private static final Logger LOGGER = LoggerFactory.getLogger(GameHeader.class);
    private static final int REGISTER_MASK = 0xffff;
    private static final List<Integer> VALID_INTERRUPT_MODES = Arrays.asList(new Integer[] {0, 1, 2});

    private Integer iRegister;
    private Integer alternateHLRegister;
    private Integer alternateDERegister;
    private Integer alternateBCRegister;
    private Integer alternateAFRegister;
    private Integer hlRegister;
    private Integer deRegister;
    private Integer bcRegister;
    private Integer iyRegister;
    private Integer ixRegister;
    private Integer interruptEnable;
    private Integer rRegister;
    private Integer afRegister;
    private Integer spRegister;
    private Integer interruptMode;
    private Integer borderColor;
    private Integer pcRegister;
    private Integer savedStackData;
    private Integer port7ffdValue;
    private Integer port1ffdValue;

    public Integer getIRegister() {
        return iRegister;
    }

    public void setIRegister(int iRegister) {
        this.iRegister = iRegister;
    }

    public Integer getAlternateHLRegister() {
        return alternateHLRegister;
    }

    public void setAlternateHLRegister(int alternateHLRegister) {
        this.alternateHLRegister = alternateHLRegister;
    }

    public Integer getAlternateDERegister() {
        return alternateDERegister;
    }

    public void setAlternateDERegister(int alternateDERegister) {
        this.alternateDERegister = alternateDERegister;
    }

    public Integer getAlternateBCRegister() {
        return alternateBCRegister;
    }

    public void setAlternateBCRegister(int alternateBCRegister) {
        this.alternateBCRegister = alternateBCRegister;
    }

    public Integer getAlternateAFRegister() {
        return alternateAFRegister;
    }

    public void setAlternateAFRegister(int alternateAFRegister) {
        this.alternateAFRegister = alternateAFRegister;
    }

    public Integer getHLRegister() {
        return hlRegister;
    }

    public void setHLRegister(int hlRegister) {
        this.hlRegister = hlRegister;
    }

    public Integer getDERegister() {
        return deRegister;
    }

    public void setDERegister(int deRegister) {
        this.deRegister = deRegister;
    }

    public Integer getBCRegister() {
        return bcRegister;
    }

    public void setBCRegister(int bcRegister) {
        this.bcRegister = bcRegister;
    }

    public Integer getIYRegister() {
        return iyRegister;
    }

    public void setIYRegister(int iyRegister) {
        this.iyRegister = iyRegister;
    }

    public Integer getIXRegister() {
        return ixRegister;
    }

    public void setIXRegister(int ixRegister) {
        this.ixRegister = ixRegister;
    }

    public Integer getInterruptEnable() {
        return interruptEnable;
    }

    public void setInterruptEnable(int interruptEnable) {
        this.interruptEnable = interruptEnable;
    }

    public Integer getRRegister() {
        return rRegister;
    }

    public void setRRegister(int rRegister) {
        this.rRegister = rRegister;
    }

    public Integer getAFRegister() {
        return afRegister;
    }

    public void setAFRegister(int afRegister) {
        this.afRegister = afRegister;
    }

    public Integer getSPRegister() {
        return spRegister;
    }

    public void setSPRegister(int spRegister) {
        if (GameUtil.isSPValid(spRegister)) {
            this.spRegister = spRegister;
        } else {
            throw new IllegalArgumentException("Invalid SP register: 0x" + Integer.toHexString(spRegister));
        }
    }

    public Integer getInterruptMode() {
        return interruptMode;
    }

    public void setInterruptMode(int interruptMode) {
        if (VALID_INTERRUPT_MODES.contains(interruptMode)) {
            this.interruptMode = interruptMode;
        } else {
            throw new IllegalArgumentException("Invalid interrupt mode: " + interruptMode);
        }
    }

    public Integer getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
    }

    public Integer getPCRegister() {
        return pcRegister;
    }

    public void setPCRegister(int pcRegister) {
        this.pcRegister = pcRegister;
    }

    public Integer getSavedStackData() {
        return savedStackData;
    }

    public Integer getSavedStackData(int defaultValue) {
        return savedStackData != null ? savedStackData : defaultValue;
    }

    public void setSavedStackData(int savedStackData) {
        this.savedStackData = savedStackData;
    }

    public Integer getPort7ffdValue() {
        return port7ffdValue;
    }

    public Integer getPort7ffdValue(int defaultValue) {
        return port7ffdValue != null ? port7ffdValue : defaultValue;
    }

    public void setPort7ffdValue(Integer port7ffdValue) {
        this.port7ffdValue = port7ffdValue;
    }

    public Integer getPort1ffdValue() {
        return port1ffdValue;
    }

    public Integer getPort1ffdValue(int defaultValue) {
        return port1ffdValue != null ? port1ffdValue : defaultValue;
    }

    public void setPort1ffdValue(Integer port1ffdValue) {
        this.port1ffdValue = port1ffdValue;
    }

    public static GameHeader from48kSnaGameByteArray(byte[] in) {
        GameHeader header = new GameHeader();
        header.setIRegister(in[SNAHeader.REG_I]);
        header.setAlternateHLRegister(Util.readAsLittleEndian(in, SNAHeader.REG_HL_alt));
        header.setAlternateDERegister(Util.readAsLittleEndian(in, SNAHeader.REG_DE_alt));
        header.setAlternateBCRegister(Util.readAsLittleEndian(in, SNAHeader.REG_BC_alt));
        header.setAlternateAFRegister(Util.readAsLittleEndian(in, SNAHeader.REG_AF_alt));
        header.setHLRegister(Util.readAsLittleEndian(in, SNAHeader.REG_HL));
        header.setDERegister(Util.readAsLittleEndian(in, SNAHeader.REG_DE));
        header.setBCRegister(Util.readAsLittleEndian(in, SNAHeader.REG_BC));
        header.setIYRegister(Util.readAsLittleEndian(in, SNAHeader.REG_IY));
        header.setIXRegister(Util.readAsLittleEndian(in, SNAHeader.REG_IX));
        header.setInterruptEnable(in[SNAHeader.INTERRUPT_ENABLE]);
        header.setRRegister(in[SNAHeader.REG_R]);
        header.setAFRegister(Util.readAsLittleEndian(in, SNAHeader.REG_AF));
        header.setSPRegister(Util.readAsLittleEndian(in, SNAHeader.REG_SP));
        header.setInterruptMode(in[SNAHeader.INTERRUPT_MODE]);
        header.setBorderColor(in[SNAHeader.BORDER_COLOR]);
        return header;
    }

    public static GameHeader from128kSnaGameByteArray(byte[] in) {
        GameHeader header = from48kSnaGameByteArray(in);
        int extendedHeaderOffset = Constants.SNA_HEADER_SIZE + Constants.SLOT_SIZE * 3;
        header.setPCRegister(Util.readAsLittleEndian(in, extendedHeaderOffset));
        header.setPort7ffdValue(Byte.toUnsignedInt(in[extendedHeaderOffset + 2]));
        return header;
    }

    @Override
    public String toString() {
        return "GameHeader{" +
                "iRegister=" + Util.toHexString(iRegister) +
                ", alternateHLRegister=" + Util.toHexString(alternateHLRegister) +
                ", alternateDERegister=" + Util.toHexString(alternateDERegister) +
                ", alternateBCRegister=" + Util.toHexString(alternateBCRegister) +
                ", alternateAFRegister=" + Util.toHexString(alternateAFRegister) +
                ", hlRegister=" + Util.toHexString(hlRegister) +
                ", deRegister=" + Util.toHexString(deRegister) +
                ", bcRegister=" + Util.toHexString(bcRegister) +
                ", iyRegister=" + Util.toHexString(iyRegister) +
                ", ixRegister=" + Util.toHexString(ixRegister) +
                ", interruptEnable=" + Util.toHexString(interruptEnable) +
                ", rRegister=" + Util.toHexString(rRegister) +
                ", afRegister=" + Util.toHexString(afRegister) +
                ", spRegister=" + Util.toHexString(spRegister) +
                ", interruptMode=" + Util.toHexString(interruptMode) +
                ", borderColor=" + Util.toHexString(borderColor) +
                ", pcRegister=" + Util.toHexString(pcRegister) +
                ", savedStackData=" + Util.toHexString(savedStackData) +
                ", port7ffdValue=" + Util.toHexString(port7ffdValue) +
                ", port1ffdValue=" + Util.toHexString(port1ffdValue) +
                '}';
    }
}
