package com.example.eugeneslizh.bluetoothprinterapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mPrintButton;
    private TextView mTextView;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private String value;
    private byte[] readBuffer;
    private int mBufferPosition;
    private Thread mWorkerThred;
    private boolean mStopWork;

    @Override
    public void onClick(View v) {
        IntentPrinter("Hello world!");
    }

    private void IntentPrinter(String text) {
        byte[] textArray = text.getBytes();
        byte[] hader = new byte[]{(byte) 0xAA, 0x55, 2, 0};
        hader[3] = (byte) textArray.length;
        initPrinter();

        if (hader.length > 128) {
            value += "header length more than 128";
        } else {
            try {
                mOutputStream.write(text.getBytes());
                mOutputStream.close();
                mSocket.close();
            } catch (IOException pE) {
                pE.printStackTrace();
                value += " Ixception in IntentPrinter";
            }
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrintButton = findViewById(R.id.print_button);
        mTextView = findViewById(R.id.description);
        mPrintButton.setOnClickListener(this);
    }

    private void initPrinter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(bluetoothIntent, 0);
            }

            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                StringBuilder builder = new StringBuilder();
                for (BluetoothDevice device : pairedDevices) {
                    builder.append(device.getName()).append("\n");
                    if(device.getName().equals("ANDROID BT")) { //
                        mBluetoothDevice = device;
                    }
                }
                mTextView.setText(builder.toString());

                UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
                Method method = mBluetoothDevice.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
                mSocket = (BluetoothSocket) method.invoke(mBluetoothDevice, 1);
                BluetoothSocket socket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                mBluetoothAdapter.cancelDiscovery();
                mSocket.connect();
                mInputStream = mSocket.getInputStream();
                mOutputStream = mSocket.getOutputStream();
                beginListenForData();
            } else {
                value += "No device found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            value += e.getMessage();
        }
    }

    private void beginListenForData() {
        try {

            final Handler handler = new Handler();
            final byte delimeter = 10;
            mStopWork = false;
            mBufferPosition = 0;
            readBuffer = new byte[1024];
            mWorkerThred = new Thread(new Runnable() {

                @Override
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !mStopWork) {

                        try {
                            int byteAvailable = mInputStream.available();

                            if (byteAvailable > 0) {
                                byte[] bytePacket = new byte[byteAvailable];
                                mInputStream.read(bytePacket);
                                for (int index = 0; index < byteAvailable; ++index) {
                                    byte someByte = bytePacket[index];

                                    if (someByte == delimeter) {
                                        byte[] encodedeBytes = new byte[mBufferPosition];
                                        System.arraycopy(
                                                readBuffer, 0,
                                                encodedeBytes, 0,
                                                encodedeBytes.length
                                        );
                                        final String data = new String(encodedeBytes, "US-ASCII");
                                        mBufferPosition = 0;
                                        handler.post(new Runnable() {

                                            @Override
                                            public void run() {
                                                Log.d("MyLogs", data);
                                            }
                                        });
                                    } else {
                                        readBuffer[mBufferPosition++] = someByte;
                                    }
                                }
                            }
                        } catch (IOException pE) {
                            pE.printStackTrace();
                            mStopWork = true;
                        }
                    }
                }
            });
            mWorkerThred.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

