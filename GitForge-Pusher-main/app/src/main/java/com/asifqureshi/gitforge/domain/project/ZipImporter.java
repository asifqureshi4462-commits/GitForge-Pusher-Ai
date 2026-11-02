package com.asifqureshi.gitforge.domain.project;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/** Streams a ZIP archive into a {@link StagingArea}; never trusts entry names or declared sizes. */
public final class ZipImporter {

    public static final class InvalidArchiveException extends Exception {
        public InvalidArchiveException(String message) {
            super(message);
        }
    }

    private ZipImporter() {
    }

    public static void importZip(InputStream raw, StagingArea area)
            throws IOException, StagingArea.StagingLimitException, InvalidArchiveException {
        int entries = 0;
        try (ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries++;
                if (entry.isDirectory()) {
                    continue;
                }
                area.add(entry.getName(), zin);
                zin.closeEntry();
            }
        } catch (ZipException e) {
            throw new InvalidArchiveException("The file is not a readable ZIP archive (it may be corrupt or encrypted).");
        }
        if (entries == 0) {
            throw new InvalidArchiveException("The file is not a ZIP archive, or the archive is empty.");
        }
    }
}
