package me.prince.launcher

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

private const val TAG = "Launcher"

class MainActivity : ComponentActivity() {
    private var reloadKey by mutableIntStateOf(0)
    private var homePresses by mutableIntStateOf(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            reloadKey++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Back on the home screen should do nothing, not exit the launcher
        onBackPressedDispatcher.addCallback(this) {}
        startHotwordService(this)

        registerReceiver(packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })

        setContent {
            HomeScreen(
                reloadKey = reloadKey,
                homePresses = homePresses,
                onLaunch = { launch(it.launchIntent) },
                onAppInfo = { app ->
                    launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}")))
                },
                onWifi = { launch(Intent(Settings.ACTION_WIFI_SETTINGS)) },
                onSettings = { launch(Intent(Settings.ACTION_SETTINGS)) },
            )
        }
    }

    // Home pressed while already on the home screen: jump back to the first app
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homePresses++
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    private fun launch(intent: Intent) {
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No activity for $intent", e)
        }
    }
}
