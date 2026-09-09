package id.nusantara.cctv.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs
import kotlin.math.max

data class MapCameraItem(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val status: String,
)

data class ClusterGroup(
    val items: List<MapCameraItem>,
    val centerLat: Double,
    val centerLng: Double,
) {
    val isCluster: Boolean get() = items.size > 1
}

/**
 * Clustering grid screen-space sederhana (§12/§14): proyeksikan semua kamera ke pixel,
 * kelompokkan per sel [cellPx]. Sel berisi >1 kamera jadi cluster dengan count.
 * Rebuild saat zoom/pan (MapListener), bukan tiap frame.
 */
class CameraClusterer(
    private val cellPx: Int = 90,
) {
    fun cluster(items: List<MapCameraItem>, projection: Projection, cellPxOverride: Int? = null): List<ClusterGroup> {
        if (items.isEmpty()) return emptyList()
        val cell = cellPxOverride ?: cellPx
        val grid = HashMap<Pair<Int, Int>, MutableList<MapCameraItem>>()
        for (item in items) {
            val p = projection.toPixels(GeoPoint(item.lat, item.lng), null)
            val key = (p.x / cell) to (p.y / cell)
            grid.getOrPut(key) { mutableListOf() }.add(item)
        }
        return grid.values.map { group ->
            val center = group.fold(0.0 to 0.0) { acc, it ->
                (acc.first + it.lat / group.size) to (acc.second + it.lng / group.size)
            }
            ClusterGroup(group, center.first, center.second)
        }
    }
}

object MarkerIcons {

    /** Drawable dengan bounds eksplisit — tanpa ini marker tidak tergambar osmdroid. */
    private fun bounded(drawable: BitmapDrawable): BitmapDrawable {
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        return drawable
    }

    fun dot(status: String, sizePx: Int = 40): BitmapDrawable {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val fill = when (status) {
            "ONLINE" -> Color.rgb(46, 204, 113)
            "OFFLINE", "TIMEOUT", "INVALID_STREAM" -> Color.rgb(231, 76, 60)
            "AUTH_REQUIRED" -> Color.rgb(241, 196, 15)
            else -> Color.rgb(149, 165, 166)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            style = Paint.Style.FILL
        }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.6f, paint)
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sizePx / 10f
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.6f, paint)
        return bounded(BitmapDrawable(null, bmp))
    }

    fun cluster(count: Int, sizePx: Int = 52): BitmapDrawable {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(41, 128, 185) }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2.4f, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = sizePx / 2.4f
        val y = sizePx / 2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(if (count > 999) "${count / 1000}k" else count.toString(), sizePx / 2f, y, paint)
        return bounded(BitmapDrawable(null, bmp))
    }
}

/** Re-render overlay marker saat zoom/pan selesai. */
fun attachClusterListener(mapView: MapView, rebuild: () -> Unit) {
    mapView.addMapListener(object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            rebuild()
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            rebuild()
            return true
        }
    })
}

/** Zoom agar seluruh [items] terlihat; center Indonesia bila kosong. */
fun fitBounds(mapView: MapView, items: List<MapCameraItem>) {
    if (items.isEmpty()) {
        mapView.controller.setZoom(4.8)
        mapView.controller.setCenter(GeoPoint(-2.5, 118.0))
        return
    }
    val north = max(items.maxOf { it.lat }, -11.0)
    val south = items.minOf { it.lat }
    val east = items.maxOf { it.lng }
    val west = items.minOf { it.lng }
    if (abs(north - south) < 0.05 && abs(east - west) < 0.05) {
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(GeoPoint(north, east))
        return
    }
    mapView.zoomToBoundingBox(BoundingBox(north, east, south, west), false, 64)
    // bounding box seluruh Indonesia bisa menghasilkan zoom terlalu jauh; jaga minimal
    if (mapView.zoomLevelDouble < 4.8) mapView.controller.setZoom(4.8)
}
