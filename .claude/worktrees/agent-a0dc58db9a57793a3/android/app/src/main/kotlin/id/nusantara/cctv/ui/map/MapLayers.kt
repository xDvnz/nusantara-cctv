package id.nusantara.cctv.ui.map

import id.nusantara.cctv.R
import org.osmdroid.tileprovider.tilesource.ITileSource
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.MapTileIndex

/** Sumber tile peta yang bisa dipilih user (persist di preferensi). */
enum class MapLayer(val labelRes: Int) {
    DEFAULT(R.string.map_layer_default),
    SATELLITE(R.string.map_layer_satellite),
    DARK(R.string.map_layer_dark),
    TERRAIN(R.string.map_layer_terrain),
}

/**
 * Tile source kustom. URL dibangun manual dari indeks tile (z/x/y atau z/y/x)
 * agar cocok dengan konvensi tiap penyedia — pola default osmdroid tidak selalu pas.
 */
private class UrlTileSource(
    name: String,
    private val base: String,
    private val extension: String,
    private val zyx: Boolean,
) : XYTileSource(name, 1, 19, 256, extension, arrayOf(base)) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val z = MapTileIndex.getZoom(pMapTileIndex)
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        return if (zyx) "$base$z/$y/$x$extension" else "$base$z/$x/$y$extension"
    }
}

object MapLayers {

    // Instance tunggal: perbandingan `map.tileSource === sumber(layer)` valid,
    // dan osmdroid tidak me-reload cache tiap pemanggilan.
    private val SATELLITE_SOURCE = UrlTileSource(
        name = "EsriWorldImagery",
        base = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/",
        extension = ".jpg",
        zyx = true,
    )
    private val DARK_SOURCE = UrlTileSource(
        name = "CartoDark",
        base = "https://a.basemaps.cartocdn.com/dark_all/",
        extension = ".png",
        zyx = false,
    )
    private val TERRAIN_SOURCE = UrlTileSource(
        name = "OpenTopoMap",
        base = "https://a.tile.opentopomap.org/",
        extension = ".png",
        zyx = false,
    )

    fun tileSource(layer: MapLayer): ITileSource = when (layer) {
        MapLayer.DEFAULT -> TileSourceFactory.MAPNIK
        // Esri World Imagery — urutan path z/y/x
        MapLayer.SATELLITE -> SATELLITE_SOURCE
        // CartoDB dark matter — cocok untuk mode monitoring malam
        MapLayer.DARK -> DARK_SOURCE
        MapLayer.TERRAIN -> TERRAIN_SOURCE
    }
}
