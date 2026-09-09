package id.nusantara.cctv

import android.graphics.BitmapFactory
import id.nusantara.cctv.data.player.MjpegDecoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MjpegDecoderTest {

    /** JPEG 1x1 merah — frame sintetis valid. */
    private fun tinyJpeg(): ByteArray {
        val bmp = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixel(0, 0, android.graphics.Color.RED)
        val out = ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, out)
        return out.toByteArray()
    }

    private fun multipart(jpegs: List<ByteArray>, boundary: String = "mjpegframe"): ByteArray {
        val body = ByteArrayOutputStream()
        jpegs.forEach { frame ->
            body.write(("--$boundary\r\n").toByteArray())
            body.write("Content-Type: image/jpeg\r\n\r\n".toByteArray())
            body.write(frame)
            body.write("\r\n".toByteArray())
        }
        body.write(("--$boundary--\r\n").toByteArray())
        return body.toByteArray()
    }

    @Test
    fun `dua frame berurutan terbaca`() {
        val jpeg = tinyJpeg()
        val decoder = MjpegDecoder(ByteArrayInputStream(multipart(listOf(jpeg, jpeg))))
        val frame1 = decoder.nextFrame()
        val frame2 = decoder.nextFrame()
        assertNotNull(frame1)
        assertNotNull(frame2)
        assertEquals(1, frame1!!.width)
        assertEquals(1, frame2!!.height)
        decoder.close()
    }

    @Test
    fun `stream tanpa jpeg start menghasilkan null`() {
        val decoder = MjpegDecoder(ByteArrayInputStream(ByteArray(64) { 0x41 })) // "AAAA..."
        assertNull(decoder.nextFrame())
    }

    @Test
    fun `stream kosong aman`() {
        val decoder = MjpegDecoder(ByteArrayInputStream(ByteArray(0)))
        assertNull(decoder.nextFrame())
    }

    @Test
    fun `frame korup tanpa EOI keluar sebagai null bukan crash`() {
        val jpeg = tinyJpeg()
        val corrupt = jpeg.copyOfRange(0, jpeg.size - 2) // buang EOI
        val decoder = MjpegDecoder(ByteArrayInputStream(multipart(listOf(corrupt))))
        // frame utuh dikirim tanpa EOI -> tidak pernah selesai -> null di EOF, tidak exception
        val result = decoder.nextFrame()
        assertNull(result)
    }

    @Test
    fun `sisa byte antar frame tidak hilang untuk frame berikutnya`() {
        val jpeg = tinyJpeg()
        val decoder = MjpegDecoder(ByteArrayInputStream(multipart(listOf(jpeg, jpeg))))
        assertNotNull(decoder.nextFrame())
        assertNotNull(decoder.nextFrame())
        assertNull(decoder.nextFrame()) // stream habis
    }
}
