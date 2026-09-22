package com.timey.app.core.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.timey.app.MainActivity

@RequiresApi(Build.VERSION_CODES.N)
class FocusQuickTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val isRunning = FocusForegroundService.serviceTimerState.value.isRunning
        if (isRunning) {
            FocusForegroundService.pause(this)
        } else {
            // Launch main app or start 25m session
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivityAndCollapse(launchIntent)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val isRunning = FocusForegroundService.serviceTimerState.value.isRunning
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Focusing..." else "Start Focus"
        tile.updateTile()
    }
}
