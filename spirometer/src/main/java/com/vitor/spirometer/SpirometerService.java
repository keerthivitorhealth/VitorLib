package com.vitor.spirometer;

import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Set;
import java.util.UUID;

public class SpirometerService extends Service {

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
    TextView tv_PEF,tv_FEV1,tv_FEV6,tv_FEV1_6,tv_FEF;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0; // we're doing nothing
    public static final int STATE_LISTEN = 1; // now listening for incoming
    // connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing
    // connection
    public static final int STATE_CONNECTED = 3; // now connected to a remote
    // device

    String dname;
    String devicename;
    BluetoothAdapter mAdapter;

    boolean check = false;

    public SpirometerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
//        Toast.makeText(this, "SpirometerService onCreate called", Toast.LENGTH_LONG).show();

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
        AddEvents();


        //we have some options for service
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }


    private void AddEvents()
    {
        // TODO Auto-generated method stub

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        mState = STATE_NONE;

        Thread th1 = new Thread()
        {
            @Override
            public void run()
            {
                // TODO Auto-generated method stub
                super.run();
                try
                {
                    check = false;
                    sleep(10000);
                } catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                } finally
                {
//                            progressDialog1.dismiss();
                    // cancelmythread();
                    if (check == false)
                    {
                        new Runnable()
                        {
                            public void run()
                            {
                                Context context = getApplicationContext();
                                CharSequence text = "could not connect , please try again.........!";

//                                Toast toast = Toast.makeText(context, text, Toast.LENGTH_LONG);
//                                toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
//                                toast.show();
                            }
                        };
                    }
                    check = true;
                }
            }
        };
        th1.start();

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


    @Override
    public void onDestroy() {
//        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();

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

            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {

            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);

            Log.d("@@@@@@@@", "beginnnnn");

            setName("ConnectThread" + mSocketType);
            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType
                            + " socket during connection failure", e2);
                }
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (getApplicationContext()) {
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
                Log.e(TAG, "close() of connect " + mSocketType
                        + " socket failed", e);
            }
        }
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

                Log.i("@@@","reda from..");
                while (bytes < 59)
                {

                    readCount = mmInStream.read(inBuffer, bytes,
                            59 - bytes);
                    bytes = bytes + readCount;

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
//                Log.d("PEF value",""+stPEF);
                Log.d("DATA","SPIR DATA : "+stPEF +" , "+ stFEV1+" , "+stFEV6+" , "+stFEV1_6+" , "+stFEF1);
                sendDataToActivity(stPEF,stFEV1,stFEV6,stFEV1_6,stFEF1);
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

    private void sendDataToActivity(String stPEF , String stFEV1, String stFEV6, String stFEV1_6, String stFEF1) {
        Intent intent = new Intent("SpirometerData");
        intent.putExtra("PEF", stPEF);
        intent.putExtra("FEV1", stFEV1);
        intent.putExtra("FEV6", stFEV6);
        intent.putExtra("FEV1_6", stFEV1_6);
        intent.putExtra("FEF1", stFEF1);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendLocationBroadcast(Intent intent){

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




}
