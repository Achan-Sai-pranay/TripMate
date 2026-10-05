package com.example.tripmate.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.tripmate.model.AssistantItineraryPin
import com.example.tripmate.model.AssistantMapRoute
import com.example.tripmate.ui.theme.Dimens
import com.example.tripmate.ui.theme.PrimaryOrange
import com.example.tripmate.ui.theme.SubtleBorder
import com.example.tripmate.ui.theme.TextPrimary
import com.example.tripmate.ui.theme.TextSecondary
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "AssistantMap"
private val MapBlue = Color(0xFF2563EB)

/**
 * Caches inlined Leaflet CSS and JS so they are read from APK assets once.
 * Inlining eliminates file:/// sandbox restrictions and guarantees 100% instant execution.
 */
object LeafletAssetsCache {
    private var cachedCss: String? = null
    private var cachedJs: String? = null

    fun getCss(context: Context): String {
        return cachedCss ?: run {
            val text = try {
                context.assets.open("leaflet/leaflet.css").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading leaflet.css from assets", e)
                ""
            }
            cachedCss = text
            text
        }
    }

    fun getJs(context: Context): String {
        return cachedJs ?: run {
            val text = try {
                context.assets.open("leaflet/leaflet.js").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading leaflet.js from assets", e)
                ""
            }
            cachedJs = text
            text
        }
    }
}

/**
 * Collapsible map panel for the assistant screen.
 *
 * - Collapsed: slim header bar so chat keeps ~85% of screen height.
 * - Expanded: fully interactive map with real pins at coordinates,
 *   touch pan/zoom, auto-framing, and horizontal stop cards.
 */
