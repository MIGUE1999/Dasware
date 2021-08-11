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

package com.example.android.bluetoothlegatt;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import org.libsodium.jni.*;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;


import java.lang.invoke.ConstantCallSite;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private static Sodium sodium;

    private  byte[] peripheralPubKey=null;
    private  byte[] peripheralPrivKey=null;
    private byte[] challenge=null;

    public final int REQUEST_COMAND_SIZE=4;
    public final int KEY_SIZE = 68;
    public final int AUTHENTICATOR_SIZE = 76;







    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //Log.i(TAG, "Attempting to start service discovery:" +
                        //mBluetoothGatt.discoverServices());
                        mBluetoothGatt.requestMtu(512);

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onMtuChanged (BluetoothGatt gatt, int mtu, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {

                Log.d(ServerUUIDS.TAG, "ON READ STATUS:"+ status);
                Log.d(ServerUUIDS.TAG, "El valor on characteristic read es: "+ DeviceControlActivity.cripto.bytesToHex(characteristic.getValue()));
            Log.d(ServerUUIDS.TAG, "El valor on characteristic read es: "+ (DeviceControlActivity.cripto.hexToAscii(DeviceControlActivity.cripto.bytesToHex(characteristic.getValue()))));
            if(DeviceControlActivity.authid)
                Log.d(ServerUUIDS.TAG, "Auth id = true");
            else
                Log.d(ServerUUIDS.TAG, "Auth id = false");



            if(characteristic.getUuid().equals(ServerUUIDS.AUTHORIZATION_CHAR) && DeviceControlActivity.controlPkey) {
                Log.d(TAG, "SE METE");
                DeviceControlActivity.cripto.setPeripheralPublicKey(characteristic.getValue());

            }else if(characteristic.getUuid().equals(ServerUUIDS.AUTHORIZATION_CHAR) && DeviceControlActivity.challenge == true){
                Log.d(TAG, "Dentro del challenge");
                DeviceControlActivity.cripto.setChallenge(characteristic.getValue());
            }

            else if(characteristic.getUuid().equals(ServerUUIDS.AUTHORIZATION_CHAR) && DeviceControlActivity.authid == true){
                Log.d(TAG, "Dentro del authid");
                String comando= DeviceControlActivity.cripto.hexToAscii(DeviceControlActivity.cripto.bytesToHex(characteristic.getValue()));
                String value="";
                Log.d(TAG, "Comando: "+ comando);
                for (int i = 0; i < REQUEST_COMAND_SIZE; i++) {
                    value = value + comando.charAt(i);
                }
                Log.d(TAG, "Value: "+value);
                if (value.equals("0700")){
                    value="";
                    for (int i = REQUEST_COMAND_SIZE; i < KEY_SIZE; i++) {
                        value += comando.charAt(i);
                    }
                    Log.d(TAG, "Peripherical authenticator: " +  value);
                    Log.d(TAG, "Central authenticator: " +  Criptografia.bytesToHex(DeviceControlActivity.cripto.getAuthenticator()));
                    if(value.equals(Criptografia.bytesToHex(DeviceControlActivity.cripto.getAuthenticator()))){
                        value="";
                        for (int i = KEY_SIZE; i < AUTHENTICATOR_SIZE ; i++) {
                            value += comando.charAt(i);
                        }
                        DeviceControlActivity.cripto.setAuthorizationId(value);
                        Log.d(TAG,"AUTH-ID completado con exito");
                    }
                    else{
                        Log.e(TAG,"Error en authid");
                    }
                }
                else{
                    Log.e(TAG, "Error en authorization Id");
                }
            }
            else if(characteristic.getUuid().equals(ServerUUIDS.AUTHORIZATION_CHAR) && DeviceControlActivity.idconfirmation==true){
                String coman = DeviceControlActivity.cripto.bytesToHex(characteristic.getValue());
                String val="";
                for(int i=0; i < 6; i++){
                    val+=coman.charAt(i);
                }
                Log.d(TAG,"VALUE: "+ val);
                if(val.equals("0E0000")){
                    Log.d(TAG, "ESTATUS COMPLETE");
                }else{
                    Log.e(TAG, "ESTATUS NOT  COMPLETE");

                }
            }



            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

        }

        //CALLBACK WRITE

        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

            Log.d(ServerUUIDS.TAG, "onCharacteristicWrite");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(ServerUUIDS.TAG, "onCharacteristicWrite: VALUE: "+ DeviceControlActivity.cripto.hexToAscii(DeviceControlActivity.cripto.bytesToHex(characteristic.getValue())).toUpperCase());
            } else {
                Log.d(ServerUUIDS.TAG, "characteristic write err:" + status);

            }
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.d(ServerUUIDS.TAG, "ON CHARACTERISTICCHANGED: gatt success");

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);

    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        } else {
            // For all other profiles, writes the data formatted in HEX.
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
            }
        }
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public BluetoothGattCharacteristic readCustomCharacteristic() {

        Log.d(ServerUUIDS.TAG, "Dentro de readCustomCharacteristic");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(ServerUUIDS.UART_SERVICE);
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return null;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(ServerUUIDS.TX_READ_CHAR);
        if(mReadCharacteristic == null){
            Log.w(TAG, "ReadCustomCharacteristic: Failed to read characteristic because is NULL");
            return null;
        }

        Log.d(ServerUUIDS.TAG, "ReadCustomCharacteristic: Realizado con Exito");
        return mReadCharacteristic;
    }

    public BluetoothGattCharacteristic readAuthorizeCharacteristic() {

        Log.d(ServerUUIDS.TAG, "Dentro de readAuthorizeCharacteristic");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        /*check if the service is available on the device*/
        BluetoothGattService mCustomService = mBluetoothGatt.getService(ServerUUIDS.KEYTURNER_PAIRING_SERVICE);
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return null;
        }
        /*get the read characteristic from the service*/
        BluetoothGattCharacteristic mReadCharacteristic = mCustomService.getCharacteristic(ServerUUIDS.AUTHORIZATION_CHAR);
        if(mReadCharacteristic == null){
            Log.w(TAG, "ReadCustomCharacteristic: Failed to read characteristic because is NULL");
            return null;
        }

        Log.d(ServerUUIDS.TAG, "ReadAuthorizeCharacteristic: Realizado con Exito");
        return mReadCharacteristic;
    }

    //Distintos Write characteristics

    public void writeCustomCharacteristic(String value) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        /*check if the service is available on the device*/

        BluetoothGattService mCustomService =  mBluetoothGatt.getService(ServerUUIDS.UART_SERVICE);
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }
        /*get the write characteristic from the service*/

        BluetoothGattCharacteristic mWriteCharacteristic =mCustomService.getCharacteristic(ServerUUIDS.TX_READ_CHAR);
        //mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
        mWriteCharacteristic.setValue(value);
        Log.d("TAG","VALOR DE WRITE CUSTOM CHARACTERISTIC. "+ mWriteCharacteristic.getValue());
        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            Log.w(TAG, "Failed to write characteristic");
        }
    }

    public void writePairingService(String value){
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        /*check if the service is available on the device*/

        BluetoothGattService mCustomService =  mBluetoothGatt.getService(ServerUUIDS.KEYTURNER_PAIRING_SERVICE);
        if(mCustomService == null){
            Log.w(TAG, "Custom BLE Service not found");
            return;
        }

        /*get the authorization characteristic from the service*/

        BluetoothGattCharacteristic mWriteCharacteristic = mCustomService.getCharacteristic(ServerUUIDS.AUTHORIZATION_CHAR);
        //mWriteCharacteristic.setValue(value,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
        Log.d(TAG, "El valor que se cambia es: "+ value);
        mWriteCharacteristic.setValue(value);
        Log.d("TAG","VALOR DE AUTHORIZATION CUSTOM CHARACTERISTIC. "+ DeviceControlActivity.cripto.hexToAscii(DeviceControlActivity.cripto.bytesToHex(mWriteCharacteristic.getValue())).toUpperCase());
        if(mBluetoothGatt.writeCharacteristic(mWriteCharacteristic) == false){
            Log.w(TAG, "Failed to write characteristic");
        }

    }





}
