package com.vitor.weighscale;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeighScaleChipseaService extends Service {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    public WeighScaleChipseaService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
//        Toast.makeText(this, "WeighScaleChipseaService onCreate called", Toast.LENGTH_LONG).show();
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Toast.makeText(this, "Weigh Scale Library : Enable Bluetooth", Toast.LENGTH_SHORT).show();
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect peripherals.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
//                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                        Toast.makeText(WeighScaleChipseaService.this, "Weigh Scale Library : Enable Location", Toast.LENGTH_SHORT).show();
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //getting systems default ringtone
        if (intent!=null) {
            String SpirometerDeviceID = intent.getStringExtra("SpirometerDeviceID");
//            Toast.makeText(this, "onStartCommand called : " + SpirometerDeviceID, Toast.LENGTH_LONG).show();
            // do something with the value here
        } else {

        }
        startScanning();


        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }


    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            final byte[] scanRecord = result.getScanRecord().getBytes();
            if(result.getScanRecord()!=null && result.getScanRecord().getDeviceName()!=null && result.getScanRecord().getDeviceName().contains("Chipsea")){
//                if(result.getScanRecord()!=null && result.getDevice().getAddress().equals("CB:8E:CD:50:AB:CD")){
                printScanRecord(result.getScanRecord().getBytes());
//                tv_peripheral.append("\n" +"Device Name: " + result.toString()  +"\n"+"SCANRECORD : "+scanRecord.toString()+"\n");
                Log.i("WS","\n" +"Device Name: " + result.toString()  +"\n"+"SCANRECORD : "+result.getScanRecord().getBytes().toString()+"\n");
            }

            // auto scroll for text view
//            final int scrollAmount = tv_peripheral.getLayout().getLineTop(tv_peripheral.getLineCount()) - tv_peripheral.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
//            if (scrollAmount > 0)
//                tv_peripheral.scrollTo(0, scrollAmount);
        }
    };


    public void printScanRecord (byte[] scanRecord) {
        // Simply print all raw bytes
        try {
            String decodedRecord = new String(scanRecord,"UTF-8");
            Log.d("DEBUG","decoded String : " + ByteArrayToString(scanRecord));
            int val = scanRecord[11];
            String weight = String.valueOf(val);
            String lastVal = weight.substring(weight.length() - 1);
            String deletedVal = weight.substring(0, weight.length() - 1);
            weight = deletedVal+"."+lastVal;



//            tv_weight.setText("Weight : "+weight);
            sendDataToActivity(weight);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // Parse data bytes into individual records
        List<AdRecord> records = AdRecord.parseScanRecord(scanRecord);

        // Print individual records
        if (records.size() == 0) {
            Log.i("DEBUG", "Scan Record Empty");
        } else {
            Log.i("DEBUG", "Scan Record: " + TextUtils.join(",", records));
        }
    }

    public static String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");

        return hex.toString();
    }


    public static class AdRecord {
        public AdRecord(int length, int type, byte[] data) {
            String decodedRecord = "";
            try {
                decodedRecord = new String(data,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.d("DEBUG", "Length: " + length + " Type : " + type + " Data : " + ByteArrayToString(data));
        }

        public static List<AdRecord> parseScanRecord(byte[] scanRecord) {
            List<AdRecord> records = new ArrayList<AdRecord>();

            int index = 0;
            while (index < scanRecord.length) {
                int length = scanRecord[index++];
                //Done once we run out of records
                if (length == 0) break;

                int type = scanRecord[index];
                //Done if our record isn't a valid type
                if (type == 0) break;

                byte[] data = Arrays.copyOfRange(scanRecord, index+1, index+length);

                records.add(new AdRecord(length, type, data));
                //Advance
                index += length;
            }
            return records;
        }
    }


    public void startScanning() {
        System.out.println("start scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    private void sendDataToActivity(String weight) {
        Intent intent = new Intent("WeighScaleChipseaData");
        intent.putExtra("weight", weight);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public void onDestroy() {
        super.onDestroy();
        btScanner.stopScan(leScanCallback);
    }



}
