package io.github.edufolly.flutterbluetoothserial.bg.param

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class StartServiceParam(
    val initCallbackHandle: Long,
    val readCallbackHandle: Long,
    val androidSettings: AndroidSettings
) : Parcelable {
    @Parcelize
    @Serializable
    data class AndroidSettings(
        val notificationTitle: String,
        val notificationBody: String,
        val showConnections: Boolean,
        val scanInterval: Long,
        val startAfterBoot: Boolean,
    ) : Parcelable {
        companion object {
            @JvmStatic
            fun fromArguments(args: Any?): AndroidSettings {
                val map = args as? Map<*, *> ?: throw InvalidArgumentException()
                return AndroidSettings(
                    notificationTitle = map["notificationTitle"] as? String
                        ?: throw InvalidArgumentException(),
                    notificationBody = map["notificationBody"] as? String
                        ?: throw InvalidArgumentException(),
                    showConnections = map["showConnections"] as? Boolean
                        ?: throw InvalidArgumentException(),
                    scanInterval = (map["scanInterval"] as? Int)?.toLong()
                        ?: throw InvalidArgumentException(),
                    startAfterBoot = map["startAfterBoot"] as? Boolean
                        ?: throw InvalidArgumentException(),
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun fromArguments(args: Any?): StartServiceParam {
            val map = args as? Map<*, *> ?: throw InvalidArgumentException()
            return StartServiceParam(
                initCallbackHandle = map["initCallbackHandle"] as? Long ?: throw InvalidArgumentException(),
                readCallbackHandle = map["readCallbackHandle"] as? Long ?: throw InvalidArgumentException(),
                androidSettings = AndroidSettings.fromArguments(map["androidSettings"])
            )
        }
    }
}