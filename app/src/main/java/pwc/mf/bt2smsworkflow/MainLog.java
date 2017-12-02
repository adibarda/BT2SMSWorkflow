package pwc.mf.bt2smsworkflow;

import android.Manifest;
import android.app.ActivityManager;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class MainLog extends AppCompatActivity implements BluetoothBroadcastReceiver.Callback, BluetoothA2DPRequester.Callback {

    private static final int REQUEST_SMS_PERMISSIONS = 30;
    private static final String[] SMS_PERM={
            Manifest.permission.SEND_SMS

    };

    private static final int REQUEST_GPS_PERMISSIONS=10;
    private static final String[] GPS_PERM={
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;

    private static final String TAG = "BluetoothActivity";
    private static final String HTC_MEDIA = "HC-06";


    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;



    private BluetoothChatService mChatService = null;
    private BluetoothAdapter mAdapter;

    @Override
    public synchronized void onResume() {
        super.onResume();
        Log.e(TAG, "+ ON RESUME +");
        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_GPS_PERMISSIONS:
                if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    startGps();
                }

            case REQUEST_SMS_PERMISSIONS:
                if (hasPermission(Manifest.permission.READ_SMS)) {
                    startSMS();
                }

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_log);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });



        startSMS();
        startGps();


        mAdapter = BluetoothAdapter.getDefaultAdapter();

        //Already connected, skip the rest
        if (mAdapter.isEnabled()) {
            mChatService = new BluetoothChatService(this,mHandler);
            onBluetoothConnected();
            return;
        }

        //Check if we're allowed to enable Bluetooth. If so, listen for a
        //successful enabling
        if (mAdapter.enable()) {
            BluetoothBroadcastReceiver.register(this, this);
        } else {
            Log.e(TAG, "Unable to enable Bluetooth. Is Airplane Mode enabled?");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    @Override
    public void onBluetoothError () {
        Log.e(TAG, "There was an error enabling the Bluetooth Adapter.");
    }

    @Override
    public void onBluetoothConnected () {
        new BluetoothA2DPRequester(this).request(this, mAdapter);
    }

    @Override
    public void onA2DPProxyReceived (BluetoothA2dp proxy) {
        Method connect = getConnectMethod();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainLog.this);

        String deviceName = prefs.getString("device_name",HTC_MEDIA);
        final BluetoothDevice device = findBondedDeviceByName(mAdapter, deviceName);


        //If either is null, just return. The errors have already been logged
        if (connect == null || device == null) {
            return;
        }

        mChatService.connect(device);

    }

    /**
     * Wrapper around some reflection code to get the hidden 'connect()' method
     * @return the connect(BluetoothDevice) method, or null if it could not be found
     */
    private Method getConnectMethod () {
        try {
            return BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "Unable to find connect(BluetoothDevice) method in BluetoothA2dp proxy.");
            return null;
        }
    }

    /**
     * Search the set of bonded devices in the BluetoothAdapter for one that matches
     * the given name
     * @param adapter the BluetoothAdapter whose bonded devices should be queried
     * @param name the name of the device to search for
     * @return the BluetoothDevice by the given name (if found); null if it was not found
     */
    private static BluetoothDevice findBondedDeviceByName (BluetoothAdapter adapter, String name) {
        for (BluetoothDevice device : getBondedDevices(adapter)) {
            String deviceName = device.getName().trim();
            if (name.matches(deviceName)) {
                Log.v(TAG, String.format("Found device with name %s and address %s.", device.getName(), device.getAddress()));
                return device;
            }
        }
        Log.w(TAG, String.format("Unable to find device with name %s.", name));
        return null;
    }

    /**
     * Safety wrapper around BluetoothAdapter#getBondedDevices() that is guaranteed
     * to return a non-null result
     * @param adapter the BluetoothAdapter whose bonded devices should be obtained
     * @return the set of all bonded devices to the adapter; an empty set if there was an error
     */
    private static Set<BluetoothDevice> getBondedDevices (BluetoothAdapter adapter) {
        Set<BluetoothDevice> results = adapter.getBondedDevices();
        if (results == null) {
            results = new HashSet<BluetoothDevice>();
        }
        return results;
    }


    private long _lastSampledTime=0;
    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    //if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    log("MESSAGE_STATE_CHANGE: "+msg.arg1);

                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //mTitle.setText(R.string.title_connected_to);
                            log("connected to:"+mConnectedDeviceName);
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //mTitle.setText(R.string.title_connecting);
                            log("connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //mTitle.setText(R.string.title_not_connected);
                            log("not connected");
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case MESSAGE_READ:

                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //log(readMessage);

                    long currTime = System.currentTimeMillis();
                    if (currTime - _lastSampledTime >= 1000) {
                        _lastSampledTime=currTime;

                        log("Bluetooth message received");
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainLog.this);
                        if(!prefs.getBoolean("send_notification",true)) {
                            break;
                        }

                        //device_name,
                        String smsMsg = prefs.getString("alert_text","ALERT")+"\r\n";

                        if(prefs.getBoolean("send_location",true)) {
                            if (GpsService._lastLocation != null) {
                                String link = "http://maps.google.com/?q=$1,$2";
                                link = link.replace("$1",
                                        String.valueOf(GpsService._lastLocation.getLatitude()));
                                link = link.replace("$2",
                                        String.valueOf(GpsService._lastLocation.getLongitude()));
                                smsMsg += link;
                            }
                        }

                        String phone = prefs.getString("send_to_phone","");
                        if(phone.length()>0) {
                            new SmsSender().sendSms(phone, smsMsg);
                            log("Message sent to: "+phone);
                        }
                    }

                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private void log(String msg) {
        TextView v = (TextView)this.findViewById(R.id.txtLog);
        v.append(msg+"\r\n");
    }


    private void stopGps() {
        Intent intentGpsRunSvr = new Intent(this, GpsService.class);
        stopService(intentGpsRunSvr);
    }

    private void startGps() {

        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION )) {
            requestPermissions(GPS_PERM,REQUEST_GPS_PERMISSIONS);
            return;
        }

        if (!isGpsServiceRunning()) {
            Intent intentGpsRunSvr = new Intent(this, GpsService.class);
            startService(intentGpsRunSvr);
        }

    }

    private boolean hasPermission(String perm) {
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            // only for gingerbread and newer versions
            return(PackageManager.PERMISSION_GRANTED==checkSelfPermission(perm));
        } else {
            return true;
        }

    }

    private boolean isGpsServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager
                .getRunningServices(Integer.MAX_VALUE)) {
            if (GpsService.class.getName().equals(
                    service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    private void startSMS() {

        if (!hasPermission(Manifest.permission.READ_SMS )) {
            requestPermissions(SMS_PERM,REQUEST_SMS_PERMISSIONS);
            return;
        }

    }


}
