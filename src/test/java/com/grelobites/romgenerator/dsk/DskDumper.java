package com.grelobites.romgenerator.dsk;

import org.junit.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

public class DskDumper {

    @Test
    public void dumpDskToRaw() throws IOException {
        InputStream dskResource = DskDumper.class.getResourceAsStream("/dsk/dan.dsk");
        assertNotNull(dskResource);

        DskContainer.fromInputStream(dskResource)
                .dumpRawData(new FileOutputStream("/var/tmp/dsk.raw"));
    }
}
