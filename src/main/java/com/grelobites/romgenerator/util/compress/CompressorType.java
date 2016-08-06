package com.grelobites.romgenerator.util.compress;


import com.grelobites.romgenerator.util.compress.zx7b.Zx7bCompressor;

public enum CompressorType {
    ZX7B(Zx7bCompressor.class);

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
