package com.evans.qontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.evans.qontrol.adapters.DeviceAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConnectionActivity extends AppCompatActivity {

    @BindView(R.id.button_connection)
    Button mConnectionBtn;
    @BindString(R.string.dialog_paired_devices_title)
    String mPairedDevicesTitle;
    @BindString(R.string.dialog_discovered_devices_title)
    String mDiscoveredDevicesTitle;
    @BindString(R.string.dialog_discover_devices_text)
    String mDiscoverDevices;
    @BindString(R.string.dialog_cancel_connection_text)
    String mCancelConnection;

    public final static String EXTRA_DEVICE_NAME = "com.evans.qontrol.DEVICE_NAME";
    public final static String EXTRA_DEVICE_ADDRESS = "com.evans.qontrol.DEVICE_ADDRESS";

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private List<Map<String, String>> mDiscoveredDevicesList = new ArrayList<>();
    private final DeviceAdapter mDiscoveredDevicesAdapter = new DeviceAdapter(mDiscoveredDevicesList);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        ButterKnife.bind(this);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }

    // listens for discovered devices
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceMACAddress = device.getAddress();

                Map<String, String> map = new HashMap<>();
                map.put("device_name", deviceName);
                map.put("device_address", deviceMACAddress);
                mDiscoveredDevicesList.add(map);

                mDiscoveredDevicesAdapter.notifyItemInserted(0);
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("DISCOVERY", "Discovery finished");
            }
        }
    };

    @OnClick(R.id.button_connection)
    public void startBTSetup() {
        bluetoothSetup();
    }

    /**
     * Check if bluetooth is enabled or not. If enabled, start discovery.
     */
    private void bluetoothSetup() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // if device does not support bluetooth
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Sorry. Your device does not support bluetooth.", Toast.LENGTH_SHORT).show();
        }

        // if bluetooth is not enabled, make request for it to be enabled
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast.makeText(this, "Bluetooth is enabled.", Toast.LENGTH_SHORT).show();
            getPairedDevices();
        }
    }

    /**
     * Get devices already paired to this device.
     */
    private void getPairedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            final List<Map<String, String>> pairedDevicesList = new ArrayList<>();

            for (BluetoothDevice btDevice : pairedDevices) {
                String deviceName = btDevice.getName();
                String deviceMACAddress = btDevice.getAddress();

                Map<String, String> map = new HashMap<>();
                map.put("device_name", deviceName);
                map.put("device_address", deviceMACAddress);

                pairedDevicesList.add(map);
            }

            final DeviceAdapter pairedDevicesAdapter = new DeviceAdapter(pairedDevicesList);
            pairedDevicesAdapter.setCallback(new DeviceAdapter.Callback() {
                @Override
                public void onItemClicked(int index) {
                    Map<String, String> map = pairedDevicesList.get(index);
                    startConnection(map.get("device_name"), map.get("device_address"));
                }

                @Override
                public void onButtonClicked(int index) {
                    Map<String, String> map = pairedDevicesList.get(index);
                    startConnection(map.get("device_name"), map.get("device_address"));
                }
            });

            displayDevicesDialog(mPairedDevicesTitle, pairedDevicesAdapter, false);
        } else {
            // no paired devices, start discovery
            doDiscovery();
        }
    }

    /**
     * Start discovery of nearby devices. If found, add them to adapter.
     */
    private void doDiscovery() {
        if (mDiscoveredDevicesList.size() > 0) {
            mDiscoveredDevicesList.clear();
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();

        mDiscoveredDevicesAdapter.setCallback(new DeviceAdapter.Callback() {
            @Override
            public void onItemClicked(int index) {
                Map<String, String> map = mDiscoveredDevicesList.get(index);
                startConnection(map.get("device_name"), map.get("device_address"));
            }

            @Override
            public void onButtonClicked(int index) {
                Map<String, String> map = mDiscoveredDevicesList.get(index);
                startConnection(map.get("device_name"), map.get("device_address"));
            }
        });

        displayDevicesDialog(mDiscoveredDevicesTitle, mDiscoveredDevicesAdapter, true);
    }

    /**
     * Display dialog showing either the paired devices or discovered devices.
     * @param title The String to be used as the title of the dialog.
     * @param adapter The DeviceAdapter to be used to display the relevant devices.
     * @param isDiscovering The Boolean to check whether dialog is in the context of discovering.
     */
    private void displayDevicesDialog(String title, DeviceAdapter adapter, final Boolean isDiscovering) {
        new MaterialDialog.Builder(this)
                .title(title)
                .adapter(adapter, null)
                .positiveText(mDiscoverDevices)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Log.d("POSITIVE", "Start Discovery");
                        doDiscovery();
                    }
                })
                .negativeText(mCancelConnection)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .cancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (isDiscovering) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                    }
                })
                .dismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (isDiscovering) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                    }
                })
                .show();
    }

    /**
     * Initiate connection with the selected device.
     * @param deviceName The String of the name of the device to connect with.
     * @param deviceAddress The String of the address of the device to connect with.
     */
    private void startConnection(String deviceName, String deviceAddress) {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // handle connection: go to new activity
//        mQBluetoothService.connect(device);

        Intent intent = new Intent(this, DeviceActivity.class);
        intent.putExtra(EXTRA_DEVICE_NAME, deviceName);
        intent.putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // request to enable bluetooth granted
                if (resultCode == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth successfully enabled.", Toast.LENGTH_SHORT).show();
                    getPairedDevices();
                } else {
                    Toast.makeText(this, "Bluetooth needs to be enabled in order for the app to work.", Toast.LENGTH_LONG).show();
                }

            default:
                // handle this
        }
    }
}
