package com.vitor.bloodsugar;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.maniteja.com.synclib.helper.Communicator;
import org.maniteja.com.synclib.helper.SerializeUUID;
import org.maniteja.com.synclib.helper.SyncLib;
import org.maniteja.com.synclib.helper.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;


public class BloodSugar extends Service {

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    Util util;
    private String mDeviceAddress;
    Animation animation;

    ActionBar mActionBar;


    public static boolean mConnected = false;
    public static boolean devTestStarted;

    Button startTest, stopTest, writeData, getData;

    InputStream ins;
    SerializeUUID serializeUUID;

    SyncLib syncLib;
    Communicator communicator= (Communicator) this;

    public BloodSugar() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        if(!mConnected)
        {
            syncLib.startReceiver();
        }
//        Toast.makeText(this, "BloodSugarService onCreate called", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //getting systems default ringtone
        if (intent!=null) {
            String BloodSugarDeviceID = intent.getStringExtra("BloodSugarDeviceID");


            ins = getResources().openRawResource(R.raw.synclibserialize);

            serializeUUID = new SerializeUUID();
            serializeUUID.readFile(ins);
            syncLib.setmDeviceAddress(BloodSugarDeviceID);
            syncLib.connectOrDisconnect();


            if (mConnected)
            {
                syncLib.startTest();
            } else
            {
                Toast.makeText(getApplicationContext(), "Please Connect to Device!", Toast.LENGTH_SHORT).show();
            }
//            Toast.makeText(this, "onStartCommand called : " + BloodSugarDeviceID, Toast.LENGTH_LONG).show();
            // do something with the value here
        } else {

        }
//        AddEvents();


        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }


    public void setConnectionStatus(String s, boolean connectionStatus)
    {
        mConnected = connectionStatus;
        if(mConnected)
        {
//            bluetoothIcon.setBackgroundResource(R.drawable.connect);
//            batteryIcon.setVisibility(View.VISIBLE);
        }
        else
        {
//            batteryIcon.setVisibility(View.INVISIBLE);
//            bluetoothIcon.setBackgroundResource(R.drawable.disconnect);
        }
    }


    public void setBatteryLevel(int value)
    {
        if (value > 25 && value < 33)
        {
            if (animation != null)
                animation.cancel();
//            batteryIcon.setBackgroundResource(R.drawable.battery1);
        } else if (value >= 33 && value < 50)
        {
            if (animation != null)
                animation.cancel();
//            batteryIcon.setBackgroundResource(R.drawable.battery2);
        } else if (value >= 50)
        {
            if (animation != null)
                animation.cancel();
//            batteryIcon.setBackgroundResource(R.drawable.battery3);
        } else if (value > 0 && value <= 25)
        {
//            batteryIcon.setBackgroundResource(R.drawable.battery0);
            animation = new AlphaAnimation(1, 0);
            animation.setDuration(400);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
//            batteryIcon.startAnimation(animation);
        }
    }

//    @Override
    public void setManufacturerName(String s)
    {
        //Manufaturer Name
    }

//    @Override
    public void setSerialNumber(String s)
    {
        //Serial Number
    }

//    @Override
    public void setModelNumber(String s)
    {
        //Manufacture Date
    }

//    @Override
    public void getOfflineResults(ArrayList<String> arrayList)
    {
        if(arrayList.size()>0)
        {
            Collections.reverse(arrayList);
//            offlineTitle.setText("Offline Results");
//            ReadingAdapter readingAdapter = new ReadingAdapter(arrayList);
//            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
//            readingrecycler.setLayoutManager(mLayoutManager);
//            readingrecycler.setItemAnimator(new DefaultItemAnimator());
//            readingrecycler.setAdapter(readingAdapter);
        }
    }




    @Override
    public void onDestroy() {
//        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();

    }


    private void sendDataToActivity(String stPEF , String stFEV1, String stFEV6, String stFEV1_6, String stFEF1) {
        Intent intent = new Intent("BloodSugarData");
        intent.putExtra("PEF", stPEF);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
