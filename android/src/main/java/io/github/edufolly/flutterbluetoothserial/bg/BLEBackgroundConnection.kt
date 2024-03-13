package io.github.edufolly.flutterbluetoothserial.bg

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.EventChannel.EventSink
import io.flutter.plugin.common.EventChannel.StreamHandler
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import io.github.edufolly.flutterbluetoothserial.BluetoothConnection
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnConnectCallback
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnDisconnectedCallback
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnReadCallback
import io.github.edufolly.flutterbluetoothserial.BuildConfig
import io.github.edufolly.flutterbluetoothserial.FlutterBluetoothSerialPlugin
import io.github.edufolly.flutterbluetoothserial.bg.param.StartServiceParam
import io.github.edufolly.flutterbluetoothserial.le.BluetoothConnectionLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext

class BLEBackgroundConnection : Service(), CoroutineScope {

    inner class Binder : android.os.Binder() {
        fun getService(): BLEBackgroundConnection {
            return this@BLEBackgroundConnection
        }
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    companion object {
        const val engineId = "BLEBackground"
        const val channelId = "BLEBackgroundChannel"
        const val initChannelId = "$channelId/init"
        const val readChannelId = "$channelId/read"
        const val stateChangeChannelId = "$channelId/stateChange"
        const val startServiceParamKey = "startServiceParamKey"
        var isServiceRunning: Boolean = false
    }

    private lateinit var binder: Binder
    private lateinit var engine: FlutterEngine
    private lateinit var adapter: BluetoothAdapter
    private lateinit var store: BLEBackgroundStore
    private var readSink: EventSink? = null
    private var stateChangeSink: EventSink? = null
    private var timer: Timer? = null
    private var scanInterval: Long = 15000

    private var addresses: MutableSet<String> = mutableSetOf()
    private val connections: MutableMap<String, BluetoothConnection> = mutableMapOf()
    private var readCallbackHandle: Long = 0
    private var stateChangeCallbackHandle: Long? = null

    private val messenger
        get() = engine.dartExecutor.binaryMessenger


    override fun onCreate() {
        super.onCreate()
        binder = Binder()
        store = BLEBackgroundStore(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val params = intent?.getParcelableExtra<StartServiceParam>(startServiceParamKey)
        if (params == null) {
            stopSelf()
            return START_STICKY
        }

        ensureFlutterInitialized(params.serviceCallbackHandle)
        readCallbackHandle = params.readCallbackHandle
        stateChangeCallbackHandle = params.stateChangeCallbackHandle

        val initChannel = EventChannel(messenger, initChannelId)
        initChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                events?.success(mapOf("initCallbackHandle" to params.initCallbackHandle))
            }

            override fun onCancel(arguments: Any?) {
            }
        })

        val stateChangeChannel = EventChannel(messenger, stateChangeChannelId)
        stateChangeChannel.setStreamHandler(object: StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                stateChangeSink = events
            }

