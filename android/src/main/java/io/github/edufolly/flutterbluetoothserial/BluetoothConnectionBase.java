package io.github.edufolly.flutterbluetoothserial;

import android.bluetooth.BluetoothDevice;

public abstract class BluetoothConnectionBase implements io.github.edufolly.flutterbluetoothserial.BluetoothConnection {
    public interface OnReadCallback {
        void onRead(byte[] data);
        default void onRead(BluetoothDevice device, byte[] data) {}
    }

    public interface OnConnectCallback {
        void onConnect();
    }

    public interface OnDisconnectedCallback {
        public void onDisconnected(boolean byRemote);
    }

    final OnReadCallback onReadCallback;
    final OnDisconnectedCallback onDisconnectedCallback;

    public BluetoothConnectionBase(OnReadCallback onReadCallback, OnDisconnectedCallback onDisconnectedCallback) {
        this.onReadCallback = onReadCallback;
        this.onDisconnectedCallback = onDisconnectedCallback;
    }

    public void onRead(byte[] data) {
        onReadCallback.onRead(data);
    }

    public void onRead(BluetoothDevice device, byte[] data) {
        onReadCallback.onRead(device, data);
    }

    public void onDisconnected(boolean byRemote) {
        onDisconnectedCallback.onDisconnected(byRemote);
    }
}
