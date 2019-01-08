package com.vitor.vlib;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

public class SpirometerVitorActivity extends AppCompatActivity {

    private static final String TAG = "SensorService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "SensorServiceSecure";
    private static final String NAME_INSECURE = "SensorServiceInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID
            .fromString("00001101-0000-1000-8000-00805f9b34fb");
    private static final int BYTES_SIX = 6;
    private static final int BYTES_FIFTY_EIGHT = 58;
    private static final int HIDE = 1;
    private static final int SHOW = 2;

    // Member fields

    Handler mHandler;

    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    boolean realTime = false;
    int FEV1,FEV2,FEV3;
    int PEF1,PEF2,PEF3;

    int FEV11,FEV12,FEV13;
    int FEV61,FEV62,FEV63;
    int FEV1_61,FEV1_62,FEV1_63;
    int FEF1,FEF2,FEF3;

    String stPEF,stFEV1,stFEV6,stFEV1_6,stFEF1;
//    TextView pe;
//    TextView fe;

//    TextView tv_PEF,tv_FEV1,tv_FEV6,tv_FEV1_6,tv_FEF;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    // device

    public String res1 = "", res2 = "", res3 = "", res4 = "", res5 = "",
            res6 = "";
    String dname;
    String devicename;
    TextView txt;
//    ProgressDialog progressDialog1;
    BluetoothAdapter mAdapter;

    boolean check = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_spirometer_vitor);

        Log.e("", "WEIGHTTTTTTTTTTTTTTTTTTTTT 111111111111111111");


//        pe=(TextView) findViewById(R.id.txt_result1);
//        fe=(TextView) findViewById(R.id.txt_result2);

