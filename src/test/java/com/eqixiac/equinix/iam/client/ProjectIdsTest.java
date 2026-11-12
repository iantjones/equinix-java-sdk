package com.eqixiac.equinix.iam.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectIdsTest {

    @Test
    void convertsBetweenFormats() {
        assertEquals("project:4b0d1c8a-1111", ProjectIds.of("4b0d1c8a-1111"));
        assertEquals("project:4b0d1c8a-1111", ProjectIds.of(" project:4b0d1c8a-1111 "));
        assertEquals("4b0d1c8a-1111", ProjectIds.bare("project:4b0d1c8a-1111"));
        assertEquals("4b0d1c8a-1111", ProjectIds.bare("4b0d1c8a-1111"));
        assertThrows(IllegalArgumentException.class, () -> ProjectIds.of(" "));
    }
}
