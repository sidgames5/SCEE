package de.westnordost.streetcomplete.screens.main.map

import android.graphics.RectF
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import de.westnordost.streetcomplete.data.download.tiles.TilesRect
import de.westnordost.streetcomplete.data.download.tiles.enclosingTilesRect
import de.westnordost.streetcomplete.data.osm.edits.MapDataWithEditsSource
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.layers.Layer
import de.westnordost.streetcomplete.screens.main.map.components.StyleableLayerMapComponent
import de.westnordost.streetcomplete.screens.main.map.components.StyledElement
import de.westnordost.streetcomplete.screens.main.map.tangram.KtMapController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Manages the layer of styled map data in the map view:
 *  Gets told by the QuestsMapFragment when a new area is in view and independently pulls the map
 *  data for the bbox surrounding the area from database and holds it in memory. */
class StyleableLayerManager(
    private val ctrl: KtMapController,
    private val mapComponent: StyleableLayerMapComponent,
    private val mapDataSource: MapDataWithEditsSource
) : DefaultLifecycleObserver {

    // last displayed rect of (zoom 16) tiles
    private var lastDisplayedRect: TilesRect? = null
    // map data in current view: key -> [pin, ...]
    private val mapDataInView: MutableMap<ElementKey, StyledElement> = mutableMapOf()

    private val viewLifecycleScope: CoroutineScope = CoroutineScope(SupervisorJob())

    /** The layer to display */
    var layer: Layer? = null
        set(value) {
            field = value
            if (field == value) return
            field = value
            if (value != null) start() else stop()
        }

    private val mapDataListener = object : MapDataWithEditsSource.Listener {
        override fun onUpdated(updated: MapDataWithGeometry, deleted: Collection<ElementKey>) {
            updateStyledElements(updated, deleted)
        }

        override fun onReplacedForBBox(bbox: BoundingBox, mapDataWithGeometry: MapDataWithGeometry) {
            clear()
            onNewScreenPosition()
        }

        override fun onCleared() {
            clear()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
        viewLifecycleScope.cancel()
    }

    private fun start() {
        onNewScreenPosition()
        mapDataSource.addListener(mapDataListener)
    }

    private fun stop() {
        clear()
        viewLifecycleScope.coroutineContext.cancelChildren()
        mapDataSource.removeListener(mapDataListener)
    }

    private fun clear() {
        synchronized(mapDataInView) { mapDataInView.clear() }
        lastDisplayedRect = null
        mapComponent.clear()
    }

    fun onNewScreenPosition() {
        if (layer == null) return
        val zoom = ctrl.cameraPosition.zoom
        if (zoom < TILES_ZOOM) return
        val displayedArea = ctrl.screenAreaToBoundingBox(RectF()) ?: return
        val tilesRect = displayedArea.enclosingTilesRect(TILES_ZOOM)
        // area too big -> skip (performance)
        if (tilesRect.size > 16) return
        if (lastDisplayedRect?.contains(tilesRect) != true) {
            lastDisplayedRect = tilesRect
            onNewTilesRect(tilesRect)
        }
    }

    private fun onNewTilesRect(tilesRect: TilesRect) {
        val bbox = tilesRect.asBoundingBox(TILES_ZOOM)
        viewLifecycleScope.launch {
            val mapData = withContext(Dispatchers.IO) { mapDataSource.getMapDataWithGeometry(bbox) }
            setStyledElements(mapData)
        }
    }

    private fun setStyledElements(mapData: MapDataWithGeometry) {
        val layer = layer ?: return
        synchronized(mapDataInView) {
            mapDataInView.clear()
            mapData.forEach { element ->
                val styledElement = createStyledElement(layer, mapData, element)
                if (styledElement != null) {
                    val key = ElementKey(element.type, element.id)
                    mapDataInView[key] = styledElement
                }
            }
            mapComponent.set(mapDataInView.values)
        }
    }

    private fun updateStyledElements(updated: MapDataWithGeometry, deleted: Collection<ElementKey>) {
        val layer = layer ?: return
        synchronized(mapDataInView) {
            updated.forEach { element ->
                val styledElement = createStyledElement(layer, updated, element)
                val key = ElementKey(element.type, element.id)
                if (styledElement != null) {
                    mapDataInView[key] = styledElement
                } else {
                    mapDataInView.remove(key)
                }
            }
            deleted.forEach { mapDataInView.remove(it) }
            mapComponent.set(mapDataInView.values)
        }
    }

    private fun createStyledElement(layer: Layer, mapData: MapDataWithGeometry, element: Element): StyledElement? {
        if (!layer.isDisplayed(element)) return null
        val geometry = mapData.getGeometry(element.type, element.id) ?: return null
        val style = layer.getStyle(element)
        return StyledElement(element, geometry, style)
    }

    companion object {
        private const val TILES_ZOOM = 16
    }
}
