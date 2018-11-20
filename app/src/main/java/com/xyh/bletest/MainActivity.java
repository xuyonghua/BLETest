package com.xyh.bletest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_ACCESS_COARSE_LOCATION = 0x00;
    private static final String TAG = "MainActivity";
    private static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805F9B34FB";
    private static final String UUID_SERVICE = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String UUID_CHARACTER_NOTIFY1 = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_CHARACTER_NOTIFY2 = "6e400004-b5a3-f393-e0a9-e50e24dcca9e";
    private static final String UUID_CHARACTER_NOTIFY3 = "6e400005-b5a3-f393-e0a9-e50e24dcca9e";
    private RecyclerView recyclerView;
    private DeviceAdapter adapter;
    private List<BluetoothDevice> deviceList;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristic01;
    private BluetoothGattCharacteristic characteristic02;
    private BluetoothGattCharacteristic characteristic03;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initBLE();
        initViews();
    }

    private void initBLE() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_ACCESS_COARSE_LOCATION);
        }
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }

        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }


    }

    private void initViews() {
        deviceList = new ArrayList<>();
        recyclerView = findViewById(R.id.deviceList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DeviceAdapter(deviceList);
        recyclerView.setAdapter(adapter);

        adapter.setItemClickListener(new DeviceAdapter.OnItemClickListener() {
            @Override
            public void onClick(int position) {
                connect(position);
            }
        });


        findViewById(R.id.scan).setOnClickListener(this);
        findViewById(R.id.disconnect).setOnClickListener(this);
    }

    private void connect(int position) {
        mBluetoothGatt = deviceList.get(position).connectGatt(this, false, mGattCallback);
    }

    private void setNotification(BluetoothGatt mGatt, BluetoothGattCharacteristic mCharacteristic, boolean mEnable) {
        if (mCharacteristic == null) {
            return;
        }
        //设置为Notify,并写入描述符
        mGatt.setCharacteristicNotification(mCharacteristic, mEnable);
        BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mGatt.writeDescriptor(descriptor);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d("MainActivity", "onConnectionStateChange: " + newState);
            if (newState == STATE_CONNECTED) {
                //通过mBluetoothGatt.discoverServices()，我们就可以获取到ble设备的所有Services。
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(UUID_SERVICE));
                characteristic01 = service.getCharacteristic(UUID.fromString(UUID_CHARACTER_NOTIFY1));
                characteristic02 = service.getCharacteristic(UUID.fromString(UUID_CHARACTER_NOTIFY2));
                characteristic03 = service.getCharacteristic(UUID.fromString(UUID_CHARACTER_NOTIFY3));
                setNotification(gatt, characteristic01, true);  //先开启1号通道
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            Log.d(TAG, "onCharacteristicRead: " + characteristic.toString());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
//            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "onCharacteristicChanged: " + new String(characteristic.getValue()));
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite: 回调");
            BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
            String uuid = characteristic.getUuid().toString();
            if (uuid.equals(UUID_CHARACTER_NOTIFY1)) {
                setNotification(gatt, characteristic02, true);
            } else if (uuid.equals(UUID_CHARACTER_NOTIFY2)) {
                setNotification(gatt, characteristic03, true);
            }
        }

    };

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.scan:
                // Stops scanning after a pre-defined scan period.
                bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                bluetoothLeScanner.startScan(scanCallback);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        bluetoothLeScanner.stopScan(scanCallback);
                    }
                }, SCAN_PERIOD);
                break;
            case R.id.disconnect:
                mBluetoothGatt.disconnect();
                break;
            default:
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //发现新设备回调该方法
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                //过滤掉其他设备
                if (device.getName() != null && !deviceList.contains(device)) {
                    deviceList.add(device);
                    adapter.notifyDataSetChanged();
                }
            }

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ACCESS_COARSE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                Toast.makeText(MainActivity.this, "权限被拒绝", Toast.LENGTH_LONG).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void finish() {
        super.finish();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
}
