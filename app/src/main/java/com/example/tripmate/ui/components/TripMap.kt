package com.example.tripmate.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.tripmate.model.AssistantItineraryPin
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Free OpenStreetMap-based vector style from OpenFreeMap: no API key, no account, no billing.
 * Map data (c) OpenStreetMap contributors, tiles by OpenFreeMap / OpenMapTiles.
 */
private const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private const val SRC_PINS = "trip-pins-source"
private const val SRC_ROUTE = "trip-route-source"
private const val LAYER_PINS = "trip-pins-layer"
private const val LAYER_ROUTE = "trip-route-layer"

/** One colour per itinerary day (cycled), so pins of the same day read as a group. */
private val DayColors = listOf(
    0xFFEA580C, 0xFF2563EB, 0xFF16A34A, 0xFF9333EA,
    0xFFDB2777, 0xFF0891B2, 0xFFCA8A04, 0xFF475569
)

fun dayColorFor(dayNumber: Int): Color = Color(DayColors[(dayNumber - 1).coerceAtLeast(0) % DayColors.size])

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private fun Color.toArgb(): Int = android.graphics.Color.argb(
    (alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt()
)

private fun iconName(order: Int, colorHex: String, selected: Boolean) = "pin-$order-$colorHex-$selected"

/** Draws a numbered, day-coloured circular marker. Selected markers are larger with a dark ring. */
private fun numberedMarkerBitmap(context: Context, number: Int, color: Color, selected: Boolean): Bitmap {
    val density = context.resources.displayMetrics.density
    val sizePx = ((if (selected) 44 else 34) * density).toInt()
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val c = sizePx / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val ring = (if (selected) 4f else 2.5f) * density
    paint.color = if (selected) android.graphics.Color.parseColor("#0F172A") else android.graphics.Color.WHITE
    canvas.drawCircle(c, c, c, paint)
    paint.color = color.toArgb()
    canvas.drawCircle(c, c, c - ring, paint)
    paint.color = android.graphics.Color.WHITE
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    paint.textSize = (if (selected) 17 else 14) * density
    canvas.drawText(number.toString(), c, c - (paint.descent() + paint.ascent()) / 2, paint)
    return bitmap
}

/**
 * Interactive MapLibre map (OpenStreetMap data) rendered inside Compose.
 *
 * - One numbered marker per itinerary place that has real coordinates, coloured by day.
 * - The camera automatically fits all pins whenever the set of resolved pins changes.
 * - [selectedKey] focuses/highlights a pin (itinerary -> map); tapping a marker calls
 *   [onPinSelected] (map -> itinerary).
 * - The user's own location is a separate blue dot + "My location" button, only with permission.
 */
@SuppressLint("MissingPermission")
@Composable
fun TripMap(
    pins: List<AssistantItineraryPin>,
    selectedKey: String?,
    onPinSelected: (AssistantItineraryPin) -> Unit,
    modifier: Modifier = Modifier,
    focusToken: Int = 0,
    isResolving: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    val mapped = remember(pins) { pins.filter { it.hasCoordinates } }
    val currentMapped by rememberUpdatedState(mapped)
    val currentOnPinSelected by rememberUpdatedState(onPinSelected)

    fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    var locationEnabled by remember { mutableStateOf(false) }

    /** Shows the user's own position as a blue dot (separate from itinerary markers). */
    fun enableLocation(): Boolean {
        val m = map ?: return false
        val s = style ?: return false
        if (!hasLocationPermission()) return false
        val lc = m.locationComponent
        if (!lc.isLocationComponentActivated) {
            lc.activateLocationComponent(
                LocationComponentActivationOptions.builder(context, s).useDefaultLocationEngine(true).build()
            )
        }
        lc.isLocationComponentEnabled = true
        lc.renderMode = RenderMode.NORMAL
        locationEnabled = true
        return true
    }

    fun centerOnMe() {
        val m = map ?: return
        if (!enableLocation()) return
        val last = m.locationComponent.lastKnownLocation
        if (last != null) {
            m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(last.latitude, last.longitude), 15.0), 700)
        } else {
            // No fix yet: follow the user once the first location arrives (panning cancels tracking).
            m.locationComponent.cameraMode = CameraMode.TRACKING
            Toast.makeText(context, "Finding your location…", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) centerOnMe()
        else Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
    }

    fun fitAll(animate: Boolean) {
        val m = map ?: return
        val list = mapped
        if (list.isEmpty()) return
        val distinctPoints = list.map { LatLng(it.latitude!!, it.longitude!!) }.distinctBy { it.latitude to it.longitude }
        val update = if (distinctPoints.size <= 1) {
            CameraUpdateFactory.newLatLngZoom(LatLng(list[0].latitude!!, list[0].longitude!!), 14.0)
        } else {
            val bounds = LatLngBounds.Builder().includes(distinctPoints).build()
            val pad = (48 * context.resources.displayMetrics.density).toInt()
            CameraUpdateFactory.newLatLngBounds(bounds, pad, pad, pad, pad)
        }
        if (animate) m.animateCamera(update, 700) else m.moveCamera(update)
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).also { mv ->
            mv.onCreate(null)
            // Keep pan/zoom gestures working inside scrolling parents (LazyColumn, etc.)
            mv.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> v.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            mv.addOnDidFailLoadingMapListener { loadFailed = true }
            mv.addOnDidFinishLoadingStyleListener { loadFailed = false }
            mv.getMapAsync { m ->
                map = m
                m.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { st ->
                    st.addSource(GeoJsonSource(SRC_ROUTE))
                    st.addSource(GeoJsonSource(SRC_PINS))
                    st.addLayer(
                        LineLayer(LAYER_ROUTE, SRC_ROUTE).withProperties(
                            PropertyFactory.lineColor(Expression.get("color")),
                            PropertyFactory.lineWidth(4f),
                            PropertyFactory.lineOpacity(0.6f),
                            PropertyFactory.lineDasharray(arrayOf(2f, 2f))
                        )
                    )
                    st.addLayer(
                        SymbolLayer(LAYER_PINS, SRC_PINS).withProperties(
                            PropertyFactory.iconImage(Expression.get("icon")),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.textField(Expression.get("label")),
                            PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                            PropertyFactory.textSize(12f),
                            PropertyFactory.textOffset(arrayOf(0f, 2f)),
                            PropertyFactory.textAllowOverlap(true),
                            PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
                            PropertyFactory.textHaloWidth(1.6f)
                        )
                    )
                    style = st
                }
                // Map -> itinerary: tapping a marker selects that place.
                m.addOnMapClickListener { latLng ->
                    val screen = m.projection.toScreenLocation(latLng)
                    val hit = m.queryRenderedFeatures(screen, LAYER_PINS).firstOrNull()
                    val key = hit?.getStringProperty("key")
                    val pin = currentMapped.firstOrNull { it.key == key }
                    if (pin != null) {
                        currentOnPinSelected(pin)
                        true
                    } else false
                }
            }
        }
    }

    // MapView must follow the host lifecycle.
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            runCatching {
                when (event) {
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> {}
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Note: Do NOT call mapView.onDestroy() here.
            // In a LazyColumn, scrolling off-screen disposes the composable.
            // Calling onDestroy() here destroys the native MapLibre engine while
            // the view is being scrolled/detached, resulting in a SIGSEGV / crash.
            runCatching { mapView.onPause() }
        }
    }

    // Show the blue "you are here" dot automatically when permission was already granted.
    LaunchedEffect(style) {
        if (style != null && hasLocationPermission()) enableLocation()
    }

    // Push itinerary places (markers, labels, per-day route lines) into the map.
    LaunchedEffect(style, mapped, selectedKey) {
        val st = style ?: return@LaunchedEffect
        val features = mapped.map { pin ->
            val selected = pin.key == selectedKey
            val color = dayColorFor(pin.dayNumber)
            val name = iconName(pin.order, color.toHex(), selected)
            if (st.getImage(name) == null) {
                st.addImage(name, numberedMarkerBitmap(context, pin.order, color, selected))
            }
            Feature.fromGeometry(Point.fromLngLat(pin.longitude!!, pin.latitude!!)).also {
                it.addStringProperty("key", pin.key)
                it.addStringProperty("icon", name)
                it.addStringProperty("label", if (selected) pin.title else "")
            }
        }
        st.getSourceAs<GeoJsonSource>(SRC_PINS)?.setGeoJson(FeatureCollection.fromFeatures(features))

        val lines = mapped.groupBy { it.dayNumber }.filter { it.value.size > 1 }.map { (day, dayPins) ->
            val line = LineString.fromLngLats(dayPins.sortedBy { it.order }.map { Point.fromLngLat(it.longitude!!, it.latitude!!) })
            Feature.fromGeometry(line).also { it.addStringProperty("color", dayColorFor(day).toHex()) }
        }
        st.getSourceAs<GeoJsonSource>(SRC_ROUTE)?.setGeoJson(FeatureCollection.fromFeatures(lines))
    }

    // Auto-fit the camera whenever the resolved itinerary places change.
    val coordsSignature = remember(mapped) { mapped.map { it.latitude to it.longitude } }
    LaunchedEffect(style, coordsSignature) {
        if (style != null) fitAll(animate = true)
    }

    // Itinerary -> map: focus the selected place.
    LaunchedEffect(selectedKey, focusToken, style) {
        val m = map ?: return@LaunchedEffect
        if (style == null) return@LaunchedEffect
        val pin = mapped.firstOrNull { it.key == selectedKey } ?: return@LaunchedEffect
        m.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(pin.latitude!!, pin.longitude!!), maxOf(m.cameraPosition.zoom, 14.0)),
            600
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                (mapView.parent as? android.view.ViewGroup)?.removeView(mapView)
                mapView
            },
            modifier = Modifier.matchParentSize()
        )

        if (loadFailed && style == null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFFEF2F2)
            ) {
                Text(
                    text = "Map couldn't load — check your internet connection",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = Color(0xFFB91C1C)
                )
            }
        }

        if (isResolving) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.92f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                    Text("Locating places…", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF475569))
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MapRoundButton(onClick = { fitAll(animate = true) }) {
                Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Show all places", tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
            }
            MapRoundButton(onClick = {
                if (hasLocationPermission()) centerOnMe()
                else permissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "My location", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MapRoundButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(shape = CircleShape, color = Color.White, shadowElevation = 4.dp, modifier = Modifier.size(40.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) { content() }
    }
}
