package com.grelobites.romgenerator.dsk;

import com.grelobites.romgenerator.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DskContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(DskContainer.class);
    public DiskInformationBlock diskInformationBlock;
    public Track[] tracks;

    private DskContainer(DiskInformationBlock diskInformationBlock,
                         Track[] tracks) {
        this.diskInformationBlock = diskInformationBlock;
        this.tracks = tracks;
    }

    public static DskContainer fromInputStream(InputStream data) throws IOException {
        DiskInformationBlock diskInformationBlock = DiskInformationBlock.fromInputStream(data);
        LOGGER.debug("Disk Information block: " + diskInformationBlock);
        Track[] tracks = new Track[diskInformationBlock.trackCount];
        for (int i = 0; i < diskInformationBlock.trackCount; i++) {
            TrackInformationBlock trackInformationBlock = TrackInformationBlock.fromInputStream(data);
            Track track = new Track(trackInformationBlock,
                    trackInformationBlock.sectorCount, trackInformationBlock.sectorSize);
            for (int j = 0; j < trackInformationBlock.sectorCount; j++) {
                track.setSectorData(j, Util.fromInputStream(data, trackInformationBlock.sectorSize));
            }
            tracks[i] = track;
        }
        return new DskContainer(diskInformationBlock, tracks);
    }

    public void dumpRawData(OutputStream os) throws IOException {
        for (Track track : tracks) {
            LOGGER.debug("Dumping information for track " + track.getInformation());
            for (int i = 0; i < track.getInformation().sectorCount; i++) {
                LOGGER.debug("Dumping data for sector " + track.getInformation().sectorInformationList[i]);
                os.write(track.getSectorData(i));
            }
        }
    }
}
