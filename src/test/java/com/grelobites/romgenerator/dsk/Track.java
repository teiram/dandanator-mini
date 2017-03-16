package com.grelobites.romgenerator.dsk;

public class Track {
    private TrackInformationBlock trackInformationBlock;
    private byte[][] data;

    public Track(TrackInformationBlock trackInformationBlock, int sectorCount, int sectorSize) {
        this.trackInformationBlock = trackInformationBlock;
        data = new byte[sectorCount][sectorSize];
    }

    public void setSectorData(int sector, byte[] data) {
        this.data[sector] = data;
    }

    public byte[] getSectorData(int sector) {
        return this.data[sector];
    }

    public TrackInformationBlock getInformation() {
        return trackInformationBlock;
    }
}
