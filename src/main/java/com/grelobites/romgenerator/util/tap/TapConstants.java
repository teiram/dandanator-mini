package com.grelobites.romgenerator.util.tap;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TapConstants {
    private static final Logger LOGGER = LoggerFactory.getLogger(TapConstants.class);

    public static final int HEADER_LENGTH = 19;
    public static final int NAME_LENGTH = 10;
    public static final int CHECKSUM_INDEX = 20;

    public static final int PROGRAM_TYPE = 0;
    public static final int NUMBERARRAY_TYPE = 1;
    public static final int CHARARRAY_TYPE = 2;
    public static final int CODE_TYPE = 3;

    public static final int HEADER_FLAG = 0;
    public static final int DATA_FLAG = 0xff;

    public static final int NO_PARAMETER_VALUE = 32768;

}
