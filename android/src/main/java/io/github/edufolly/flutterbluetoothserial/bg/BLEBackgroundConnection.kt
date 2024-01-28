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
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterCallbackInformation
import io.github.edufolly.flutterbluetoothserial.BluetoothConnection
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnDisconnectedCallback
import io.github.edufolly.flutterbluetoothserial.BluetoothConnectionBase.OnReadCallback
import io.github.edufolly.flutterbluetoothserial.FlutterBluetoothSerialPlugin
import io.github.edufolly.flutterbluetoothserial.bg.param.StartServiceParam
import io.github.edufolly.flutterbluetoothserial.le.BluetoothConnectionLE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.timer
import kotlin.coroutines.CoroutineContext

class BLEBackgroundConnection : Service(), MethodChannel.MethodCallHandler, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    companion object {
        const val engineId = "BLEBackground"
        const val channelId = "BLEBackgroundChannel"
        const val readChannelId = "$channelId/read"
        const val startServiceParamKey = "startServiceParamKey"
        var isServiceRunning: Boolean = false
    }

    private lateinit var engine: FlutterEngine
    private lateinit var methodChannel: MethodChannel
    private lateinit var adapter: BluetoothAdapter
    private var timer: Timer? = null
    private var scanInterval: Long = 15000

    private var addresses = mutableSetOf<String>()
    private val connections = mutableMapOf<String, BluetoothConnection>()
    private var readCallbackHandle: Long = 0

    private val messenger
        get() = engine.dartExecutor.binaryMessenger

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val params = intent?.getParcelableExtra<StartServiceParam>(startServiceParamKey)
        if (params == null) {
            stopSelf()
            return START_STICKY
        }

        ensureFlutterInitialized(params.initCallbackHandle)

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

        startForeground(101, notification)

        scanInterval = params.androidSettings.scanInterval
        startTimer(scanInterval)

        isServiceRunning = true

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun ensureFlutterInitialized(callbackHandle: Long) {
        val engineCache = FlutterEngineCache.getInstance()
        if (engineCache.contains(engineId)) {
            engine = engineCache.get(engineId)!!
        } else {
            engine = FlutterEngine(this)
            engineCache.put(engineId, engine)
        }

        // register callback handle
        val callbackInfo = FlutterCallbackInformation.lookupCallbackInformation(callbackHandle)
        val args = DartExecutor.DartCallback(
            assets,
            FlutterInjector.instance().flutterLoader().findAppBundlePath(),
            callbackInfo
        )
        engine.dartExecutor.executeDartCallback(args)

        methodChannel = MethodChannel(messenger, channelId)
        methodChannel.setMethodCallHandler(this)
    }

    private fun startTimer(interval: Long) {
        timer?.cancel()
        timer = timer(period = interval) {
            for (address in addresses) {
                if (!connections.containsKey(address)) {
                    connect(address)
                }
            }

            if (connections.keys.all { addresses.contains(it) }) {
                timer?.cancel()
                timer = null
            }
        }
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            BLEBackgroundMethod.connect -> connect(result, call.argument<String>("address"))
            BLEBackgroundMethod.disconnect -> connect(result, call.argument<String>("address"))
            else -> result.notImplemented()
        }
    }

    private fun connect(result: MethodChannel.Result, address: String?) {
        if (address == null || !BluetoothAdapter.checkBluetoothAddress(address)) {
            result.error("invalid_argument", "address is not valid", null)
            return
        }

        if (connections[address]?.isConnected == true) {
            result.error("already_connected", "already connected", null)
            return
        }

        connect(address)

        result.success(null)
    }

    private fun connect(address: String) {
        var connection: BluetoothConnectionLE? = null
        var readSink: EventSink? = null

        val readChannel = EventChannel(messenger, readChannelId)
        readChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                readSink = events
            }

            override fun onCancel(arguments: Any?) {
                launch {
                    connection?.disconnect()
                    connection = null

                    startTimer(scanInterval)
                }
            }
        })

        val onRead = object : OnReadCallback {
            override fun onRead(data: ByteArray) {}
            override fun onRead(device: BluetoothDevice, data: ByteArray) {
                launch {
                    val deviceMap = mapOf(
                        "address" to device.address,
                        "name" to device.name,
                        "type" to device.type,
                        "isConnected" to FlutterBluetoothSerialPlugin.checkIsDeviceConnected(device),
                        "bondState" to device.bondState,
                    )
                    readSink?.success(
                        mapOf(
                            "device" to deviceMap,
                            "bytes" to data,
                            "readCallbackHandle" to readCallbackHandle,
                        )
                    )
                }
            }
        }

        val onDisconnect = OnDisconnectedCallback { byRemote ->
            if (byRemote) {
                launch {
                    readSink?.endOfStream()
                    readSink = null
                }
            }
        }

        connection = BluetoothConnectionLE(onRead, onDisconnect, this)
        connection?.connect(address)

        connections[address] = connection!!
        addresses.add(address)
    }

    private fun disconnect(result: MethodChannel.Result, address: String?) {
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
    }
}