@Composable
fun InteractiveAssistantMap(
    route: AssistantMapRoute,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    focusPinTitle: String? = null,
    focusRequestId: Int = 0
) {
    val context = LocalContext.current
    var selectedPinIndex by remember(route) { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var mapReady by remember(route) { mutableStateOf(false) }
    val stripState = rememberLazyListState()

    // Sync focused pin from chat response
    LaunchedEffect(focusRequestId, route) {
        val title = focusPinTitle ?: return@LaunchedEffect
        val idx = route.pins.indexOfFirst {
            it.title.equals(title, ignoreCase = true) ||
                it.title.contains(title, ignoreCase = true) ||
                title.contains(it.title, ignoreCase = true)
        }
        if (idx >= 0) selectedPinIndex = idx
    }

    // Keep stop cards strip and map centered on active pin
    LaunchedEffect(selectedPinIndex, mapReady, expanded) {
        if (!expanded) return@LaunchedEffect
        if (selectedPinIndex in route.pins.indices) {
            stripState.animateScrollToItem(selectedPinIndex)
            if (mapReady) {
                webViewRef?.evaluateJavascript("window.focusPin && window.focusPin($selectedPinIndex);", null)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MapHeader(
                route = route,
                expanded = expanded,
                onToggle = { onExpandedChange(!expanded) },
                onRecenter = {
                    webViewRef?.evaluateJavascript("window.recenterMap && window.recenterMap();", null)
                },
                onOpenExternal = {
                    launchExternalMaps(context, route.pins.getOrNull(selectedPinIndex), route.destination)
                },
                onDismiss = onDismiss
            )

            if (expanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(horizontal = Dimens.sm)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, SubtleBorder, RoundedCornerShape(14.dp))
                        .background(Color(0xFFE8EEF3))
                ) {
                    key(route) {
                        LeafletMapView(
                            route = route,
                            initialFocusIndex = selectedPinIndex,
                            onMarkerClicked = { selectedPinIndex = it },
                            onReady = { mapReady = true },
                            onWebViewCreated = { webViewRef = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Loading indicator overlay while map tiles connect
                    if (!mapReady) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE8EEF3).copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MapBlue
                                )
                                Text(
                                    text = "Rendering map pins...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }

                LazyRow(
                    state = stripState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.sm, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(route.pins) { index, pin ->
                        StopCardItem(
                            pin = pin,
                            index = index,
                            isSelected = index == selectedPinIndex,
                            onClick = { selectedPinIndex = index },
                            onNavigateClick = { launchExternalMaps(context, pin, route.destination) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapHeader(
    route: AssistantMapRoute,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRecenter: () -> Unit,
    onOpenExternal: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = Dimens.md, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MapBlue, modifier = Modifier.size(17.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = route.itineraryTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (expanded) "Tap a pin or stop to focus" else "${route.pins.size} stops • Tap to view map",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextSecondary,
                maxLines = 1
            )
        }

        if (expanded) {
            IconButton(onClick = onRecenter, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recenter map", tint = Color(0xFF475569), modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onOpenExternal, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open in Google Maps", tint = MapBlue, modifier = Modifier.size(18.dp))
            }
        }

        IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                contentDescription = if (expanded) "Collapse map" else "Expand map",
                tint = Color(0xFF475569),
                modifier = Modifier.size(22.dp)
            )
        }
        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close map", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

private class MapJsBridge(
    private val onMarker: (Int) -> Unit,
    private val onReady: () -> Unit
) {
    private val main = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onMarkerClicked(index: Int) {
        main.post { onMarker(index) }
    }

    @JavascriptInterface
    fun onMapReady() {
        main.post { onReady() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LeafletMapView(
    route: AssistantMapRoute,
    initialFocusIndex: Int,
    onMarkerClicked: (Int) -> Unit,
    onReady: () -> Unit,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.apply {
                stopLoading()
                removeJavascriptInterface("AndroidApp")
                destroy()
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadsImagesAutomatically = true
                    blockNetworkImage = false
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    cacheMode = WebSettings.LOAD_DEFAULT
                    // Custom user-agent so OSM/Carto tile servers permit the tile requests
                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36 TripMate/1.0"
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    @Suppress("DEPRECATION")
                    allowFileAccess = true
                    @Suppress("DEPRECATION")
                    allowContentAccess = true
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                }
                setBackgroundColor(android.graphics.Color.parseColor("#E8EEF3"))

                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onReady()
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                        Log.d(TAG, "WebView: ${msg.message()} (line ${msg.lineNumber()})")
                        return true
                    }
                }

                addJavascriptInterface(MapJsBridge(onMarkerClicked, onReady), "AndroidApp")

                // Read inlined assets once
                val css = LeafletAssetsCache.getCss(ctx)
                val js = LeafletAssetsCache.getJs(ctx)
                val html = buildLeafletHtml(route, initialFocusIndex, css, js)

                // Use HTTPS base URL so tile requests are HTTPS-to-HTTPS (no file:/// CORS block)
                loadDataWithBaseURL(
                    "https://tile.openstreetmap.org/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
                webView = this
                onWebViewCreated(this)
            }
        }
    )
}

@Composable
private fun StopCardItem(
    pin: AssistantItineraryPin,
    index: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) MapBlue else Color(0xFFE2E8F0)),
        modifier = Modifier.width(190.dp)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MapBlue else PrimaryOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pin.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Day ${pin.dayNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextSecondary,
                    maxLines = 1
                )
            }
            IconButton(onClick = onNavigateClick, modifier = Modifier.size(26.dp)) {
                Icon(
                    Icons.Filled.NearMe,
                    contentDescription = "Navigate in Google Maps",
                    tint = if (isSelected) MapBlue else Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Builds standalone HTML with inlined Leaflet CSS and JS.
 * Connects to OpenStreetMap and CARTO Voyager tiles directly with fallback.
 */
private fun buildLeafletHtml(
    route: AssistantMapRoute,
    initialFocusIndex: Int,
    inlinedCss: String,
    inlinedJs: String
): String {
    val pinsJson = JSONArray().apply {
        route.pins.forEachIndexed { idx, pin ->
            put(JSONObject().apply {
                put("index", idx)
                put("title", pin.title)
                put("day", pin.dayNumber)
                put("lat", pin.latitude ?: 15.4989)
                put("lng", pin.longitude ?: 73.8278)
            })
        }
    }.toString()

    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<style>
  $inlinedCss
  html, body, #map { width:100%; height:100%; margin:0; padding:0; background:#E8EEF3;
    font-family:-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
  .pin { display:flex; align-items:center; justify-content:center; width:28px; height:28px;
    background:#2563EB; color:#fff; font-size:12px; font-weight:700; border-radius:50%;
    border:2.5px solid #fff; box-shadow:0 3px 8px rgba(0,0,0,.3); transition:transform .2s, background .2s; }
  .pin.active { background:#EA580C; transform:scale(1.25); }
  .leaflet-popup-content-wrapper { border-radius:10px; box-shadow:0 4px 12px rgba(0,0,0,0.15); }
  .leaflet-popup-content { margin:8px 12px; }
  .t { font-weight:700; font-size:13px; color:#0f172a; }
  .d { font-size:11px; color:#64748b; margin-top:2px; }
</style>
<script>
  $inlinedJs
</script>
</head>
<body>
<div id="map"></div>
<script>
(function () {
  try {
    if (typeof L === 'undefined') {
      console.error('Leaflet is not defined');
      return;
    }

    var pins = $pinsJson;
    var markers = [];
    var bounds = [];

    var map = L.map('map', { zoomControl:false, attributionControl:false });

    // Primary: OpenStreetMap standard tiles (reliable globally)
    var osm = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      crossOrigin: true
    });

    // Secondary fallback: CARTO Voyager tiles
    var carto = L.tileLayer('https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}@2x.png', {
      maxZoom: 19,
      crossOrigin: true
    });

    var osmFailed = false;
    osm.on('tileerror', function () {
      if (!osmFailed) {
        osmFailed = true;
        map.removeLayer(osm);
        carto.addTo(map);
        console.warn('Switching to CARTO tiles fallback');
      }
    });

    osm.addTo(map);

    pins.forEach(function (p, idx) {
      var icon = L.divIcon({
        className: '',
        html: '<div id="pin-' + idx + '" class="pin">' + (idx + 1) + '</div>',
        iconSize: [28, 28],
        iconAnchor: [14, 14],
        popupAnchor: [0, -14]
      });

      var m = L.marker([p.lat, p.lng], { icon: icon }).addTo(map);
      m.bindPopup('<div class="t">' + p.title + '</div><div class="d">Day ' + p.day + ' stop</div>');
      m.on('click', function () {
        setActive(idx);
        if (window.AndroidApp) window.AndroidApp.onMarkerClicked(idx);
      });
      markers.push(m);
      bounds.push([p.lat, p.lng]);
    });

    function fit() {
      map.invalidateSize();
      if (bounds.length > 1) {
        map.fitBounds(bounds, { padding: [30, 30], maxZoom: 14 });
      } else if (bounds.length === 1) {
        map.setView(bounds[0], 13);
      } else {
        map.setView([15.4989, 73.8278], 11);
      }
    }

    function setActive(idx) {
      for (var i = 0; i < pins.length; i++) {
        var el = document.getElementById('pin-' + i);
        if (el) el.classList.toggle('active', i === idx);
      }
    }

    window.focusPin = function (idx) {
      var m = markers[idx];
      if (!m) return;
      map.invalidateSize();
      setActive(idx);
      map.flyTo(m.getLatLng(), Math.max(map.getZoom(), 13), { duration: 0.6 });
      m.openPopup();
    };

    window.recenterMap = function () {
      map.closePopup();
      fit();
    };

    fit();
    setTimeout(fit, 100);
    setTimeout(fit, 400);
    window.addEventListener('resize', function () { map.invalidateSize(); });
    setActive($initialFocusIndex);

    if (window.AndroidApp) window.AndroidApp.onMapReady();
  } catch (err) {
    console.error('Map init error:', err);
  }
})();
</script>
</body>
</html>
""".trimIndent()
}

private fun launchExternalMaps(context: Context, pin: AssistantItineraryPin?, destination: String) {
    try {
        if (pin?.latitude != null && pin.longitude != null) {
            val geoUri = Uri.parse("geo:${pin.latitude},${pin.longitude}?q=${pin.latitude},${pin.longitude}(${Uri.encode(pin.title)})")
            val intent = Intent(Intent.ACTION_VIEW, geoUri).setPackage("com.google.android.apps.maps")
            try {
                context.startActivity(intent)
            } catch (_: Exception) {
                val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${pin.latitude},${pin.longitude}")
                context.startActivity(Intent(Intent.ACTION_VIEW, web))
            }
        } else {
            val query = if (pin != null) "${pin.title}, $destination" else destination
            val web = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    } catch (e: Exception) {
        Log.w(TAG, "No app available to open maps", e)
    }
}
