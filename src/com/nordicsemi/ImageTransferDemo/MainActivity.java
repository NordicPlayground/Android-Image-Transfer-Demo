/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nordicsemi.ImageTransferDemo;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Date;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "image_transfer_main";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final int SETTINGS_ACTIVITY = 100;

    private static final String FONT_LABEL_APP_NORMAL = "<font color='#EE0000'>";
    private static final String FONT_LABEL_APP_ERROR = "<font color='#EE0000'>";
    private static final String FONT_LABEL_PEER_NORMAL = "<font color='#EE0000'>";
    private static final String FONT_LABEL_PEER_ERROR = "<font color='#EE0000'>";
    public enum AppLogFontType {APP_NORMAL, APP_ERROR, PEER_NORMAL, PEER_ERROR};
    private String mLogMessage = "";

    TextView mTextViewLog, mTextViewFileLabel, mTextViewPictureStatus, mTextViewConInt, mTextViewMtu;
    Button mBtnTakePicture, mBtnStartStream;
    ProgressBar mProgressBarFileStatus;
    ImageView mMainImage;
    Spinner mSpinnerResolution, mSpinnerPhy;

    private int mState = UART_PROFILE_DISCONNECTED;
    private ImageTransferService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private byte []mUartData = new byte[6];
    private long mStartTimeImageTransfer;

    // File transfer variables
    private int mBytesTransfered = 0, mBytesTotal = 0;
    private byte []mDataBuffer;
    private boolean mStreamActive = false;

    public enum AppRunMode {Disconnected, Connected, ConnectedDuringSingleTransfer, ConnectedDuringStream};
    public enum BleCommand {NoCommand, StartSingleCapture, StartStreaming, StopStreaming, ChangeResolution, ChangePhy, GetBleParams};

    Handler guiUpdateHandler = new Handler();
    Runnable guiUpdateRunnable = new Runnable(){
        @Override
        public void run(){
            if(mTextViewFileLabel != null) {
                mTextViewFileLabel.setText("Incoming: " + String.valueOf(mBytesTransfered) + "/" + String.valueOf(mBytesTotal));
                if(mBytesTotal > 0) {
                    mProgressBarFileStatus.setProgress(mBytesTransfered * 100 / mBytesTotal);
                }
            }
            guiUpdateHandler.postDelayed(this, 50);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        btnConnectDisconnect    = (Button) findViewById(R.id.btn_select);
        mTextViewLog = (TextView)findViewById(R.id.textViewLog);
        mTextViewFileLabel = (TextView)findViewById(R.id.textViewFileLabel);
        mTextViewPictureStatus = (TextView)findViewById(R.id.textViewImageStatus);
        mTextViewConInt = (TextView)findViewById(R.id.textViewCI);
        mTextViewMtu = (TextView)findViewById(R.id.textViewMTU);
        mProgressBarFileStatus = (ProgressBar)findViewById(R.id.progressBarFile);
        mBtnTakePicture = (Button)findViewById(R.id.buttonTakePicture);
        mBtnStartStream = (Button)findViewById(R.id.buttonStartStream);
        mMainImage = (ImageView)findViewById(R.id.imageTransfered);
        mSpinnerResolution = (Spinner)findViewById(R.id.spinnerResolution);
        mSpinnerResolution.setSelection(1);
        mSpinnerPhy = (Spinner)findViewById(R.id.spinnerPhy);
        service_init();
        for(int i = 0; i < 6; i++) mUartData[i] = 0;

        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });

        mBtnTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    mService.sendCommand(BleCommand.StartSingleCapture.ordinal(), null);
                    setGuiByAppMode(AppRunMode.ConnectedDuringSingleTransfer);
                }
            }
        });

        mBtnStartStream.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mService != null){
                    if(!mStreamActive) {
                        mStreamActive = true;

                        mService.sendCommand(BleCommand.StartStreaming.ordinal(), null);
                        setGuiByAppMode(AppRunMode.ConnectedDuringStream);
                    }
                    else {
                        mStreamActive = false;

                        mService.sendCommand(BleCommand.StopStreaming.ordinal(), null);
                        setGuiByAppMode(AppRunMode.Connected);
                    }
                }
            }
        });

        mSpinnerResolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mService != null && mService.isConnected()){
                    byte []cmdData = new byte[1];
                    cmdData[0] = (byte)position;
                    mService.sendCommand(BleCommand.ChangeResolution.ordinal(), cmdData);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        mSpinnerPhy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                if(mService != null && mService.isConnected()){
                    byte []cmdData = new byte[1];
                    cmdData[0] = (byte)position;
                    mService.sendCommand(BleCommand.ChangePhy.ordinal(), cmdData);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // Set initial UI state
        guiUpdateHandler.postDelayed(guiUpdateRunnable, 0);

        setGuiByAppMode(AppRunMode.Disconnected);
    }


    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((ImageTransferService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
       ////     mService.disconnect(mDevice);
        		mService = null;
        }
    };

    private void setGuiByAppMode(AppRunMode appMode)
    {
        switch(appMode)
        {
            case Connected:
                mBtnTakePicture.setEnabled(true);
                mBtnStartStream.setEnabled(true);
                btnConnectDisconnect.setText("Disconnect");
                mBtnStartStream.setText("Start Stream");
                mSpinnerResolution.setEnabled(true);
                mSpinnerPhy.setEnabled(true);
                break;

            case Disconnected:
                mBtnTakePicture.setEnabled(false);
                mBtnStartStream.setEnabled(false);
                btnConnectDisconnect.setText("Connect");
                mBtnStartStream.setText("Start Stream");
                mTextViewPictureStatus.setVisibility(View.INVISIBLE);
                mSpinnerResolution.setEnabled(false);
                mSpinnerPhy.setEnabled(false);
                mSpinnerPhy.setSelection(0);
                break;

            case ConnectedDuringSingleTransfer:
                mBtnTakePicture.setEnabled(false);
                mBtnStartStream.setEnabled(false);
                break;

            case ConnectedDuringStream:
                mBtnTakePicture.setEnabled(false);
                mBtnStartStream.setEnabled(true);
                mBtnStartStream.setText("Stop Stream");
                break;
        }
    }

    private void writeToLog(String message, AppLogFontType msgType){
        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
        String newMessage = currentDateTimeString + " - " + message;
        String fontHtmlTag;
        switch(msgType){
            case APP_NORMAL:
                fontHtmlTag = "<font color='#000000'>";
                break;
            case APP_ERROR:
                fontHtmlTag = "<font color='#AA0000'>";
                break;
            case PEER_NORMAL:
                fontHtmlTag = "<font color='#0000AA'>";
                break;
            case PEER_ERROR:
                fontHtmlTag = "<font color='#FF00AA'>";
                break;
            default:
                fontHtmlTag = "<font>";
                break;
        }
        mLogMessage = fontHtmlTag + newMessage + "</font>" + "<br>" + mLogMessage;
        mTextViewLog.setText(Html.fromHtml(mLogMessage));
    }

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        final Intent mIntent = intent;
        //*********************//
        if (action.equals(ImageTransferService.ACTION_GATT_CONNECTED)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    Log.d(TAG, "UART_CONNECT_MSG");

                    writeToLog("Connected", AppLogFontType.APP_NORMAL);
                }
            });
        }

          //*********************//
        if (action.equals(ImageTransferService.ACTION_GATT_DISCONNECTED)) {
            runOnUiThread(new Runnable() {
                public void run() {
                    String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                    Log.d(TAG, "UART_DISCONNECT_MSG");
                    setGuiByAppMode(AppRunMode.Disconnected);
                    writeToLog("Disconnected", AppLogFontType.APP_NORMAL);
                    mState = UART_PROFILE_DISCONNECTED;
                    mUartData[0] = mUartData[1] = mUartData[2] = mUartData[3] = mUartData[4] = mUartData[5] = 0;
                    mService.close();
                }
            });
        }

        //*********************//
        if (action.equals(ImageTransferService.ACTION_GATT_SERVICES_DISCOVERED)) {
            mService.enableTXNotification();
            mService.sendCommand(BleCommand.GetBleParams.ordinal(), null);
            setGuiByAppMode(AppRunMode.Connected);
        }

        //*********************//
        if (action.equals(ImageTransferService.ACTION_DATA_AVAILABLE)) {

            final byte[] txValue = intent.getByteArrayExtra(ImageTransferService.EXTRA_DATA);
            runOnUiThread(new Runnable() {
            public void run() {
                try {
                    System.arraycopy(txValue, 0, mDataBuffer, mBytesTransfered, txValue.length);
                    if(mBytesTransfered == 0){
                        Log.w(TAG, "First packet received: " + String.valueOf(txValue.length) + " bytes");
                    }
                    mBytesTransfered += txValue.length;
                    if(mBytesTransfered >= mBytesTotal) {
                        long elapsedTime = System.currentTimeMillis() - mStartTimeImageTransfer;
                        float elapsedSeconds = (float)elapsedTime / 1000.0f;
                        DecimalFormat df = new DecimalFormat("0.0");
                        df.setMaximumFractionDigits(1);
                        String elapsedSecondsString = df.format(elapsedSeconds);
                        String kbpsString = df.format((float)mDataBuffer.length / elapsedSeconds * 8.0f / 1000.0f);
                        //writeToLog("Completed in " + elapsedSecondsString + " seconds. " + kbpsString + " kbps", AppLogFontType.APP_NORMAL);
                        mTextViewPictureStatus.setText(String.valueOf(mDataBuffer.length / 1024) + "kB - " + elapsedSecondsString + " seconds - " + kbpsString + " kbps");
                        mTextViewPictureStatus.setVisibility(View.VISIBLE);
                        Bitmap bitmap;
                        Log.w(TAG, "attempting JPEG decode");
                        try {
                            bitmap = BitmapFactory.decodeByteArray(mDataBuffer, 0, mDataBuffer.length);
                            mMainImage.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            Log.w(TAG, "Bitmapfactory fail :(");
                        }
                        if(!mStreamActive) {
                            setGuiByAppMode(AppRunMode.Connected);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
            });
        }
        //*********************//
        if (action.equals(ImageTransferService.ACTION_IMG_INFO_AVAILABLE)) {

            final byte[] txValue = intent.getByteArrayExtra(ImageTransferService.EXTRA_DATA);
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        switch(txValue[0]) {
                            case 1:
                                // Start a new file transfer
                                ByteBuffer byteBuffer = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 1, 5));
                                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                                int fileSize = byteBuffer.getInt();
                                mBytesTotal = fileSize;
                                mDataBuffer = new byte[fileSize];
                                mTextViewFileLabel.setText("Incoming file: " + String.valueOf(fileSize) + " bytes.");
                                mBytesTransfered = 0;
                                mStartTimeImageTransfer = System.currentTimeMillis();
                                break;

                            case 2:
                                ByteBuffer mtuBB = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 1, 3));
                                mtuBB.order(ByteOrder.LITTLE_ENDIAN);
                                short mtu = mtuBB.getShort();
                                mTextViewMtu.setText(String.valueOf(mtu) + " bytes");
                                ByteBuffer ciBB = ByteBuffer.wrap(Arrays.copyOfRange(txValue, 3, 5));
                                ciBB.order(ByteOrder.LITTLE_ENDIAN);
                                short conInterval = ciBB.getShort();
                                mTextViewConInt.setText(String.valueOf((float)conInterval * 1.25f) + "ms");
                                short txPhy = txValue[5];
                                short rxPhy = txValue[6];
                                if(txPhy == 0x0001 && mSpinnerPhy.getSelectedItemPosition() == 1) {
                                    mSpinnerPhy.setSelection(0);
                                    writeToLog("2Mbps not supported!", AppLogFontType.APP_ERROR);
                                }
                                else {
                                    writeToLog("Parameters updated.", AppLogFontType.APP_NORMAL);
                                }
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            });
        }
        //*********************//
        if (action.equals(ImageTransferService.DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER)){
            //showMessage("Device doesn't support UART. Disconnecting");
            writeToLog("APP: Invalid BLE service, disconnecting!",  AppLogFontType.APP_ERROR);
            mService.disconnect();
        }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, ImageTransferService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ImageTransferService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(ImageTransferService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(ImageTransferService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(ImageTransferService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(ImageTransferService.ACTION_IMG_INFO_AVAILABLE);
        intentFilter.addAction(ImageTransferService.DEVICE_DOES_NOT_SUPPORT_IMAGE_TRANSFER);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
    	 super.onDestroy();
        Log.d(TAG, "onDestroy()");
        
        try {
        	LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        } 
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;
       
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
 
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    //((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName()+ " - connecting");
                    mService.connect(deviceAddress);
                }
                break;

            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
       
    }

    
    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  
    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        }
        else {
            finish();
        }
    }
}
