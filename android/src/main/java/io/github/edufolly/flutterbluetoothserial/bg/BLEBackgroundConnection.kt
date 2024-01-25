package io.github.edufolly.flutterbluetoothserial.bg

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.SparseArray
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
import io.github.edufolly.flutterbluetoothserial.le.BluetoothConnectionLE

class BLEBackgroundConnection : Service(), MethodChannel.MethodCallHandler {

    companion object {
        const val engineId = "BLEBackground"
        const val channelId = "BLEBackgroundChannel"
        const val readChannelId = "$channelId/read"
    }

    private lateinit var engine: FlutterEngine
    private lateinit var methodChannel: MethodChannel
    private lateinit var discoveryChannel: EventChannel
    private lateinit var adapter: BluetoothAdapter

    private val targetAddresses = mutableListOf<String>()
    private val connections = SparseArray<BluetoothConnectionLE>()

    private val messenger
        get() = engine.dartExecutor.binaryMessenger

    private var lastConnectionId: Int = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_STICKY
        }

        val callbackHandle = intent.getLongExtra("callbackHandle", 0)
        ensureFlutterInitialized(callbackHandle)

        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter

        val address = intent.getStringExtra("address")
        if (address != null) {
            targetAddresses.add(address)
        }

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null;
    }

    private fun ensureFlutterInitialized(callbackHandle: Long) {
        val engineCache = FlutterEngineCache.getInstance()
        if (engineCache.contains(engineId)) {
            engine = engineCache.get(engineId)!!
        } else {
            engine = FlutterEngine(this)
            engineCache.put(engineId, engine)
        }
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

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            BLEBackgroundMethod.connect -> connect(result, call.argument<String>("address"))
            else -> result.notImplemented()
        }
    }

    private fun connect(result: MethodChannel.Result, address: String?) {
        if (address == null) {
            result.error("invalid_argument", "address is not found", null)
            return
        }

        val readChannel = EventChannel(messenger, readChannelId)
        readChannel.setStreamHandler(object : StreamHandler {
            override fun onListen(arguments: Any?, events: EventSink?) {
                TODO("Not yet implemented")
            }

            override fun onCancel(arguments: Any?) {
                TODO("Not yet implemented")
            }
        })
    }
}
