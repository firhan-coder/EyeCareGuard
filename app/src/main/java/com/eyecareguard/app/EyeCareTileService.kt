package com.eyecareguard.app

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class EyeCareTileService : TileService() {

    private var isActive = false

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!isActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivityAndCollapse(intent)
                return
            }
            startOverlay()
            isActive = true
        } else {
            stopOverlay()
            isActive = false
        }
        updateTile()
    }

    private fun startOverlay() {
        val prefs = getSharedPreferences(EyeRestService.PREFS_NAME, MODE_PRIVATE)
        val intensity = prefs.getInt("saved_intensity", 45)
        val color = prefs.getString("saved_color", "#FFB300") ?: "#FFB300"

        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra(OverlayService.EXTRA_INTENSITY, intensity)
            putExtra(OverlayService.EXTRA_COLOR, color)
        }
        startForegroundService(intent)
    }

    private fun stopOverlay() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        if (isActive) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = "Eyecare ON"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = "Eyecare"
        }
        tile.updateTile()
    }
}
