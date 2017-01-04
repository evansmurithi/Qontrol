package com.evans.qontrol.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.evans.qontrol.R;

import java.util.List;
import java.util.Map;

/**
 * Created by evans on 12/27/16.
 *
 * Adapter for devices (paired or discovered) in the dialog.
 */

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceVH> {

    public interface Callback {
        void onItemClicked(int index);

        void onButtonClicked(int index);
    }

    private final List<Map<String, String>> mPairedDevices;
    private Callback mCallback;

    public DeviceAdapter(List<Map<String, String>> pairedDevices) {
        this.mPairedDevices = pairedDevices;
    }

    public void setCallback(Callback mCallback) {
        this.mCallback = mCallback;
    }

    @Override
    public DeviceVH onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view  = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.dialog_devices, parent, false);

        return new DeviceVH(view, this);
    }

    @Override
    public void onBindViewHolder(DeviceVH holder, int position) {
        holder.deviceName.setText(mPairedDevices.get(position).get("device_name"));
        holder.deviceAddress.setText(mPairedDevices.get(position).get("device_address"));
        holder.connectBtn.setTag(position);
    }

    @Override
    public int getItemCount() {
        return mPairedDevices.size();
    }

    public static class DeviceVH extends RecyclerView.ViewHolder implements View.OnClickListener {

        final TextView deviceName;
        final TextView deviceAddress;
        final Button connectBtn;
        final DeviceAdapter adapter;

        public DeviceVH(View itemView, DeviceAdapter adapter) {
            super(itemView);

            deviceName = (TextView) itemView.findViewById(R.id.text_device_name);
            deviceAddress = (TextView) itemView.findViewById(R.id.text_device_address);
            connectBtn = (Button) itemView.findViewById(R.id.button_device_connect);

            this.adapter = adapter;
            itemView.setOnClickListener(this);
            connectBtn.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (adapter.mCallback == null) {
                return;
            }

            if (view instanceof Button) {
                adapter.mCallback.onButtonClicked(getAdapterPosition());
            } else {
                adapter.mCallback.onItemClicked(getAdapterPosition());
            }
        }
    }
}
