package me.prince.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.text.format.DateFormat
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val Background = Color(0xFF07080C)
private val TextPrimary = Color.White
private val TextSecondary = Color.White.copy(alpha = 0.75f)
private val Glass = Color.White.copy(alpha = 0.10f)
private val GlassBorder = Color.White.copy(alpha = 0.18f)

private val TileWidth = 200.dp
private val TileHeight = 112.dp
private val TileShape = RoundedCornerShape(14.dp)
private val ShelfShape = RoundedCornerShape(28.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    reloadKey: Int,
    homePresses: Int,
    onLaunch: (TvApp) -> Unit,
    onAppInfo: (TvApp) -> Unit,
    onWifi: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var shelves by remember { mutableStateOf(AppShelves(emptyList(), emptyList())) }
    var focusedApp by remember { mutableStateOf<TvApp?>(null) }
    val scroll = rememberScrollState()
    val favouritesRow = rememberLazyListState()
    val firstTile = remember { FocusRequester() }

    LaunchedEffect(reloadKey) {
        shelves = withContext(Dispatchers.IO) { loadApps(context) }
    }

    LaunchedEffect(shelves.favourites.isNotEmpty(), homePresses) {
        if (shelves.favourites.isEmpty()) return@LaunchedEffect
        scroll.animateScrollTo(0)
        favouritesRow.scrollToItem(0)
        // On cold start the tile may not be attached for a few frames; if focus is never set,
        // Android picks an arbitrary element (seen: the Settings pill)
        repeat(30) {
            withFrameNanos { }
            try {
                firstTile.requestFocus()
                return@LaunchedEffect
            } catch (e: IllegalStateException) {
                // not attached yet, try next frame
            }
        }
    }

    val shownApp = focusedApp ?: shelves.favourites.firstOrNull()
    val glow by animateColorAsState(shownApp?.glow ?: Background, tween(600), label = "glow")

    val edgeMargin = with(LocalDensity.current) { 40.dp.toPx() }
    MaterialTheme(colorScheme = darkColorScheme()) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides remember(edgeMargin) { minimalScroll(edgeMargin) }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Background)
                    .drawBehind {
                        drawRect(
                            Brush.radialGradient(
                                0f to glow.copy(alpha = 0.9f),
                                0.45f to glow.copy(alpha = 0.3f),
                                1f to Color.Transparent,
                                center = Offset(size.width * 0.36f, size.height * 0.42f),
                                radius = size.width * 0.6f,
                            )
                        )
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                    }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 56.dp)
                ) {
                    Spacer(Modifier.height(36.dp))
                    Header(onWifi = onWifi, onSettings = onSettings)
                    Spacer(Modifier.height(132.dp))
                    Text(
                        text = shownApp?.label.orEmpty(),
                        color = TextPrimary,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(12.dp))
                    Shelf(shelves.favourites, favouritesRow, firstTile, onLaunch, onAppInfo) { focusedApp = it }
                    Spacer(Modifier.height(20.dp))
                    Shelf(shelves.others, rememberLazyListState(), null, onLaunch, onAppInfo) { focusedApp = it }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

// Compose's TV default pins the focused item 30% from the edge, which scrolls the header away.
// Scroll only when the item would leave the screen, keeping a margin so the focus scale isn't clipped.
@OptIn(ExperimentalFoundationApi::class)
private fun minimalScroll(margin: Float) = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val start = offset - margin
        val end = offset + size + margin
        return when {
            start < 0 -> start
            end > containerSize -> end - containerSize
            else -> 0f
        }
    }
}

