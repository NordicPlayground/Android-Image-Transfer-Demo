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

package com.nordicsemi.IntensityLightControl;

import java.text.DateFormat;
import java.util.Date;


import com.nordicsemi.IntensityLightControl.gui.IntensityLedButton;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener, IntensityLedButton.OnLedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final String TAG = "lbs_tag";
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

    TextView mRemoteRssiVal, mTextViewLog;
    RadioGroup mRg;
    private int mState = UART_PROFILE_DISCONNECTED;
    private LedButtonService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private Button btnConnectDisconnect;
    private IntensityLedButton btnLed;
    private byte []mUartData = new byte[6];

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
        btnLed = (IntensityLedButton) findViewById(R.id.ledbutton_led01);
        btnLed.setLedChangedListener(this);
        mTextViewLog = (TextView)findViewById(R.id.textViewLog);
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
        // Set initial UI state
        //mRgbLedButton.setRgbChangedListener(this);
    }


    @Override
    public void onIntensityChanged(IntensityLedButton sender, boolean ledOn, float ledIntensity) {
        byte[] uartData = new byte[6];
        writeToLog("Set intensity: " + String.valueOf(ledIntensity), AppLogFontType.APP_NORMAL);
        if(mService.isConnected()){
            String uartString;
            int intensity = (int)(ledIntensity * 255.0f);
            uartData[0] = (byte)(ledOn ? '1' : '0');
            uartData[1] = (byte)'-';
            uartData[2] = (byte)(intensity / 100 + '0');
            uartData[3] = (byte)((intensity / 10) % 10 + '0');
            uartData[4] = (byte)(intensity % 10 + '0');
            uartData[5] = 0;
            mService.writeRXCharacteristic(uartData);
        }
    }

    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((LedButtonService.LocalBinder) rawBinder).getService();
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

    private Handler mHandler = new Handler() {
        @Override
        
        //Handler events that received from UART service 
        public void handleMessage(Message msg) {
  
        }
    };

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
            if (action.equals(LedButtonService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnLed.setEnabled(true);
                        btnConnectDisconnect.setText("Disconnect");
                        writeToLog("Connected", AppLogFontType.APP_NORMAL);
                    }
                });
            }

              //*********************//
            if (action.equals(LedButtonService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        btnLed.setEnabled(false);
                        writeToLog("Disconnected", AppLogFontType.APP_NORMAL);
                        mState = UART_PROFILE_DISCONNECTED;
                        mUartData[0] = mUartData[1] = mUartData[2] = mUartData[3] = mUartData[4] = mUartData[5] = 0;
                        mService.close();
                    }
                });
            }
          
          //*********************//
            if (action.equals(LedButtonService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
                //mRgbLedButton.setEnabled(true);
            }
          //*********************//
            if (action.equals(LedButtonService.ACTION_DATA_AVAILABLE)) {
              
                 final byte[] txValue = intent.getByteArrayExtra(LedButtonService.EXTRA_DATA);
                 runOnUiThread(new Runnable() {
                     public void run() {
                         try {
                             String text = new String(txValue, "UTF-8");
                             if(text.charAt(0) == '!'){
                                 writeToLog(text.substring(1, text.length()), AppLogFontType.PEER_ERROR);
                             }
                             else {
                                 writeToLog(text, AppLogFontType.PEER_NORMAL);
                             }
                         } catch (Exception e) {
                             Log.e(TAG, e.toString());
                         }
                     }
                 });
             }
           //*********************//
            if (action.equals(LedButtonService.DEVICE_DOES_NOT_SUPPORT_LEDBUTTON)){
            	//showMessage("Device doesn't support UART. Disconnecting");
                writeToLog("APP: Invalid BLE service, disconnecting!",  AppLogFontType.APP_ERROR);
            	mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, LedButtonService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
  
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(LedButtonService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(LedButtonService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(LedButtonService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(LedButtonService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(LedButtonService.DEVICE_DOES_NOT_SUPPORT_LEDBUTTON);
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
