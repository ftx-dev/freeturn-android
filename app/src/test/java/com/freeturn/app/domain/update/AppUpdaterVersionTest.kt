package com.freeturn.app.domain.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterVersionTest {

    @Test
    fun ourVersionWinsOverParentVersion() {
        assertTrue(AppUpdater.isNewer("0.2-5.3.4", "0.1-5.3.4"))
        assertTrue(AppUpdater.isNewer("0.2-5.3.4", "0.1-5.4.0"))
        assertFalse(AppUpdater.isNewer("0.1-5.4.0", "0.2-5.3.4"))
        assertTrue(AppUpdater.isNewer("1.0-5.3.4", "0.10-5.3.4"))
        assertTrue(AppUpdater.isNewer("0.10-5.3.4", "0.9-5.3.4"))
    }

    @Test
    fun parentVersionDecidesWithinSameOwnVersion() {
        assertTrue(AppUpdater.isNewer("0.1-5.4.3", "0.1-5.3.4"))
        assertTrue(AppUpdater.isNewer("0.1-5.3.10", "0.1-5.3.9"))
        assertFalse(AppUpdater.isNewer("0.1-5.3.4", "0.1-5.3.4"))
        assertFalse(AppUpdater.isNewer("0.1-5.3.3", "0.1-5.3.4"))
    }

    @Test
    fun suffixesAndTagPrefixAreIgnored() {
        assertFalse(AppUpdater.isNewer("v0.1-5.3.4", "0.1-5.3.4-debug"))
        assertTrue(AppUpdater.isNewer("v0.1-5.3.5", "0.1-5.3.4-debug"))
    }
}
