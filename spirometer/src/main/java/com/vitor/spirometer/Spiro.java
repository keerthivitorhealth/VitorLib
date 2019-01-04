package com.vitor.spirometer;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

public class Spiro {

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

    // Member fields

    Handler mHandler;

    private ConnectThread mConnectThread;
//    private ConnectedThread mConnectedThread;
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

    TextView tv_PEF,tv_FEV1,tv_FEV6,tv_FEV1_6,tv_FEF;

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
    Button BTN_connect;
    ProgressDialog progressDialog1;
    BluetoothAdapter mAdapter;

    boolean check = false;


    public void startMethod(){

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
//        if (mConnectedThread != null)
//        {
////            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }

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
//        if (mConnectedThread != null)
//        {
////            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }

        // Start the thread to manage the connection and perform transmissions
//        mConnectedThread = new ConnectedThread(socket, socketType);
//        mConnectedThread.start();
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

//        if (mConnectedThread != null)
//        {
////            mConnectedThread.cancel();
//            mConnectedThread = null;
//        }

        setState(STATE_NONE);
    }


//    public void write(byte[] out)
//    {
//        ConnectedThread r;
//        synchronized (this)
//        {
//            if (mState != STATE_CONNECTED)
//                return;
//            r = mConnectedThread;
//        }
//        r.write(out);
//    }

    private class ConnectThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;
        private Object Context;

        @SuppressLint("MissingPermission")
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
            synchronized (Context)
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




}
