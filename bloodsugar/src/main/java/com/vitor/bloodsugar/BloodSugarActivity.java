package com.vitor.bloodsugar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.vitor.bloodsugar.adapters.ReadingAdapter;

import org.maniteja.com.synclib.helper.Communicator;
import org.maniteja.com.synclib.helper.HelperC;
import org.maniteja.com.synclib.helper.SerializeUUID;
import org.maniteja.com.synclib.helper.SyncLib;
import org.maniteja.com.synclib.helper.Util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;

public class BloodSugarActivity extends AppCompatActivity implements Communicator{

    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    Util util;
    private String mDeviceAddress;
    Animation animation;

    ActionBar mActionBar;

    //Action bar
    ImageView batteryIcon;
    ImageView bluetoothIcon;
    TextView connectionLabel;

    public static boolean mConnected = false;
    public static boolean devTestStarted;

    Button startTest, stopTest, writeData, getData;
    TextView logDisplay,offlineTitle;
    RecyclerView readingrecycler;

    BloodSugarActivity activity_home;
    InputStream ins;
    SerializeUUID serializeUUID;

    SyncLib syncLib;

    Communicator communicator=this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_sugar);

        util = new Util(this, this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(null);
        toolbar.setContentInsetsAbsolute(0, 0);
        setSupportActionBar(toolbar);

        toolbar.setOnTouchListener(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                try
                {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                } catch (Exception crap)
                {
                    crap.printStackTrace();

                }
                return false;
            }
        });

        ins = getResources().openRawResource(R.raw.synclibserialize);

        serializeUUID = new SerializeUUID();
        serializeUUID.readFile(ins);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayShowCustomEnabled(true);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.bloodsugar_action_bar, null);

        ImageView menuIcon = (ImageView) mCustomView.findViewById(R.id.menuIcon);
        menuIcon.setVisibility(View.GONE);

        connectionLabel = (TextView) mCustomView.findViewById(R.id.connectionLabel);
        connectionLabel.setText("Biosense Blood Sugar Test");

        batteryIcon = (ImageView) mCustomView.findViewById(R.id.batteryIcon);
        bluetoothIcon = (ImageView) mCustomView.findViewById(R.id.bluetoothIcon);
        bluetoothIcon.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                syncLib.setmDeviceAddress(mDeviceAddress);
