package pwc.mf.bt2smsworkflow;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.Context;


public class BluetoothA2DPRequester implements BluetoothProfile.ServiceListener {
    private Callback mCallback;

    /**
     * Creates a new instance of an A2DP Proxy requester with the
     * callback that should receive the proxy once it is acquired
     * @param callback the callback that should receive the proxy
     */
    public BluetoothA2DPRequester(Callback callback) {
        mCallback = callback;
    }

    /**
     * Start an asynchronous request to acquire the A2DP proxy. The callback
     * will be notified when the proxy is acquired
     * @param c the context used to obtain the proxy
     * @param adapter the BluetoothAdapter that should receive the request for proxy
     */
    public void request (Context c, BluetoothAdapter adapter) {
        adapter.getProfileProxy(c, this, BluetoothProfile.A2DP);
    }

    @Override
    public void onServiceConnected(int i, BluetoothProfile bluetoothProfile) {
        if (mCallback != null) {
            mCallback.onA2DPProxyReceived((BluetoothA2dp) bluetoothProfile);
        }
    }

    @Override
    public void onServiceDisconnected(int i) {
        //It's a one-off connection attempt; we don't care about the disconnection event.
    }

    public static interface Callback {
        public void onA2DPProxyReceived (BluetoothA2dp proxy);
    }
}