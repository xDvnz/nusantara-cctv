package id.nusantara.cctv

import id.nusantara.cctv.data.update.VersionCompare
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

    @Test
    fun `versi remote lebih tinggi terdeteksi`() {
        assertTrue(VersionCompare.isRemoteNewer("1.1", "1.0"))
        assertTrue(VersionCompare.isRemoteNewer("v1.2", "1.1"))
        assertTrue(VersionCompare.isRemoteNewer("2.0", "1.9"))
        assertTrue(VersionCompare.isRemoteNewer("1.10", "1.9")) // numerik, bukan leksikografis
        assertTrue(VersionCompare.isRemoteNewer("1.1.1", "1.1"))
    }

    @Test
    fun `versi sama atau lebih rendah tidak dianggap update`() {
        assertFalse(VersionCompare.isRemoteNewer("1.0", "1.0"))
        assertFalse(VersionCompare.isRemoteNewer("v1.0", "1.0"))
        assertFalse(VersionCompare.isRemoteNewer("1.0", "1.1"))
        assertFalse(VersionCompare.isRemoteNewer("", "1.0"))
    }

    @Test
    fun `format aneh tidak melempar exception`() {
        assertFalse(VersionCompare.isRemoteNewer("abc", "1.0"))
        assertTrue(VersionCompare.isRemoteNewer("1.0-beta", "0.9"))
        assertFalse(VersionCompare.isRemoteNewer("x.y.z", "1.1"))
    }
}
