package com.hoomanconnect.app.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.hoomanconnect.app.MainActivity
import com.hoomanconnect.app.R

/** Central Android VPN entry point. It deliberately refuses to create a TUN until a
 * verified packet-forwarding CoreAdapter is installed; an empty TUN would black-hole traffic. */
class HoomanVpnService : VpnService() {
    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                publish(STATE_DISCONNECTED, "درخواست توقف دریافت شد؛ تونلی فعال نبود.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("در حال بررسی هسته اتصال…"))
                // No core is bundled/verified in v0.3.0. Do not establish a dummy VPN interface.
                publish(STATE_ERROR, "هسته سازگار هنوز نصب و تأیید نشده است؛ اتصال آغاز نشد.")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(message: String): Notification {
        val channelId = "hooman_vpn_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "وضعیت Hooman Connect", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Hooman Connect")
            .setContentText(message)
            .setContentIntent(openApp)
            .setOngoing(false)
            .build()
    }

    private fun publish(state: String, message: String) {
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra(EXTRA_STATE, state).putExtra(EXTRA_MESSAGE, message))
    }

    companion object {
        const val ACTION_START = "com.hoomanconnect.app.vpn.START"
        const val ACTION_STOP = "com.hoomanconnect.app.vpn.STOP"
        const val ACTION_STATUS = "com.hoomanconnect.app.vpn.STATUS"
        const val EXTRA_STATE = "state"
        const val EXTRA_MESSAGE = "message"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_ERROR = "ERROR"
        private const val NOTIFICATION_ID = 3101
    }
}
