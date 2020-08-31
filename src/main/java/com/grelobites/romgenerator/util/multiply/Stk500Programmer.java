package com.grelobites.romgenerator.util.multiply;

import com.grelobites.romgenerator.util.Util;
import jssc.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Stk500Programmer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Stk500Programmer.class);

    private static final int PROGRAM_CHUNK_SIZE = 128;
    private static final int SERIAL_READ_TIMEOUT = 5000;
    private static final int SERIAL_DRAIN_TIMEOUT = 250;
    private static final int MAX_SYNC_ATTEMPTS = 3;

    private static final int RESP_STK_OK =          0x10;
    private static final int RESP_STK_FAILED =      0x11;
    private static final int RESP_STK_UNKNOWN =     0x12;
    private static final int RESP_STK_NODEVICE =    0x13;
    private static final int RESP_STK_INSYNC =      0x14;
    private static final int RESP_STK_NOSINK =      0x15;

    private static final int SYNC_CRC_EOP =         0x20;

    private static final int  CMND_STK_GET_SYNC =          0x30;
    private static final int  CMND_STK_GET_SIGN_ON =       0x31;

    private static final int  CMND_STK_SET_PARAMETER =     0x40;
    private static final int  CMND_STK_GET_PARAMETER =     0x41;
    private static final int  CMND_STK_SET_DEVICE =        0x42;
    private static final int  CMND_STK_SET_DEVICE_EXT =    0x45;

    private static final int  CMND_STK_ENTER_PROGMODE =    0x50;
    private static final int  CMND_STK_LEAVE_PROGMODE =    0x51;
    private static final int  CMND_STK_CHIP_ERASE =        0x52;
    private static final int  CMND_STK_CHECK_AUTOINC =     0x53;
    private static final int  CMND_STK_LOAD_ADDRESS =      0x55;
    private static final int  CMND_STK_UNIVERSAL =         0x56;
    private static final int  CMND_STK_UNIVERSAL_MULTI =   0x57;

    private static final int  CMND_STK_PROG_FLASH =        0x60;
    private static final int  CMND_STK_PROG_DATA =         0x61;
    private static final int  CMND_STK_PROG_FUSE =         0x62;
    private static final int  CMND_STK_PROG_LOCK =         0x63;
    private static final int  CMND_STK_PROG_PAGE =         0x64;
    private static final int  CMND_STK_PROG_FUSE_EXT =     0x65;

    private static final int  CMND_STK_READ_FLASH =        0x70;
    private static final int  CMND_STK_READ_DATA =         0x71;
    private static final int  CMND_STK_READ_FUSE =         0x72;
    private static final int  CMND_STK_READ_LOCK =         0x73;
    private static final int  CMND_STK_READ_PAGE =         0x74;
    private static final int  CMND_STK_READ_SIGN =         0x75;
    private static final int  CMND_STK_READ_OSCCAL =       0x76;
    private static final int  CMND_STK_READ_FUSE_EXT =     0x77;
    private static final int  CMND_STK_READ_OSCCAL_EXT =   0x78;

    private static final int PARM_STK_HW_VER            = 0x80; //R
    private static final int PARM_STK_SW_MAJOR          = 0x81; //R
    private static final int PARM_STK_SW_MINOR          = 0x82; //R
    private static final int PARM_STK_LEDS              = 0x83; //R/W
    private static final int PARM_STK_VTARGET           = 0x84; //R/W
    private static final int PARM_STK_VADJUST           = 0x85; //R/W
    private static final int PARM_STK_OSC_PSCALE        = 0x86; //R/W
    private static final int PARM_STK_OSC_CMATCH        = 0x87; //R/W
    private static final int PARM_STK_RESET_DURATION    = 0x88; //R/W
    private static final int PARM_STK_SCK_DURATION      = 0x89; //R/W

    private static final int PARM_STK_BUFSIZEL          = 0x90; //R/W, Range {0..255}
    private static final int PARM_STK_BUFSIZEH          = 0x91; //R/W, Range {0..255}
    private static final int PARM_STK_DEVICE            = 0x92; //R/W, Range {0..255}
    private static final int PARM_STK_PROGMODE          = 0x93; //'P' or 'S'
    private static final int PARM_STK_PARAMODE          = 0x94; //TRUE or FALSE
    private static final int PARM_STK_POLLING           = 0x95; //TRUE or FALSE
    private static final int PARM_STK_SELFTIMED         = 0x96; //TRUE or FALSE
    private static final int PARM_STK500_TOPCARD_DETECT = 0x98; //Detect top-card attached

    private static final byte[] ATMEGA_328P_SIGNATURE = {(byte) 0x1e, (byte) 0x95, (byte) 0x0F};

    private static class ParametersBuilder {
        List<Byte> parameters = new ArrayList<>();

        public ParametersBuilder withByte(int value) {
            parameters.add(Integer.valueOf(value).byteValue());
            return this;
        }

        public ParametersBuilder withChar(char value) {
            parameters.add((byte) value);
            return this;
        }

        public ParametersBuilder withLittleEndianShort(int value) {
            parameters.add(Integer.valueOf(value & 0xff).byteValue());
            parameters.add(Integer.valueOf((value >> 8) & 0xff).byteValue());
            return this;
        }

        public ParametersBuilder withBigEndianShort(int value) {
            parameters.add(Integer.valueOf((value >> 8) & 0xff).byteValue());
            parameters.add(Integer.valueOf(value & 0xff).byteValue());
            return this;
        }

        public ParametersBuilder withByteArray(byte[] value) {
            for (byte item : value) {
                parameters.add(item);
            }
            return this;
        }

        public byte[] build() {
            byte[] result = new byte[parameters.size()];
            int index = 0;
            for (byte value : parameters) {
                result[index++] = value;
            }
            return result;
        }

        public static ParametersBuilder newInstance() {
            return new ParametersBuilder();
        }

    }
    private SerialPort serialPort;

    private void handleResponse() {
        try {
            byte response = readByte();
            if (response == RESP_STK_INSYNC) {
                response = readByte();
                if (response != RESP_STK_OK) {
                    LOGGER.debug("Got non RESP_STK_OK response {}", response);
                    throw new RuntimeException("STK Operation returned error");
                }
            } else {
                LOGGER.warn("Got out of sync. Response: {}", Util.asByteHexString(response));
                throw new RuntimeException("Sync lost");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private byte readByte() {
        try {
            byte[] response = serialPort.readBytes(1, SERIAL_READ_TIMEOUT);
            LOGGER.debug("SERIAL PORT. Received {}", Util.dumpAsHexString(response));
            return response[0];
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
        }
    }

    public void initialize(int dtrRtsDelay, int afterDtrRtsDelay) {
        try {
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            LOGGER.debug("DTR/RTS set low");
            Thread.sleep(dtrRtsDelay);
            serialPort.setDTR(true);
            serialPort.setRTS(true);
            LOGGER.debug("DTR/RTS set high");
            Thread.sleep(afterDtrRtsDelay);
            purgeSerialPort();
        } catch (Exception e) {
            LOGGER.error("Clearing serial port", e);
            throw new RuntimeException(e);
        }
    }

    private void purgeSerialPort() {
        LOGGER.debug("SERIAL PORT. Flushing");
        while (true) {
            try {
                byte [] flushed = serialPort.readBytes(1, SERIAL_DRAIN_TIMEOUT);
                LOGGER.debug("SERIAL PORT. Flushed: {}", Util.dumpAsHexString(flushed));
            } catch (Exception e) {
                return;
            }
        }
    }

    private byte[] readBytes(int count) {
        try {
            byte[] response = serialPort.readBytes(count, SERIAL_READ_TIMEOUT);
            LOGGER.debug("SERIAL PORT. Received {}", Util.dumpAsHexString(response));
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Reading from serial port", e);
        }
    }

    private void sendCommand(byte[] command) {
        try {
            LOGGER.debug("SERIAL PORT. Sending {}", Util.dumpAsHexString(command));
            serialPort.writeBytes(command);
        } catch (Exception e) {
            LOGGER.error("In sendCommand", e);
            throw new RuntimeException(e);
        }
    }

    private void sendCommandAndHandleResponse(byte[] command) {
        sendCommand(command);
        handleResponse();
    }

    public Stk500Programmer(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    public void programBinary(Binary binary, boolean checkCurrent, boolean validate) throws IOException {
        programBinary(binary, null, checkCurrent, validate);
    }

    public void programBinary(Binary binary, ProgressListener listener,
                              boolean checkCurrent, boolean validate) throws IOException {
        byte[] data = binary.toByteArray();
        int chunks = (data.length + PROGRAM_CHUNK_SIZE - 1) / PROGRAM_CHUNK_SIZE;
        int address = binary.getAddress();
        for (int i = 0; i < chunks; i++) {
            boolean programmed = false;
            byte[] chunkData = Arrays.copyOfRange(data, i * PROGRAM_CHUNK_SIZE,
                    Math.min((i + 1) * PROGRAM_CHUNK_SIZE, data.length));
            if (!checkCurrent || !checkChunk(address, chunkData)) {
                programChunk(address, chunkData);
                programmed = true;
            }
            if (validate && programmed && !checkChunk(address, chunkData)) {
                LOGGER.error("Validating flash content");
                throw new IllegalStateException("Flash validation failed");
            }
            if (listener != null) {
                listener.onProgressUpdate(1.0 * (i + 1) / chunks);
            }
            address += PROGRAM_CHUNK_SIZE >> 1;
        }
    }

    private void programChunk(int address, byte[] data) {
        LOGGER.debug("Programming chunk with address={}, data={}",
                String.format("0x%04x", address), Util.dumpAsHexString(data));
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_LOAD_ADDRESS)
                .withLittleEndianShort(address)
                .withByte(SYNC_CRC_EOP).build());
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_PROG_PAGE)
                .withBigEndianShort(data.length)
                .withChar('F')
                .withByteArray(data)
                .withByte(SYNC_CRC_EOP)
                .build());
    }

    private boolean checkChunk(int address, byte[] data) {
        LOGGER.debug("Checking chunk with address={}",
                String.format("0x%04x", address));
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_LOAD_ADDRESS)
                .withLittleEndianShort(address)
                .withByte(SYNC_CRC_EOP).build());
        sendCommand(ParametersBuilder.newInstance()
                .withByte(CMND_STK_READ_PAGE)
                .withBigEndianShort(data.length)
                .withChar('F')
                .withByte(SYNC_CRC_EOP)
                .build());

        try {
            byte response = readByte();
            if (response == RESP_STK_INSYNC) {
                byte[] flashData = readBytes(data.length);
                boolean result = Arrays.equals(flashData, data);
                response = readByte();
                if (response != RESP_STK_OK) {
                    LOGGER.debug("Got non RESP_STK_OK response {}", response);
                    throw new RuntimeException("STK Operation returned error");
                }
                return result;
            } else {
                LOGGER.warn("Got out of sync. Value {}",
                        Util.asByteHexString(response));
                throw new RuntimeException("Sync lost");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void waitMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            LOGGER.warn("Thread interrupted on wait");
        }
    }
    public void sync() {
        byte[] syncCommand = ParametersBuilder.newInstance()
                .withByte(CMND_STK_GET_SYNC)
                //.withByte(CMND_STK_GET_SIGN_ON)
                .withByte(SYNC_CRC_EOP)
                .build();
        LOGGER.debug("Starting SYNC attempt");
        sendCommand(syncCommand);
        purgeSerialPort();
        waitMillis(100);
        sendCommand(syncCommand);
        purgeSerialPort();
        waitMillis(100);
        int syncAttempts = MAX_SYNC_ATTEMPTS;
        while (true) {
            try {
                sendCommandAndHandleResponse(syncCommand);
                return;
            } catch (Exception e) {
                if (--syncAttempts == 0) {
                    throw e;
                }
            }
        }
    }

    public void enterProgramMode() {
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_SET_DEVICE) //Set device program parameters
                .withByte(0x86) //devicecode
                .withByte(0x00) //revision
                .withByte(0x00) //progtype
                .withByte(0x01) //parmode
                .withByte(0x01) //polling
                .withByte(0x01) //selftimed
                .withByte(0x01) //lockbytes
                .withByte(0x03) //fusebytes
                .withByte(0xFF) //flashpollval1
                .withByte(0xFF) //Flashpollval2
                .withByte(0xFF) //eeprompollval1
                .withByte(0xFF) //eeprompollval2
                .withByte(0x00) //pagesizehigh
                .withByte(0x80) //pagesizelow
                .withByte(0x04) //eepromsizehigh
                .withByte(0x00) //eepromsizelow
                .withByte(0x00) //flashsize4
                .withByte(0x00) //flashsize3
                .withByte(0x80) //flashsize2
                .withByte(0x00) //flashsize1
                .withByte(SYNC_CRC_EOP)
                .build());
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_SET_DEVICE_EXT) //Set device extended parameters
                .withByte(0x05) //Command size
                .withByte(0x04) //eeprompagesize
                .withByte(0xD7) //signalpagel
                .withByte(0xC2) //signalbs2
                .withByte(0x00) //resetdisable
                .withByte(SYNC_CRC_EOP).build());

        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_ENTER_PROGMODE) //Enter program mode
                .withByte(SYNC_CRC_EOP)
                .build());
    }

    public void leaveProgramMode() {
        sendCommandAndHandleResponse(ParametersBuilder.newInstance()
                .withByte(CMND_STK_LEAVE_PROGMODE)
                .withByte(SYNC_CRC_EOP)
                .build());
    }

    private int readParameter(byte[] command) {
        sendCommand(command);
        byte response = readByte();
        if (response == RESP_STK_INSYNC) {
            int value = readByte();
            LOGGER.debug("Got parameter value {}", Util.asByteHexString(value));
            response = readByte();
            if (response != RESP_STK_OK) {
                LOGGER.debug("Got non RESP_STK_OK response {}", response);
                throw new RuntimeException("STK Operation returned error");
            }
            return value;
        } else {
            LOGGER.debug("Not in sync. Value: {}", Util.asByteHexString(response));
            throw new IllegalStateException("Sync lost");
        }
    }

    public int getMajorVersion() {
        return readParameter(ParametersBuilder.newInstance()
                .withByte(CMND_STK_GET_PARAMETER)
                .withByte(PARM_STK_SW_MAJOR)
                .withByte(SYNC_CRC_EOP)
                .build());
    }

    public int getMinorVersion() {
        return readParameter(ParametersBuilder.newInstance()
                .withByte(CMND_STK_GET_PARAMETER)
                .withByte(PARM_STK_SW_MINOR)
                .withByte(SYNC_CRC_EOP)
                .build());
    }

    public byte[] getDeviceSignature() {
        sendCommand(ParametersBuilder.newInstance()
                .withByte(CMND_STK_READ_SIGN)
                .withByte(SYNC_CRC_EOP)
                .build());
        byte response = readByte();
        if (response == RESP_STK_INSYNC) {
            LOGGER.debug("Got RESP_STK_INSYNC code");
            byte[] signature = readBytes(3);
            response = readByte();
            if (response != RESP_STK_OK) {
                LOGGER.debug("Got non RESP_STK_OK response {}", response);
                throw new RuntimeException("STK Operation returned error");
            }
            return signature;
        } else {
            throw new IllegalStateException("Sync lost");
        }
    }

    public boolean supportedSignature(byte[] signature) {
        return Arrays.equals(ATMEGA_328P_SIGNATURE, signature);
    }

}