            override fun onCancel(arguments: Any?) {
            }
        })

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationUtil.createNotificationChannel(this)
        }

        val notification = NotificationUtil.createNotification(
            this,
            params.androidSettings.notificationTitle,
            params.androidSettings.notificationBody,
        )

        startForeground(1, notification)

        addresses = store.getAddressSet()
        scanInterval = params.androidSettings.scanInterval
        startTimer(scanInterval)

        isServiceRunning = true

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    private fun ensureFlutterInitialized(callbackHandle: Long) {
        val engineCache = FlutterEngineCache.getInstance()
        if (engineCache.contains(engineId)) {
            engine = engineCache.get(engineId)!!
        } else {
            engine = FlutterEngine(this)
            engineCache.put(engineId, engine)
        }

        createReadChannel()

        // register callback handle
        val callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
        val args = DartExecutor.DartCallback(
            assets,
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            callbackInfo
        )
        engine.dartExecutor.executeDartCallback(args)
    }

    private fun createReadChannel() {
        val readChannel = EventChannel(messenger, readChannelId)
        readChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                readSink = events
            }

            override fun onCancel(arguments: Any?) {
                launch {
                    connections.forEach { entry ->
                        entry.value.disconnect()
                    }
                    connections.clear()
                    startTimer(scanInterval)
                }
            }
        })
    }

    private fun startTimer(interval: Long) {
        timer?.cancel()
        timer = timer(period = interval) {
            for (address in addresses) {
                if (!connections.containsKey(address)) {
                    try {
                        connect(address)
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            if (connections.keys.all { addresses.contains(it) }) {
                timer?.cancel()
                timer = null
            }
        }
    }

    fun connect(result: MethodChannel.Result, address: String?) {
        if (address == null || !BluetoothAdapter.checkBluetoothAddress(address)) {
            result.error("invalid_argument", "address is not valid", null)
            return
        }

        if (connections[address]?.isConnected == true) {
            result.error("already_connected", "already connected", null)
            return
        }

        try {
            connect(address)
            result.success(null)
        } catch (e: Exception) {
            result.error("connection_error", null, null)
        }
    }

    private fun connect(address: String) {
        val onRead = object : OnReadCallback {
            override fun onRead(data: ByteArray) {}
            override fun onRead(device: BluetoothDevice, data: ByteArray) {
                launch {
                    readSink?.success(
                        mapOf(
                            "device" to getDeviceMap(device),
                            "bytes" to data,
                            "readCallbackHandle" to readCallbackHandle,
                        )
                    )
                }
            }
        }

        val onConnect = OnConnectCallback {
            val device = getDevice(address)
            if (device != null) {
                launch {
                    stateChangeSink?.success(
                        mapOf(
                            "device" to getDeviceMap(device),
                            "connected" to true,
                            "stateChangeCallbackHandle" to stateChangeCallbackHandle
                        )
                    )
                }
            }
        }

        val onDisconnect = OnDisconnectedCallback { byRemote ->
//            if (byRemote) {
//                launch {
//                    readSink?.endOfStream()
//                    readSink = null
//                }
//            }
            connections[address]?.disconnect()
            connections.remove(address)

            val device = getDevice(address)
            if (device != null) {
                launch {
                    stateChangeSink?.success(
                        mapOf(
                            "device" to getDeviceMap(device),
                            "connected" to false,
                            "stateChangeCallbackHandle" to stateChangeCallbackHandle
                        )
                    )
                }
            }

            startTimer(scanInterval)
        }

        val connection = BluetoothConnectionLE(onRead, onConnect, onDisconnect, this)
        connection.connect(address)
        connections[address] = connection
        addresses.add(address)

        store.putAddressSet(addresses)
    }

    fun disconnect(result: MethodChannel.Result, address: String?) {
        if (address == null || !BluetoothAdapter.checkBluetoothAddress(address)) {
            result.error("invalid_argument", "address is not valid", null)
            return
        }

        disconnect(address)
        result.success(null)
    }

    private fun disconnect(address: String) {
        connections[address]?.disconnect()
        connections.remove(address)

        addresses.remove(address)
        store.putAddressSet(addresses)
    }

    fun getConnectedDevices(result: MethodChannel.Result) {
        val devices = connections.keys
            .mapNotNull { getDevice(it) }
            .map { getDeviceMap(it) }

        result.success(devices)
    }

    private fun getDevice(address: String): BluetoothDevice? {
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter.getRemoteDevice(address)
    }

    private fun getDeviceMap(device: BluetoothDevice): Map<String, Any> {
        return mapOf(
            "address" to device.address,
            "name" to device.name,
            "type" to device.type,
            "isConnected" to FlutterBluetoothSerialPlugin.checkIsDeviceConnected(device),
            "bondState" to device.bondState,
        )
    }

    fun write(result: MethodChannel.Result, address: String, bytes: ByteArray) {
        try {
            val connection = connections[address]
            connection?.write(bytes)
            launch { result.success(null) }
        } catch (e: Exception) {
            launch { result.error("write_error", e.message, FlutterBluetoothSerialPlugin.exceptionToString(e)) }
        }
    }
}
