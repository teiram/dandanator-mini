package com.grelobites.romgenerator.util.player;

public class StandardWavOutputFormat {
    public static final int SRATE_44100 = 44100;
    public static final int SRATE_48000 = 48000;

    private static final int DEFAULT_LOW_VALUE = 0x40;
    private static final int DEFAULT_HIGH_VALUE = 0xC0;

    private static final int PILOT_DURATION = 3000;
    private static final int ZERO_DURATION_STANDARD = 748;
    private static final int ONE_DURATION_STANDARD = 1496;

    private static final int ZERO_DURATION_TURBO_1 = 392;
    private static final int ONE_DURATION_TURBO_1 = 784;

    private static final int ZERO_DURATION_TURBO_2 = 262;
    private static final int ONE_DURATION_TURBO_2 = 524;

    public static final int[] ZERO_DURATIONS = new int[] {ZERO_DURATION_STANDARD, ZERO_DURATION_TURBO_1, ZERO_DURATION_TURBO_2};
    public static final int[] ONE_DURATIONS = new int[] {ONE_DURATION_STANDARD, ONE_DURATION_TURBO_1, ONE_DURATION_TURBO_2};

    private static final int LEAD_OUT_DURATION = 250;

    private int sampleRate = SRATE_48000;
    private ChannelType channelType = ChannelType.STEREOINV;
    private int pilotDurationMillis = PILOT_DURATION;
    private int oneDurationTStates = ONE_DURATION_STANDARD;
    private int zeroDurationTStates = ZERO_DURATION_STANDARD;
    private int leadOutDurationMillis = LEAD_OUT_DURATION;
    private int lowValue = DEFAULT_LOW_VALUE;
    private int highValue = DEFAULT_HIGH_VALUE;
    private boolean reversePhase = false;

    public static class Builder {
        private StandardWavOutputFormat outputFormat = new StandardWavOutputFormat();

        public Builder withSampleRate(int sampleRate) {
            outputFormat.setSampleRate(sampleRate);
            return this;
        }

        public Builder withChannelType(ChannelType channelType) {
            outputFormat.setChannelType(channelType);
            return this;
        }

        public Builder withPilotDurationMillis(int pilotDurationMillis) {
            outputFormat.setPilotDurationMillis(pilotDurationMillis);
            return this;
        }

        public Builder withZeroDurationTStates(int zeroDurationTStates) {
            outputFormat.setZeroDurationTStates(zeroDurationTStates);
            return this;
        }

        public Builder withOneDurationTStates(int oneDurationTStates) {
            outputFormat.setOneDurationTStates(oneDurationTStates);
            return this;
        }

        public Builder withReversePhase(boolean reversePhase) {
            outputFormat.setReversePhase(reversePhase);
            return this;
        }

        public Builder withLowValue(int lowValue) {
            outputFormat.setLowValue(lowValue);
            return this;
        }

        public Builder withHighValue(int highValue) {
            outputFormat.setHighValue(highValue);
            return this;
        }

        public Builder withLeadOutDurationMillis(int leadOutDurationMillis) {
            outputFormat.setLeadOutDurationMillis(leadOutDurationMillis);
            return this;
        }

        public StandardWavOutputFormat build() {
            return outputFormat;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public StandardWavOutputFormat(int sampleRate, ChannelType channelType,
                                   int pilotDurationMillis) {
        this.sampleRate = sampleRate;
        this.channelType = channelType;
        this.pilotDurationMillis = pilotDurationMillis;
    }

    public StandardWavOutputFormat() {}

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

    public int getPilotDurationMillis() {
        return pilotDurationMillis;
    }

    public void setPilotDurationMillis(int pilotDurationMillis) {
        this.pilotDurationMillis = pilotDurationMillis;
    }

    public int getOneDurationTStates() {
        return oneDurationTStates;
    }

    public void setOneDurationTStates(int oneDurationTStates) {
        this.oneDurationTStates = oneDurationTStates;
    }

    public int getZeroDurationTStates() {
        return zeroDurationTStates;
    }

    public void setZeroDurationTStates(int zeroDurationTStates) {
        this.zeroDurationTStates = zeroDurationTStates;
    }

    public int getLowValue() {
        return lowValue;
    }

    public void setLowValue(int lowValue) {
        this.lowValue = lowValue;
    }

    public int getHighValue() {
        return highValue;
    }

    public void setHighValue(int highValue) {
        this.highValue = highValue;
    }

    public boolean isReversePhase() {
        return reversePhase;
    }

    public void setReversePhase(boolean reversePhase) {
        this.reversePhase = reversePhase;
    }

    public int getLeadOutDurationMillis() {
        return leadOutDurationMillis;
    }

    public void setLeadOutDurationMillis(int leadOutDurationMillis) {
        this.leadOutDurationMillis = leadOutDurationMillis;
    }
}