//                Toast.makeText(activity_home, "Device Address : "+mDeviceAddress, Toast.LENGTH_SHORT).show();
                syncLib.connectOrDisconnect();
            }
        });

        mActionBar.setCustomView(mCustomView);

        activity_home = BloodSugarActivity.this;

        //communicator = (Communicator) activity_home;

        startTest = (Button) findViewById(R.id.startTest);
        startTest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mConnected)
                {
                    syncLib.startTest();
                } else
                {
                    syncLib.startReceiver();

                    if(mConnected)
                        Toast.makeText(getApplicationContext(), "Please Connect to Device!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        stopTest = (Button) findViewById(R.id.stopTest);
        stopTest.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mConnected)
                {
                    if(devTestStarted)
                    {
                        try
                        {
                            syncLib.stopTest();
                            logDisplay.setText("Start the test.");
                        } catch (Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), "Test Not Started!", Toast.LENGTH_SHORT).show();
                    }
                } else
                {
                    Toast.makeText(getApplicationContext(), "Please Connect to Device!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        writeData = (Button) findViewById(R.id.writeData);
        writeData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
//                if (mConnected)
//                {
//                    IntentIntegrator integrator = new IntentIntegrator(activity_home);
//                    integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
//                    integrator.setPrompt("scan");
//                    integrator.setCameraId(0);
//
//                    integrator.setBeepEnabled(true);
//                    integrator.setBarcodeImageEnabled(true);
//                    integrator.initiateScan();
//                    integrator.setOrientationLocked(false);
//                } else
//                {
//                    Toast.makeText(getApplicationContext(), "Please Connect to Device!", Toast.LENGTH_SHORT).show();
//                }
            }
        });

        getData = (Button) findViewById(R.id.getData);
        getData.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mConnected)
                {
                    syncLib.notifyGetData();
                } else
                {
                    Toast.makeText(getApplicationContext(), "Please Connect to Device!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        logDisplay = (TextView) findViewById(R.id.logDisplay);
        offlineTitle = (TextView) findViewById(R.id.offlineTitle);

        readingrecycler = (RecyclerView) findViewById(R.id.readingrecycler);

        final Intent intent = getIntent();
        if (intent.getStringExtra(EXTRAS_DEVICE_ADDRESS) != null)
        {
            mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
            util.putString(HelperC.key_mybluetoothaddress, mDeviceAddress);
        } else
        {
            mDeviceAddress = util.readString(HelperC.key_autoconnectaddress, "");
        }

        syncLib = new SyncLib(communicator,this,BloodSugarActivity.this,serializeUUID,mDeviceAddress);

        util.print("Scan List Address :Main " + mDeviceAddress + "::" + util.readString(HelperC.key_mybluetoothaddress, "") + " - " + mDeviceAddress.length());

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        System.out.println("device id");
        super.onActivityResult(requestCode, resultCode, data);
//        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
//        if (result != null)
//        {
//            if (result == null)
//            {
//
//                Toast.makeText(this, getResources().getString(R.string.canceled), Toast.LENGTH_LONG).show();
//            } else
//            {
//                if(result.getContents() != null)
//                {
//                    syncLib.writeCalibData(result.getContents());
//                }
//            }
//        } else
//        {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if(!mConnected)
        {
            syncLib.startReceiver();
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        syncLib.stopReceiver();
    }

//    @Override
//    public void onBackPressed()
//    {
//        setSwitchActivity();
//    }

    @Override
    public boolean go(String text)
    {
        return  false;
    }

    @Override
    public void setLog(String text)
    {
        logDisplay.setText(text);

        if(text.contains("Result")){
//            Toast.makeText(activity_home, "RESULT : " +text, Toast.LENGTH_SHORT).show();

            Intent returnIntent = new Intent();
            returnIntent.putExtra("BloodSugar",text);
            setResult(Activity.RESULT_OK,returnIntent);
            finish();
        }
    }

    @Override
    public void testStarted(boolean testStarted)
    {
        devTestStarted = testStarted;
    }

    @Override
    public void stopNotiFication()
    {

    }

    @Override
    public void setConnectionStatus(String s, boolean connectionStatus)
    {
        mConnected = connectionStatus;
        if(mConnected)
        {
            bluetoothIcon.setBackgroundResource(R.drawable.connect);
            batteryIcon.setVisibility(View.VISIBLE);
        }
        else
        {
            batteryIcon.setVisibility(View.INVISIBLE);
            logDisplay.setText("Disconnected");
            bluetoothIcon.setBackgroundResource(R.drawable.disconnect);
        }
    }

    //This method is used to navigate between the activities
    @Override
    public void setSwitchActivity()
    {
//        Intent intent = new Intent(getApplicationContext(), Activity_ScanList.class);
//        intent.putExtra("flag", 2);
//        startActivity(intent);
//        finish();
    }


    @Override
    public void setBatteryLevel(int value)
    {
        if (value > 25 && value < 33)
        {
            if (animation != null)
                animation.cancel();
            batteryIcon.setBackgroundResource(R.drawable.battery1);
        } else if (value >= 33 && value < 50)
        {
            if (animation != null)
                animation.cancel();
            batteryIcon.setBackgroundResource(R.drawable.battery2);
        } else if (value >= 50)
        {
            if (animation != null)
                animation.cancel();
            batteryIcon.setBackgroundResource(R.drawable.battery3);
        } else if (value > 0 && value <= 25)
        {
            batteryIcon.setBackgroundResource(R.drawable.battery0);
            animation = new AlphaAnimation(1, 0);
            animation.setDuration(400);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setRepeatMode(Animation.REVERSE);
            batteryIcon.startAnimation(animation);
        }
    }

    @Override
    public void setManufacturerName(String s)
    {
        //Manufaturer Name
    }

    @Override
    public void setSerialNumber(String s)
    {
        //Serial Number
    }

    @Override
    public void setModelNumber(String s)
    {
        //Manufacture Date
    }

    @Override
    public void getOfflineResults(ArrayList<String> arrayList)
    {
        if(arrayList.size()>0)
        {
            Collections.reverse(arrayList);
            offlineTitle.setText("Offline Results");
            ReadingAdapter readingAdapter = new ReadingAdapter(arrayList);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            readingrecycler.setLayoutManager(mLayoutManager);
            readingrecycler.setItemAnimator(new DefaultItemAnimator());
            readingrecycler.setAdapter(readingAdapter);
        }
    }
}
