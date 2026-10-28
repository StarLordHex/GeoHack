package org.geohack.game

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.UUID

/** Opt-in, foreground BLE scan + advertise. Only GeoHack service UUID packets count. */
class RadarService : Service() {
    companion object {
        val GAME_UUID: UUID = UUID.fromString("be86e2a9-6840-4d62-a49c-33c82b8427e9")
        const val ACTION_STOP = "org.geohack.game.STOP_RADAR"
        const val CHANNEL = "geohack_radar"
        const val ALERTS = "geohack_encounters"
    }
    private val handler = Handler(Looper.getMainLooper())
    private val bluetooth by lazy { getSystemService(BLUETOOTH_SERVICE) as BluetoothManager }
    private var scanner: BluetoothLeScanner? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var token = ByteArray(8)
    private var active = false
    private var scanning = false
    private val rotation = object : Runnable {
        override fun run() {
            if (!active) return
            stopAdvertise()
            token = ByteArray(8).also { SecureRandom().nextBytes(it) }
            startAdvertise()
            handler.postDelayed(this, 15 * 60 * 1000L)
        }
    }
    private val scanCycle = object : Runnable {
        override fun run() {
            if (!active) return
            if (scanning) stopScan() else startScan()
            handler.postDelayed(this, if (scanning) 12_000L else 18_000L)
        }
    }
    private val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val bytes = result.scanRecord?.getServiceData(android.os.ParcelUuid(GAME_UUID)) ?: return
            if (bytes.size != 8 || bytes.contentEquals(token)) return
            val id = bytes.joinToString("") { "%02x".format(it) }
            val store = getSharedPreferences("encounters", MODE_PRIVATE)
            val old = store.getStringSet("seen", emptySet()).orEmpty()
            if (id !in old) {
                store.edit().putStringSet("seen", old + id).apply()
                notifyEncounter(id.take(8))
            }
        }
    }
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "GeoHack radar", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel(ALERTS, "New encounters", NotificationManager.IMPORTANCE_DEFAULT))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        val stop = PendingIntent.getService(this, 0, Intent(this, RadarService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("GeoHack radar active").setContentText("Searching for opted-in GeoHack players")
            .addAction(Notification.Action.Builder(null, "Stop", stop).build()).build()
        startForeground(101, notification)
        if (!hasPermissions()) { stopSelf(); return START_NOT_STICKY }
        if (!active) { active = true; handler.post(rotation); handler.post(scanCycle) }
        return START_NOT_STICKY
    }
    private fun hasPermissions() = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT)
        .all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    private fun startScan() {
        try {
            scanner = bluetooth.adapter?.bluetoothLeScanner
            val filter = ScanFilter.Builder().setServiceUuid(android.os.ParcelUuid(GAME_UUID)).build()
            scanner?.startScan(listOf(filter), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build(), callback)
            scanning = scanner != null
        } catch (_: SecurityException) { scanning = false }
    }
    private fun stopScan() {
        try { scanner?.stopScan(callback) } catch (_: SecurityException) { }
        scanning = false
    }
    private fun startAdvertise() {
        try {
            advertiser = bluetooth.adapter?.bluetoothLeAdvertiser
            val data = AdvertiseData.Builder().addServiceUuid(android.os.ParcelUuid(GAME_UUID))
                .addServiceData(android.os.ParcelUuid(GAME_UUID), token).setIncludeDeviceName(false).build()
            advertiser?.startAdvertising(AdvertiseSettings.Builder().setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(false).build(), data, adCallback)
        } catch (_: SecurityException) { }
    }
    private fun stopAdvertise() {
        try { advertiser?.stopAdvertising(adCallback) } catch (_: SecurityException) { }
    }
    private val adCallback = object : AdvertiseCallback() {}
    private fun notifyEncounter(id: String) {
        val notification = Notification.Builder(this, ALERTS).setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("GeoHack player detected").setContentText("Nearby token: $id — open Radar to inspect")
            .setAutoCancel(true).build()
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(id.hashCode(), notification)
    }
    override fun onDestroy() {
        active = false; handler.removeCallbacksAndMessages(null); stopScan(); stopAdvertise(); super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
