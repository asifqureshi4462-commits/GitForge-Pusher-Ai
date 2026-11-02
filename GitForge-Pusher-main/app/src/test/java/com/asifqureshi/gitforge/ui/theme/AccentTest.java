package com.asifqureshi.gitforge.ui.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class AccentTest {

    @Test
    public void fromId_null_returnsDefault() {
        assertEquals(Accent.DEFAULT, Accent.fromId(null));
    }

    @Test
    public void fromId_unknown_returnsDefault() {
        assertEquals(Accent.DEFAULT, Accent.fromId("not-a-colour"));
    }

    @Test
    public void fromId_roundTripsEveryAccent() {
        for (Accent accent : Accent.values()) {
            assertEquals(accent, Accent.fromId(accent.getId()));
        }
    }

    @Test
    public void ids_areUnique() {
        Set<String> seen = new HashSet<>();
        for (Accent accent : Accent.values()) {
            assertTrue("duplicate accent id " + accent.getId(), seen.add(accent.getId()));
        }
    }

    @Test
    public void default_isIndigo() {
        assertEquals("indigo", Accent.DEFAULT.getId());
    }
}
