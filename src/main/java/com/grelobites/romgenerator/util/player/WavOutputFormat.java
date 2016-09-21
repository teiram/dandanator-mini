package com.grelobites.romgenerator.util.player;

public class WavOutputFormat {
    public static final int SRATE_44100 = 44100;
    public static final int SRATE_48000 = 48000;
    public static final int HEADER_FLAG_BYTE = 0;
    public static final int DATA_FLAG_BYTE = 0xff;

    public static final int DEFAULT_PILOT_DURATION = 1000;
    public static final int DEFAULT_PAUSE_DURATION = 1000;
    public static final int DEFAULT_SPEED = 3;
    public static final int DEFAULT_OFFSET = 0;
    private int sampleRate;
    private ChannelType channelType;
    private int flagByte;
    private int speed;
    private int offset;
    private int pilotDurationMillis;
    private int finalPauseDurationMillis;

    public static class Builder {
        private WavOutputFormat outputFormat = new WavOutputFormat();

        public Builder withSampleRate(int sampleRate) {
            outputFormat.setSampleRate(sampleRate);
            return this;
        }

        public Builder withChannelType(ChannelType channelType) {
            outputFormat.setChannelType(channelType);
            return this;
        }

        public Builder withFlagByte(int flagByte) {
            outputFormat.setFlagByte(flagByte);
            return this;
        }

        public Builder withSpeed(int speed) {
            outputFormat.setSpeed(speed);
            return this;
        }

        public Builder withOffset(int offset) {
            outputFormat.setOffset(offset);
            return this;
        }

        public Builder withPilotDurationMillis(int pilotDurationMillis) {
            outputFormat.setPilotDurationMillis(pilotDurationMillis);
            return this;
        }

        public Builder withFinalPauseDurationMillis(int finalPauseDurationMillis) {
            outputFormat.setFinalPauseDurationMillis(finalPauseDurationMillis);
            return this;
        }

        public WavOutputFormat build() {
            return outputFormat;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WavOutputFormat defaultHeaderFormat() {
        return new Builder().withSampleRate(SRATE_44100)
                .withChannelType(ChannelType.MONO)
                .withFlagByte(HEADER_FLAG_BYTE)
                .withSpeed(DEFAULT_SPEED)
                .withOffset(DEFAULT_OFFSET)
                .withPilotDurationMillis(DEFAULT_PILOT_DURATION)
                .withFinalPauseDurationMillis(DEFAULT_PAUSE_DURATION)
                .build();
    }

    public static WavOutputFormat defaultDataFormat() {
        return new Builder().withSampleRate(SRATE_44100)
                .withChannelType(ChannelType.MONO)
                .withFlagByte(DATA_FLAG_BYTE)
                .withSpeed(DEFAULT_SPEED)
                .withOffset(DEFAULT_OFFSET)
                .withPilotDurationMillis(DEFAULT_PILOT_DURATION)
                .withFinalPauseDurationMillis(DEFAULT_PAUSE_DURATION)
                .build();
    }

    public WavOutputFormat(int sampleRate, ChannelType channelType, int flagByte,
                           int speed, int offset, int pilotDurationMillis,
                           int finalPauseDurationMillis) {
        this.sampleRate = sampleRate;
        this.channelType = channelType;
        this.flagByte = flagByte;
        this.speed = speed;
        this.offset = offset;
        this.pilotDurationMillis = pilotDurationMillis;
        this.finalPauseDurationMillis = finalPauseDurationMillis;
    }

    public WavOutputFormat() {}

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public int getFlagByte() {
        return flagByte;
    }

    public void setFlagByte(int flagByte) {
        this.flagByte = flagByte;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getPilotDurationMillis() {
        return pilotDurationMillis;
    }

    public void setPilotDurationMillis(int pilotDurationMillis) {
        this.pilotDurationMillis = pilotDurationMillis;
    }

    public int getFinalPauseDurationMillis() {
        return finalPauseDurationMillis;
    }

    public void setFinalPauseDurationMillis(int finalPauseDurationMillis) {
        this.finalPauseDurationMillis = finalPauseDurationMillis;
    }
}