@Composable
private fun Header(onWifi: () -> Unit, onSettings: () -> Unit) {
    val context = LocalContext.current
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(60_000 - System.currentTimeMillis() % 60_000)
        }
    }
    val is24h = DateFormat.is24HourFormat(context)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(greeting(now.hour), color = TextSecondary, fontSize = 13.sp, letterSpacing = 2.5.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row {
                    Text(
                        now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm")),
                        color = TextPrimary,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.alignByBaseline(),
                    )
                    if (!is24h) {
                        Text(
                            " " + now.format(DateTimeFormatter.ofPattern("a")).uppercase(),
                            color = TextPrimary,
                            fontSize = 16.sp,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                Spacer(Modifier.width(18.dp))
                Box(Modifier.size(1.dp, 28.dp).background(Color.White.copy(alpha = 0.4f)))
                Spacer(Modifier.width(18.dp))
                Text(
                    now.format(DateTimeFormatter.ofPattern("EEEE  d MMMM")).uppercase(),
                    color = TextSecondary,
                    fontSize = 14.sp,
                    letterSpacing = 2.sp,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pill(R.drawable.ic_wifi, rememberNetworkLabel(), onWifi)
            Pill(R.drawable.ic_settings, "Settings", onSettings)
        }
    }
}

private fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "GOOD MORNING"
    in 12..16 -> "GOOD AFTERNOON"
    else -> "GOOD EVENING"
}

@Composable
private fun Pill(@DrawableRes icon: Int, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Glass,
            contentColor = TextPrimary,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(1.dp, GlassBorder), shape = CircleShape),
            focusedBorder = Border.None,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
    ) {
        Row(
            Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun Shelf(
    apps: List<TvApp>,
    state: LazyListState,
    firstTile: FocusRequester?,
    onLaunch: (TvApp) -> Unit,
    onAppInfo: (TvApp) -> Unit,
    onFocused: (TvApp) -> Unit,
) {
    if (apps.isEmpty()) return
    LazyRow(
        state = state,
        contentPadding = PaddingValues(14.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            // LazyRow only clips to a rectangle, so partly visible tiles poked past the rounded corners
            .clip(ShelfShape)
            .background(Color.White.copy(alpha = 0.06f), ShelfShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), ShelfShape)
            .edgeFade(fadeStart = state.canScrollBackward, fadeEnd = state.canScrollForward),
    ) {
        itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
            val requester = if (index == 0) firstTile else null
            AppTile(
                app = app,
                onClick = { onLaunch(app) },
                onLongClick = { onAppInfo(app) },
                modifier = Modifier
                    .then(if (requester != null) Modifier.focusRequester(requester) else Modifier)
                    .onFocusChanged { if (it.isFocused) onFocused(app) },
            )
        }
    }
}

// Softly fades tiles cut off at the shelf edge, only on the side that has more to scroll
private fun Modifier.edgeFade(fadeStart: Boolean, fadeEnd: Boolean): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = 48.dp.toPx() / size.width
        drawRect(
            Brush.horizontalGradient(
                0f to if (fadeStart) Color.Transparent else Color.Black,
                fade to Color.Black,
                1f - fade to Color.Black,
                1f to if (fadeEnd) Color.Transparent else Color.Black,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
private fun AppTile(app: TvApp, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.size(TileWidth, TileHeight),
        shape = ClickableSurfaceDefaults.shape(TileShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color(0xFF1C1D24),
            focusedContainerColor = Color(0xFF1C1D24),
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = TileShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
    ) {
        if (app.hasBanner) {
            Image(app.image, contentDescription = app.label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(app.image, contentDescription = null, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text(app.label, color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// SSID is only readable with location permission + location on; otherwise falls back to "Wi-Fi"
@Composable
private fun rememberNetworkLabel(): String {
    val context = LocalContext.current
    var label by remember { mutableStateOf(networkLabel(context)) }
    DisposableEffect(Unit) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { label = networkLabel(context) }
            override fun onLost(network: Network) { label = networkLabel(context) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { label = networkLabel(context) }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return label
}

@Suppress("DEPRECATION") // connectionInfo is the only SSID API on Android 9
private fun networkLabel(context: Context): String {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    // VPN-based ad blockers (e.g. AdGuard) make the VPN the active network; look at every network instead
    val transports = cm.allNetworks.mapNotNull { cm.getNetworkCapabilities(it) }
    if (transports.none { it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) }) return "Offline"
    return when {
        transports.any { it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } -> "Ethernet"
        transports.any { it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } -> {
            val ssid = context.applicationContext.getSystemService(WifiManager::class.java).connectionInfo.ssid
            ssid?.trim('"')?.takeUnless { it.isBlank() || it == "<unknown ssid>" } ?: "Wi-Fi"
        }
        else -> "Online"
    }
}
