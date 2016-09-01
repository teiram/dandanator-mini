package com.grelobites.romgenerator.util.compress;


import com.grelobites.romgenerator.util.compress.z80.Z80Compressor;
import com.grelobites.romgenerator.util.compress.zx7.Zx7Compressor;

public enum CompressorType {
    ZX7(Zx7Compressor.class),
    Z80(Z80Compressor.class);

    private Class<? extends Compressor> compressor;

    CompressorType(Class<? extends Compressor> compressor) {
        this.compressor = compressor;
    }

    public static CompressorType fromString(String type) {
        return CompressorType.valueOf(type.toUpperCase());
    }

    public Class<? extends Compressor> compressor() {
        return compressor;
    }
}
