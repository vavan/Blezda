package pasa.blezda;


import android.app.Activity;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Dave Smith
 * Date: 11/13/14
 * ClientActivity
 */
public class ClientActivity extends Activity {
    private static final String TAG = "ClientActivity";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private SparseArray<BluetoothDevice> mDevices;

    private BluetoothGatt mConnectedGatt;

    private Handler mHandler = new Handler();

    /* Client UI elements */
    private TextView mLatestValue;
    private TextView mCurrentOffset;

    private final String SCAN_START = "pasa.ble.start";
    private final String SCAN_DONE = "pasa.ble.done";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        mLatestValue = (TextView) findViewById(R.id.latest_value);
//        mCurrentOffset = (TextView) findViewById(R.id.offset_date);
//        updateDateText(0);

        /*
         * Bluetooth in Android 4.3+ is accessed via the BluetoothManager, rather than
         * the old static BluetoothAdapter.getInstance()
         */
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        mDevices = new SparseArray<BluetoothDevice>();

        IntentFilter filter = new IntentFilter();
        filter.addAction(SCAN_START);
        filter.addAction(SCAN_DONE);
        registerReceiver(scanReceiver, filter);

        Intent start = new Intent(SCAN_START);
        sendBroadcast(start);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
            finish();
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "No LE Support.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        unregisterReceiver(scanReceiver);
        //Stop any active scans
        stopScan();
        //Disconnect from any active connection
        if (mConnectedGatt != null) {
            mConnectedGatt.disconnect();
            mConnectedGatt = null;
        }
    }

    private byte command_emulation = 0;
    private byte argument_emulation = 0;

    /*
     * Select a new time to set as the base offset
     * on the GATT Server. Then write to the characteristic.
     */
    public void onUpdateClick(View v) {
        if (mConnectedGatt != null) {

            //*********************************************************************************
            //Vova. Send command to the server
            command_emulation += 1;
            argument_emulation += 2;

            BluetoothGattCharacteristic characteristic = mConnectedGatt
                    .getService(DeviceProfile.SERVICE_UUID)
                    .getCharacteristic(DeviceProfile.CHARACTERISTIC_COMMAND_UUID);
            byte[] value = {command_emulation, argument_emulation};
            Log.d(TAG, "Writing value of size "+value.length);
            characteristic.setValue(value);

            mConnectedGatt.writeCharacteristic(characteristic);
            //*********************************************************************************
        }
    }


    /*
     * Begin a scan for new servers that advertise our
     * matching service.
     */
    private void startScan() {
        //Scan for devices advertising our custom service
        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(DeviceProfile.SERVICE_UUID))
                .build();
        ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
        filters.add(scanFilter);

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
        mBluetoothAdapter.getBluetoothLeScanner().startScan(filters, settings, mScanCallback);
    }

    /*
     * Terminate any active scans
     */
    private void stopScan() {
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
    }

    /*
     * Callback handles results from new devices that appear
     * during a scan. Batch results appear when scan delay
     * filters are enabled.
     */
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.d(TAG, "onScanResult");

            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Log.d(TAG, "onBatchScanResults: "+results.size()+" results");

            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.w(TAG, "LE Scan Failed: "+errorCode);
        }

        private void processResult(ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.i(TAG, "New LE Device: " + device.getName() + " @ " + result.getRssi());
            //Add it to the collection
            mDevices.put(device.hashCode(), device);

            stopScan();

            Intent done = new Intent(SCAN_DONE);
            sendBroadcast(done);
        }
    };

    /*
     * Callback handles GATT client events, such as results from
     * reading or writing a characteristic value on the server.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange "
                    + DeviceProfile.getStatusDescription(status) + " "
                    + DeviceProfile.getStateDescription(newState));

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered:");

            for (BluetoothGattService service : gatt.getServices()) {
                Log.d(TAG, "Service: " + service.getUuid());

                if (DeviceProfile.SERVICE_UUID.equals(service.getUuid())) {
                    //Read the current characteristic's value
                    gatt.readCharacteristic(service.getCharacteristic(DeviceProfile.CHARACTERISTIC_COMMAND_UUID));
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            final byte command = DeviceProfile.getCoomand(characteristic);
            final byte argument = DeviceProfile.getArgument(characteristic);

            if (DeviceProfile.CHARACTERISTIC_COMMAND_UUID.equals(characteristic.getUuid())) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mLatestValue.setText(">" + command + ", " + argument + "<");
                    }
                });

                //Register for further updates as notifications
                gatt.setCharacteristicNotification(characteristic, true);
            }


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            //VOVA get update from server
            //*************************************************************************************
            Log.i(TAG, "Notification of time characteristic changed on server.");
            final byte command = DeviceProfile.getCoomand(characteristic);
            final byte argument = DeviceProfile.getArgument(characteristic);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLatestValue.setText(">" + command + ", " + argument + "<");
                }
            });
            //*************************************************************************************
        }
    };

    private final BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SCAN_START.equals(action)) {
                startScan();
            } else if (SCAN_DONE.equals(action)) {
                BluetoothDevice device = mDevices.valueAt(0);
                Log.i(TAG, "Connecting to " + device.getName());
                /*
                 * Make a connection with the device using the special LE-specific
                 * connectGatt() method, passing in a callback for GATT events
                 */
                mConnectedGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
            }
        }
    };


}
