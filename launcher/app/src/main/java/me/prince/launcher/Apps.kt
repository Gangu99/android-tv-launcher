package me.prince.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette

// The favourites shelf, in this order; anything not installed is skipped.
// Change it to your apps (find package names with: adb shell pm list packages -3)
private val FAVOURITES = listOf(
    "com.netflix.ninja",                  // Netflix
    "com.google.android.youtube.tv",      // YouTube
    "com.amazon.amazonvideo.livingroom",  // Prime Video
    "in.startv.hotstar",                  // JioHotstar
    "com.disney.disneyplus",              // Disney+
)

private const val BANNER_WIDTH = 400
private const val BANNER_HEIGHT = 225
private val FALLBACK_GLOW = Color(0xFF3A4A8C)

data class TvApp(
    val packageName: String,
    val label: String,
    val image: ImageBitmap,
    val hasBanner: Boolean,
    val glow: Color,
    val launchIntent: Intent,
)

data class AppShelves(val favourites: List<TvApp>, val others: List<TvApp>)

fun loadApps(context: Context): AppShelves {
    val pm = context.packageManager
    val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
    val apps = pm.queryIntentActivities(query, 0)
        .filter { it.activityInfo.packageName != context.packageName }
        .distinctBy { it.activityInfo.packageName }
        .map { toTvApp(context, it) }

    val byPackage = apps.associateBy { it.packageName }
    val favourites = FAVOURITES.mapNotNull { byPackage[it] }
    val others = apps.filter { it.packageName !in FAVOURITES }.sortedBy { it.label.lowercase() }
    return AppShelves(favourites, others)
}

private fun toTvApp(context: Context, info: ResolveInfo): TvApp {
    val pm = context.packageManager
    val activity = info.activityInfo
    val banner = activity.loadBanner(pm)
    val drawable = banner ?: activity.loadIcon(pm)
    val bitmap = if (banner != null) {
        drawable.toBitmap(BANNER_WIDTH, BANNER_HEIGHT)
    } else {
        drawable.toBitmap(128, 128)
    }
    val intent = Intent(Intent.ACTION_MAIN)
        .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        .setClassName(activity.packageName, activity.name)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    return TvApp(
        packageName = activity.packageName,
        label = info.loadLabel(pm).toString(),
        image = bitmap.asImageBitmap(),
        hasBanner = banner != null,
        glow = glowColor(bitmap),
        launchIntent = intent,
    )
}

// Brand colour of the banner, e.g. Netflix red rather than its white background
private fun glowColor(bitmap: android.graphics.Bitmap): Color {
    val palette = Palette.from(bitmap).generate()
    val swatch = palette.vibrantSwatch
        ?: palette.darkVibrantSwatch
        ?: palette.mutedSwatch
        ?: palette.dominantSwatch
        ?: return FALLBACK_GLOW
    val color = Color(swatch.rgb)
    if (color.luminance() > 0.8f || color.luminance() < 0.02f) return FALLBACK_GLOW
    return lerp(color, Color.Black, 0.15f)
}
