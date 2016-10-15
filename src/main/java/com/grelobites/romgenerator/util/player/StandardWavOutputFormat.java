package com.grelobites.romgenerator.util.player;

public class StandardWavOutputFormat {
    public static final int SRATE_44100 = 44100;
    public static final int SRATE_44800 = 44800;

    private static final int ZERO_DURATON_STANDARD = 10;
    private static final int ONE_DURATION_STANDARD = 10;

    private static final int ZERO_DURATON_TURBO_1 = 10;
    private static final int ONE_DURATION_TURBO_1 = 10;

    private static final int ZERO_DURATON_TURBO_2 = 10;
    private static final int ONE_DURATION_TURBO_2 = 10;

    private int sampleRate;
    private ChannelType channelType;
    private int pilotDurationMillis;
    private int oneDurationTStates;
    private int zeroDurationTStates;


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

}