//        tv_PEF =(TextView) findViewById(R.id.tv_PEF);
//        tv_FEV1 =(TextView) findViewById(R.id.tv_FEV1);
//        tv_FEV6 =(TextView) findViewById(R.id.tv_FEV6);
//        tv_FEV1_6 =(TextView) findViewById(R.id.tv_FEV1_6);
//        tv_FEF =(TextView) findViewById(R.id.tv_FEF);
        AddEvents();
    }

    public void onDestroy() {
        super.onDestroy();
    }


    private void AddEvents()
    {
        // TODO Auto-generated method stub
        mAdapter = BluetoothAdapter.getDefaultAdapter();
//        progressDialog1 = ProgressDialog.show(SpirometerVitorActivity.this, "Info", "Please wait", true, false);
//        progressDialog1.setContentView(R.layout.customprocessdlg);
//
//        TextView text1 = (TextView) progressDialog1.findViewById(R.id.msgtxt);
//        text1.setText("Data Reading in Progress...");
        mState = STATE_NONE;
//        Thread th1;
//        th1 = new Thread()
//        {
//            @Override
//            public void run()
//            {
//                // TODO Auto-generated method stub
//                super.run();
//                try
//                {
//                    check = false;
//                    sleep(10000);
//                } catch (InterruptedException e)
//                {
//                    // TODO Auto-generated catch block
//                } finally
//                {
//                    progressDialog1.dismiss();
//                    // cancelmythread();
//                    if (check == false)
//                    {
//                        runOnUiThread(new Runnable()
//                        {
//                            public void run()
//                            {
//                                Context context = getApplicationContext();
//                                CharSequence text = "could not connect , please try again.........!";
//
//                                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
//                                toast.show();
//                            }
//                        });
//                    }
//                    check = true;
//                }
//            }
//        };
//        th1.start();

        Set<BluetoothDevice> bondedSet = mAdapter.getBondedDevices();
        Log.v("", "BluetoothDemo : bondedSet: " + bondedSet);

        int count = 0;
        if (bondedSet.size() > 0)
        {
            for (BluetoothDevice device : bondedSet)
            {
                Log.e("", device.getName() + "\n" + device.getAddress());
                Log.v("", " count = " + count++);

                dname = device.getName();

                devicename = dname.substring(0, 4);
                if (devicename.equalsIgnoreCase("LUNG"))
                {
                    if (device != null)
                    {
                        if (getState() != STATE_CONNECTED)
                            connect(device, true);
                    }
                }
            }
        } else
        {
            Log.e("", "No Devices");
        }
    }

    private synchronized void setState(int state)
    {
        if (D)
            Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState()
    {
        return mState;
    }

    public synchronized void connect(BluetoothDevice device, boolean secure)
    {
        if (D)
            Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING)
        {
            if (mConnectThread != null)
            {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device

        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        if (D)
            Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();
        setState(STATE_CONNECTED);
    }

    public synchronized void stop()
    {
        if (D)
            Log.d(TAG, "stop");

        if (mConnectThread != null)
        {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null)
        {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_NONE);
    }


    public void write(byte[] out)
    {
        ConnectedThread r;
        synchronized (this)
        {
            if (mState != STATE_CONNECTED)
                return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure)
        {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            try
            {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run()
        {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            Log.d("@@@@@@@@", "beginnnnn");

            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();

            try
            {
                mmSocket.connect();
            } catch (IOException e)
            {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType + " socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SpirometerVitorActivity.this)
            {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);

        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e)
            {
                Log.e(TAG, "close() of connect " + mSocketType                        + " socket failed", e);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            moveTaskToBack(false);
            cancelmythread();
            // txt3.setText(res1.toString());

            //cancelmythread();
            // txt4.setText(res2.toString());
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket, String socketType)
        {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try
            {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run()
        {
//            progressDialog1.dismiss();
            check = true;
            Log.i("@@", "finally method..");
            Log.i(TAG, "BEGIN mConnectedThread");

            byte[] inBuffer = new byte[59];
            byte[] outBuffer = new byte[7];

            Log.i("@@", "after read-write..");

            try
            {
                mmOutStream.write(outBuffer);

            } catch (IOException e1)
            {

                e1.printStackTrace();
            }

            int bytes = 0;
            int readCount = 0;
            try
            {
                Log.i("@@@","read from..");
                while (bytes < 59)
                {
                    readCount = mmInStream.read(inBuffer, bytes, 59 - bytes);
                    bytes = bytes + readCount;
                    //Log.d("Bytes count",""+inBuffer[bytes]);

                }
                BigInteger mBigInteger = new BigInteger(inBuffer);
                Log.d("Big Integer value",""+mBigInteger.toString(16) );
                Log.d("Big Integer",""+mBigInteger);

                for(int i=0;i<bytes;i++)
                {
                    Log.d("Byte value",": "+i +":"+ inBuffer[i]);
                }
                FEV1=inBuffer[14]-48;
                FEV2=inBuffer[15]-48;
                FEV3=inBuffer[16]-48;

                PEF1=inBuffer[17]-48;
                PEF2=inBuffer[18]-48;
                PEF3=inBuffer[19]-48;

                FEV11 = inBuffer[20]-48;
                FEV12 = inBuffer[21]-48;
                FEV13 = inBuffer[22]-48;

                FEV61 = inBuffer[23]-48;
                FEV62 = inBuffer[24]-48;
                FEV63 = inBuffer[25]-48;

                FEV1_61 = inBuffer[26]-48;
                FEV1_62 = inBuffer[27]-48;
                FEV1_63 = inBuffer[28]-48;

                FEF1 = inBuffer[29]-48;
                FEF2 = inBuffer[30]-48;
                FEF3 = inBuffer[31]-48;

                stFEF1 = FEF1+"."+FEF2+FEF3;

                stFEV1 = FEV11+"."+FEV12+FEV13;
                stFEV6 = FEV61+"."+FEV62+FEV63;
                stFEV1_6 =FEV1_61+"."+FEV1_62+FEV1_63;

                stPEF=""+FEV1+""+FEV2+FEV3;
//                PEF=""+PEF1+PEF2+PEF3;
//                Log.d("FEV value","" +FEV);
                Log.d("PEF value",""+stPEF);

//                SpirometerVitorActivity.this.runOnUiThread(new Runnable() {
//
//                    public void run() {
//
////                        SpirometerVitorActivity.this.tv_PEF.setText    (" PEF            :   "+stPEF);
////                        SpirometerVitorActivity.this.tv_FEV1.setText   (" FEV1           :   "+stFEV1);
////                        SpirometerVitorActivity.this.tv_FEV6.setText   (" FEV6           :   "+stFEV6);
////                        SpirometerVitorActivity.this.tv_FEV1_6.setText (" FEV1/FEV       :   "+stFEV1_6);
////                        SpirometerVitorActivity.this.tv_FEF.setText    (" FEF            :   "+stFEF1);
//
//                        Intent returnIntent = new Intent();
//                        returnIntent.putExtra("PEF",stPEF);
//                        returnIntent.putExtra("FEV1",stFEV1);
//                        returnIntent.putExtra("FEV6",stFEV6);
//                        returnIntent.putExtra("FEV1/FEV",stFEV1_6);
//                        returnIntent.putExtra("FEF",stFEF1);
//                        setResult(Activity.RESULT_OK,returnIntent);
//                        finish();
//
//                    }
//
//                });
            } catch (IOException e)
            {
                Log.e(TAG, "@@disconnected", e);

            }

        }



        public void write(byte[] buffer)
        {
            try
            {
                mmOutStream.write(buffer);

            } catch (IOException e)
            {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel()
        {
            try
            {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }



    public void cancelmythread()
    {
        try
        {
            if (mConnectThread != null)
            {
                mConnectThread.cancel();
                mConnectThread = null;
            }

            if (mConnectedThread != null)
            {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            setState(STATE_NONE);
        }
        catch (Exception e)
        {
            // TODO: handle exception
        }
    }


}
