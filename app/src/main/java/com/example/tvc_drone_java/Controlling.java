package com.example.tvc_drone_java;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class Controlling extends AppCompatActivity {
    private static final String TAG = "BlueTest5-Controlling";
    private int mMaxChars = 50000;//Default//change this to string..........
    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;

    private BluetoothDevice mDevice;

    final static String raise="R";
    final static String lower="L";
    final static String takeOff="T";
    final static String kill="K";


    private ProgressDialog progressDialog;
    Button btnRaise,btnLower, btnTakeOff, btnKill;
    TextView xAccel, yAccel, zAccel, xServo, yServo, motors, joystickVals;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_controlling);

        ActivityHelper.initialize(this);
        btnRaise=(Button)findViewById(R.id.raise);
        btnLower=(Button)findViewById(R.id.lower);
        btnTakeOff=(Button)findViewById(R.id.takeOff);
        btnKill=(Button)findViewById(R.id.kill);
        xAccel=(TextView)findViewById(R.id.xAccel);
        yAccel=(TextView)findViewById(R.id.yAccel);
        zAccel=(TextView)findViewById(R.id.zAccel);
        xServo=(TextView)findViewById(R.id.xServo);
        yServo=(TextView)findViewById(R.id.yServo);
        motors=(TextView)findViewById(R.id.motors);
        joystickVals=(TextView)findViewById(R.id.joystickVals);


        Intent intent = getIntent();
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA);
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID));
        mMaxChars = b.getInt(MainActivity.BUFFER_SIZE);

        Log.d(TAG, "Ready");

        btnRaise.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(raise.getBytes());

                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});

        btnLower.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(lower.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});
        btnTakeOff.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(takeOff.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});
        btnKill.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                try {
                    mBTSocket.getOutputStream().write(kill.getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }});
        final JoystickView joystick = (JoystickView) findViewById(R.id.joystick);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                if (strength == 0) { //Notify the drone that we let go of the joystick
                    try {
                        mBTSocket.getOutputStream().write(("D").getBytes());
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //Calculate servo angles
                long maxVal = Math.round(strength*.17);
                double x = maxVal*Math.cos(Math.toRadians(angle));
                double y = maxVal*Math.sin(Math.toRadians(angle));
                double denom;
                if(Math.abs(x) > Math.abs(y)){
                    denom = x;
                    x = Math.round(x * (maxVal/(denom+.01)));
                    y = Math.round(y * (maxVal/(denom+.01)));
                }
                else{
                    denom = y;
                    y = Math.round(y * (maxVal/(denom+.01)));
                    x = Math.round(x * (maxVal/(denom+.01)));
                }
                //Inverting values based on where sin and cos are reversed
                if(angle > 135 && angle <= 315){
                    x *= -1;
                    y *= -1;
                };
                String xS = String.valueOf((int)x);
                String yS = String.valueOf((int)y);
                String output = xS + ":" + yS + ",";
                joystickVals.setText(output);
                try {

                    mBTSocket.getOutputStream().write((output).getBytes());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }, 150);

    }

    private class ReadInput implements Runnable {

        private boolean bStop = false;
        private Thread t;

        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }

        public boolean isRunning() {
            return t.isAlive();
        }

        @Override
        public void run() {
            InputStream inputStream;

            try {
                inputStream = mBTSocket.getInputStream();
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer);
                        int i = 0;
                        /*
                         * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
                         */
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);
                        //splitting by new lines, but keeping newlines https://stackoverflow.com/questions/2206378/how-to-split-a-string-but-also-keep-the-delimiters
                        String [] lines = strInput.split("(?<=\n)");

                        for(String line : lines){
                            if(line.startsWith("X:") && line.endsWith("\n")){
                                xAccel.setText(line.substring(0, line.length() - 1));
                            } else if(line.startsWith("Y:") && line.endsWith("\n")){
                                yAccel.setText(line.substring(0, line.length() - 1));
                            } else if(line.startsWith("Z:") && line.endsWith("\n")){
                                zAccel.setText(line.substring(0, line.length() - 1));
                            } else if(line.startsWith("X Servo:") && line.endsWith("\n")){
                                xServo.setText(line.substring(0, line.length() - 1));
                            } else if(line.startsWith("Y Servo:") && line.endsWith("\n")){
                                yServo.setText(line.substring(0, line.length() - 1));
                            } else if(line.startsWith("Speed:") && line.endsWith("\n")){
                                motors.setText(line.substring(0, line.length() - 1));
                            }
                        }
                    }
                    Thread.sleep(200);
                }
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InterruptedException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public void stop() {
            bStop = true;
        }

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {//cant inderstand these dotss

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;

            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
// TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }

    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        if (mBTSocket != null && mIsBluetoothConnected) {
            msg("onPause");
            new DisConnectBT().execute();
        }
        Log.d(TAG, "Paused");
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (mBTSocket == null || !mIsBluetoothConnected) {
//        if(mBTSocket == null){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            msg("onResume");
            new ConnectBT().execute();
        }
        Log.d(TAG, "Resumed");
        super.onResume();

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Stopped");
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
// TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {

            progressDialog = ProgressDialog.show(Controlling.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554

        }

        @Override
        protected Void doInBackground(Void... devices) {

            try {
                Thread.sleep(2000);
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
// Unable to connect to device`
                // e.printStackTrace();
                mConnectSuccessful = false;

            } catch(InterruptedException e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                Toast.makeText(getApplicationContext(), "Could not connect to device.Please turn on your Hardware", Toast.LENGTH_LONG).show();
                //finish();
            } else {
                msg("Connected to device");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }

            progressDialog.dismiss();
        }

    }
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
    }
}