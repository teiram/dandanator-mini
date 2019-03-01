package com.grelobites.romgenerator.model;

import com.grelobites.romgenerator.util.Util;

import java.io.IOException;

public class DanTapConstants {
    public static final int COMMON_CODE_SIZE = 140;
    public static final int TABLE_CODE_SIZE = 111;
    public static final int TAP_TABLE_ENTRY_SIZE = 8;

    private static final String COMMON_CODE_RESOURCE = "dan-tap/common-code.bin";
    private static final String TABLE_CODE_RESOURCE = "dan-tap/table-code.bin";

    private static byte[] COMMON_CODE;
    private static byte[] TABLE_CODE;

    private static byte[] getResource(String resourceName) throws IOException {
        return Util.fromInputStream(DanTapConstants.class.getClassLoader()
            .getResourceAsStream(resourceName));
    }

    public static byte[] getCommonCode() throws IOException {
        if (COMMON_CODE == null) {
            COMMON_CODE = getResource(COMMON_CODE_RESOURCE);
        }
        return COMMON_CODE;
    }

    public static byte[] getTableCode() throws IOException {
        if (TABLE_CODE == null) {
            TABLE_CODE = getResource(TABLE_CODE_RESOURCE);
        }
        return TABLE_CODE;
    }
}
