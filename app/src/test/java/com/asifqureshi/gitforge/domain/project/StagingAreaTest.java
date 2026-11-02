package com.asifqureshi.gitforge.domain.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;

public class StagingAreaTest {

    private static StagingArea newArea() throws Exception {
        return new StagingArea(Files.createTempDirectory("staging").toFile());
    }

    private static ByteArrayInputStream bytes(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] zip(String[][] entries) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(bos)) {
            for (String[] e : entries) {
                z.putNextEntry(new ZipEntry(e[0]));
                z.write(e[1].getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
        }
        return bos.toByteArray();
    }

    @Test
    public void addsFilesAndRecordsSkips() throws Exception {
        StagingArea area = newArea();
        area.add("src/Main.java", bytes("class Main {}"));
        area.add("node_modules/x/index.js", bytes("x"));
        area.add(".DS_Store", bytes("x"));
        area.add("src/Main.java", bytes("again"));
        assertEquals(1, area.getFiles().size());
        assertEquals(3, area.getSkipped().size());
        assertEquals(SkipReason.IGNORED, area.getSkipped().get(0).getReason());
        assertEquals(SkipReason.DUPLICATE, area.getSkipped().get(2).getReason());
        area.deleteAll();
    }

    @Test
    public void zipSlipEntriesAreSkippedAndNothingIsWrittenOutside() throws Exception {
        File parent = Files.createTempDirectory("outer").toFile();
        File root = new File(parent, "staging");
        StagingArea area = new StagingArea(root);
        byte[] z = zip(new String[][]{{"../evil.txt", "pwn"}, {"ok/a.txt", "fine"}, {"/abs/b.txt", "x"}});
        ZipImporter.importZip(new ByteArrayInputStream(z), area);
        assertFalse(new File(parent, "evil.txt").exists());
        assertEquals(1, countSkipped(area, SkipReason.UNSAFE_PATH));
        assertEquals(2, area.getFiles().size());
        assertTrue(area.getFiles().get(0).getFile().getCanonicalPath().startsWith(root.getCanonicalPath()));
        area.deleteAll();
    }

    private static int countSkipped(StagingArea area, SkipReason r) {
        int n = 0;
        for (SkippedEntry s : area.getSkipped()) {
            if (s.getReason() == r) {
                n++;
            }
        }
        return n;
    }

    @Test
    public void oversizedFileIsSkippedNotStored() throws Exception {
        StagingArea area = newArea();
        byte[] big = new byte[(int) StagingArea.MAX_FILE_BYTES + 1];
        area.add("big.bin", new ByteArrayInputStream(big));
        assertEquals(0, area.getFiles().size());
        assertEquals(SkipReason.TOO_LARGE, area.getSkipped().get(0).getReason());
        assertFalse(new File(area.getRoot(), "big.bin").exists());
        area.deleteAll();
    }

    @Test
    public void invalidArchiveIsRejected() throws Exception {
        StagingArea area = newArea();
        assertThrows(ZipImporter.InvalidArchiveException.class,
                () -> ZipImporter.importZip(bytes("this is not a zip"), area));
        area.deleteAll();
    }

    @Test
    public void commonRootCanBeStripped() throws Exception {
        StagingArea area = newArea();
        byte[] z = zip(new String[][]{{"proj-main/README.md", "r"}, {"proj-main/src/A.java", "a"}});
        ZipImporter.importZip(new ByteArrayInputStream(z), area);
        area.finish();
        assertEquals("proj-main", area.commonRoot());
        area.setStripCommonRoot(true);
        assertEquals("README.md", area.getFiles().get(0).getPath());
        assertEquals("src/A.java", area.getFiles().get(1).getPath());
        area.setStripCommonRoot(false);
        assertEquals("proj-main/README.md", area.getFiles().get(0).getPath());
        area.deleteAll();
    }

    @Test
    public void noCommonRootWhenMixed() throws Exception {
        StagingArea area = newArea();
        area.add("a/x.txt", bytes("x"));
        area.add("b/y.txt", bytes("y"));
        assertNull(area.commonRoot());
        area.deleteAll();
    }

    @Test
    public void finishHashesAndUnselectsSecrets() throws Exception {
        StagingArea area = newArea();
        area.add("hello.txt", bytes("hello\n"));
        area.add(".env", bytes("A=1"));
        area.finish();
        ProjectFile hello = null;
        ProjectFile env = null;
        for (ProjectFile f : area.getFiles()) {
            if (f.getPath().equals("hello.txt")) {
                hello = f;
            } else {
                env = f;
            }
        }
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", hello.getBlobSha());
        assertTrue(hello.isSelected());
        assertTrue(env.hasSecretFindings());
        assertFalse(env.isSelected());
        area.deleteAll();
    }

    @Test
    public void tooManyFilesAbortsImport() throws Exception {
        StagingArea area = newArea();
        for (int i = 0; i < StagingArea.MAX_FILES; i++) {
            area.add("f" + i + ".txt", bytes("x"));
        }
        assertThrows(StagingArea.StagingLimitException.class, () -> area.add("one-more.txt", bytes("x")));
        area.deleteAll();
    }
}
