package com.vera.guard.service

import android.appwidget.AppWidgetManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.vera.guard.R

object ServiceStarter {
    fun start(ctx: Context, action: String = Actions.START, configure: Intent.() -> Unit = {}) {
        val i = Intent(ctx, GuardService::class.java).setAction(action).apply(configure)
        ContextCompat.startForegroundService(ctx, i)
    }
    fun pending(ctx: Context, action: String, code: Int): PendingIntent {
        val i = Intent(ctx, GuardService::class.java).setAction(action)
        return PendingIntent.getForegroundService(ctx, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }
}

/** Restarts protection after reboot or app update. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            try { ServiceStarter.start(context) } catch (e: Exception) { /* background start can be refused; the app starts it on next open */ }
        }
    }
}

/** Home-screen widget with a big SOS button. */
class SosWidgetProvider : android.appwidget.AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.widget_sos)
            views.setOnClickPendingIntent(R.id.widget_sos_button, ServiceStarter.pending(context, Actions.SOS, 7001))
            manager.updateAppWidget(id, views)
        }
    }
}

/** Quick-settings tile "VERA SOS". */
class SosTileService : TileService() {
    override fun onStartListening() {
        qsTile?.apply { state = Tile.STATE_INACTIVE; label = getString(R.string.tile_label); updateTile() }
    }
    override fun onClick() {
        try { ServiceStarter.start(applicationContext, Actions.SOS) } catch (e: Exception) { }
        qsTile?.apply { state = Tile.STATE_ACTIVE; updateTile() }
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        intent.action?.let { try { ServiceStarter.start(context, it) } catch (e: Exception) { } }
    }
}
