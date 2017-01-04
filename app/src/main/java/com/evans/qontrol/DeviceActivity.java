package com.evans.qontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.evans.qontrol.services.QBluetoothService;
import com.wang.avi.AVLoadingIndicatorView;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;

public class DeviceActivity extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.text_device_status)
    TextView mDeviceStatus;
    @BindString(R.string.text_device_connecting)
    String mDeviceConnecting;
    @BindString(R.string.text_device_connected)
    String mDeviceConnected;
    @BindView(R.id.avi_connecting)
    AVLoadingIndicatorView mAVIConnecting;

    // Message types sent from QBluetoothService Handler
    public static final int MSG_CONNECTION_STATUS = 1;

    // Key names received from QBluetoothService Handler
    public static final String CONNECTION_STATUS = "connection_status";

    // Connection statuses
    public static final String CONN_SUCCESSFUL = "success";
    public static final String CONN_FAILED = "failed";
    public static final String CONN_LOST = "lost";

    private BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private QBluetoothService mQBluetoothService = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        String deviceName = intent.getStringExtra(ConnectionActivity.EXTRA_DEVICE_NAME);
        String deviceAddress = intent.getStringExtra(ConnectionActivity.EXTRA_DEVICE_ADDRESS);

        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(deviceName);
        }

        mDeviceStatus.setText(mDeviceConnecting);
        mAVIConnecting.smoothToShow();

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        mQBluetoothService = new QBluetoothService(this);
        mQBluetoothService.connect(device);
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    @Override
    protected void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver, new IntentFilter("QEvents"));
        super.onResume();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(CONNECTION_STATUS);

            Log.d("QSERVICE", "Message: " + message);

            if (CONN_SUCCESSFUL.equals(message)) {
                mDeviceStatus.setText(mDeviceConnected);
                mAVIConnecting.smoothToHide();
            } else if (CONN_FAILED.equals(message)) {
                mDeviceStatus.setText("Connection failed.");
                mAVIConnecting.smoothToHide();
            } else if (CONN_LOST.equals(message)) {
                mDeviceStatus.setText("Connection lost.");
                mAVIConnecting.smoothToHide();
            }
        }
    };

}
