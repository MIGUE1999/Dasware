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

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String value;
    private boolean cerrado=true;
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private PrivateKey centralPrivateKey = null;
    private PublicKey centralPublicKey = null;
    private byte[] peripheralPrivateKey = null;
    private byte[] peripheralPublicKey = null;

    public static Criptografia cripto;
    public static boolean challenge=false;
    public static boolean controlPkey=false;
    public static boolean authid=false;
    public static boolean idconfirmation=false;





    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    /**
     *     Code to manage Service lifecycle.
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     *      Handles various events fired by the Service.
     *      ACTION_GATT_CONNECTED: connected to a GATT server.
     *      ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     *      ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
     *      ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
     *
     */
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                //displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                mDataField.setText(" ");
                value=intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                displayData(value);
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    /*
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
    };
    */

    /**
     * Metodo que limpia la interfaz
     */
    private void clearUI() {
        //mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.button_control);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        /*
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
    mConnectionState = (TextView) findViewById(R.id.connection_state);
    */
        mDataField = (TextView) findViewById(R.id.data_value);

        cripto = new Criptografia();
        cripto.generateKeyPair();
        centralPrivateKey = cripto.getPrivateKey();
        centralPublicKey = cripto.getPublicKey();

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        if(cripto.getSharedKey() != null)
            Log.e("OnResume", "Shared Key restablecida: " + cripto.getSharedKey());

        if(cripto.getAuthorizationId() != null)
            Log.e("OnResume", "AuthorizationId restablecida: " + cripto.getAuthorizationId());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(cripto.getAuthorizationId() != null)
        cripto.setAuthorizationId(cripto.bytesToHex(cripto.getAuthorizationId()));
        if(cripto.getSharedKey() != null)
        cripto.setSharedKey(cripto.getSharedKey());

        Log.e("OnPause", "Shared Key restablecida: " + cripto.getSharedKey());
        Log.e("OnPause", "AuthorizationId restablecida: " + cripto.getAuthorizationId());
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               // mConnectionState.setText(resourceId);
            }
        });
    }

    /**
     * Muestra los datos en la interfaz que se le pasen por parametro
     * @param data texto a mostrar en la interfaz
     */
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    /**
     *     Demonstrates how to iterate through the supported GATT Services/Characteristics.
     *     In this sample, we populate the data structure that is bound to the ExpandableListView
     *     on the UI.
     */
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    /**
     * Al pulsar en write en la interfaz comienza el proceso de comunicacion de requestDataKeyturnerStates
     * @param v vista actual
     * @throws InterruptedException
     */
    public void onClickWrite(View v) throws InterruptedException {
        if (mBluetoothLeService != null) {
/*
            if(cerrado) {
                //mBluetoothLeService.writeCustomCharacteristic("0xBB");
                authorizeApp();
                cerrado=false;
                Log.d(TAG, "Creando KeyPair...");
                peripheralPublicKey = cripto.getPeripheralPublicKeyBytes();
                Log.d(TAG, "Peripheral Public Key"+ peripheralPublicKey);


            }else{
                //mBluetoothLeService.writeCustomCharacteristic("0xAA");
                authorizeApp();
                cerrado=true;
            }
            */
            requestDataKeyturnerStates();
            //TimeUnit.SECONDS.sleep(1);
            //readAuthorizeCharacteristic();
        }
    }

    /**
     * Al pulsar en Read en la interfaz comienza el proceso de autenticacion del usuario con la puerta
     * @param v vista actual
     * @throws InterruptedException
     */
    public void onClickRead(View v) throws InterruptedException {
        if(mBluetoothLeService != null) {
            //readAuthorizeCharacteristic();


            /* SEND PUBLICKEY
            String code="0300";
            cripto.generateNonce();
            code = code + cripto.getPublicKey() + BluetoothLeService.bytesToHex(cripto.getNonce());
            Log.d(TAG, "EL codigo es: "+ code);
            mBluetoothLeService.writePairingService(code);
            */

            authorizeApp();

            challenge();
            String r = concatenate();
            cripto.calculateAuthenticator(r);
            authorizationAuthenticator();
            Log.d(TAG,"***********************************************************");

            challenge();

            r = concatenate();

            cripto.calculateAuthenticator(r);

            authorizationData();

            authorizationId();

            authorizationIdConfirmation();

            //requestDataKeyturnerStates();



        }

    }

    /**
     * Metodo que sirve para compartir la shared key entre el periferico y el central
     * @throws InterruptedException
     */
    public void authorizeApp() throws InterruptedException {

        String code = "0100030027A7";
        controlPkey=true;
        mBluetoothLeService.writePairingService(code);          //Request Data Public Key
        TimeUnit.SECONDS.sleep(1);
        readAuthorizeCharacteristic();
        TimeUnit.SECONDS.sleep(1);
        controlPkey = false;

        //comprobacion diffieHellman
            peripheralPublicKey = cripto.getPeripheralPublicKeyBytes();
            Log.d(TAG, "Peripheral Public Key: " + cripto.bytesToHex(peripheralPublicKey));

            sendPublicKey();

            byte[] shared_key_local = new byte[Sodium.crypto_core_hsalsa20_outputbytes()];
            cripto.setSharedKey(cripto.diffieHellman(peripheralPublicKey));



    }


    public void readMainCharacteristic(){

            BluetoothGattCharacteristic characteristic = mBluetoothLeService.readCustomCharacteristic();

            if(characteristic == null){
                Log.d(TAG, "OnClickRead: ERROR on readCustomCharacteristic");
            }
            else{
                final int charaProp = characteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService.setCharacteristicNotification(
                                mNotifyCharacteristic, false);
                        mNotifyCharacteristic = null;
                    }
                    mBluetoothLeService.readCharacteristic(characteristic);
                }
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    mNotifyCharacteristic = characteristic;
                    mBluetoothLeService.setCharacteristicNotification(
                            characteristic, true);
                }

            }

    }

    /**
     * Metodo para leer la característica Authorize
     */
    public void readAuthorizeCharacteristic(){
        BluetoothGattCharacteristic characteristic = mBluetoothLeService.readAuthorizeCharacteristic();

        if(characteristic == null){
            Log.d(TAG, "OnClickRead: ERROR on readAuthorizeCharacteristic");
        }
        else{
            final int charaProp = characteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.
                if (mNotifyCharacteristic != null) {
                    mBluetoothLeService.setCharacteristicNotification(
                            mNotifyCharacteristic, false);
                    mNotifyCharacteristic = null;
                }
                mBluetoothLeService.readCharacteristic(characteristic);
            }
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mNotifyCharacteristic = characteristic;
                mBluetoothLeService.setCharacteristicNotification(
                        characteristic, true);
            }

        }

    }

    /**
     * Método que manda la public key al periferico escribiendo en la caracteristica esta key
     * @throws InterruptedException
     */
    public void sendPublicKey() throws InterruptedException {

            String code="0300";
            cripto.generateNonce();
            code = code + cripto.getPublicKey() + cripto.bytesToHex(cripto.getNonce());
            Log.d(TAG, "EL codigo es: "+ code);
            TimeUnit.SECONDS.sleep(1);
            mBluetoothLeService.writePairingService(code);
            //Log.d("SendPublicKey", "Shared Key: "+ cripto.bytesToHex(cripto.getSharedKey()));
            Log.d("SendPublicKey","Valor de Peri: "+cripto.bytesToHex(peripheralPublicKey)) ;
            Log.d("SendPublicKey","Valor de Central: "+cripto.bytesToHex(centralPublicKey.toBytes())) ;


    }

    /**
     * Metodo por el cual se manda un challenge al periferico que debería descifrarlo
     * si obtuvo la shared key correcta
     * @throws InterruptedException
     */
    public void challenge() throws InterruptedException {
        challenge = true;
        String code="0400";
        byte[] nonceChallenge = cripto.generateNonce64();
        cripto.generateNonce();
        Log.d(TAG, "Nonce Challenge: " + cripto.bytesToHex(nonceChallenge));

        code = code + cripto.bytesToHex(nonceChallenge) + cripto.bytesToHex(cripto.getNonce());

        Log.d("Challenge", "Code Challenge: " + code);

        TimeUnit.SECONDS.sleep(1);
        mBluetoothLeService.writePairingService(code);
        TimeUnit.SECONDS.sleep(1);
        //readAuthorizeCharacteristic();
        //TimeUnit.SECONDS.sleep(1);
        cripto.setChallenge(nonceChallenge);

        challenge = false;


        Log.d("Challenge","Valor de Peri: "+cripto.bytesToHex(peripheralPublicKey)) ;
        Log.d("Challenge","Valor de Central: "+cripto.bytesToHex(centralPublicKey.toBytes())) ;
        Log.d("Challenge","Valor de Challenge: "+ cripto.bytesToHex(cripto.getChallenge())) ;
        Log.d("Challenge", "Shared Key: "+ cripto.bytesToHex(cripto.getSharedKey()));


    }

    /**
     * metodo que concatena la Pkey del central con la Pkey del periferico con el challenge calculado en challenge()
     * @return r concatenacion de pkeys y challenge
     */
    public String concatenate(){
        String r;
        r= cripto.bytesToHex(centralPublicKey.toBytes())+ cripto.bytesToHex(peripheralPublicKey) + cripto.bytesToHex(cripto.getChallenge());
        Log.d("Concatenate","Valor de R: "+r) ;
        Log.d("Concatenate", "Shared Key: "+ cripto.bytesToHex(cripto.getSharedKey()));
        Log.d("Concatenate","Valor de Peri: "+cripto.bytesToHex(peripheralPublicKey)) ;
        Log.d("Concatenate","Valor de Central: "+cripto.bytesToHex(centralPublicKey.toBytes())) ;

        return r;
    }


    /**
     * Escribe el codigo del authorizationAuthenticator en la caracteristica para que se realicen
     * las operaciones determinadas en el periferico
     * @throws InterruptedException
     */
    public void authorizationAuthenticator() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        String code="0500";
        byte[] auth = cripto.getAuthenticator();
        cripto.generateNonce();
        code = code +  cripto.bytesToHex(auth) + cripto.bytesToHex(cripto.getNonce());
        Log.d("authoriAuthenticator", "EL codigo es: "+ code);
        Log.d("authoriAuthenticator", "Shared Key: "+ cripto.bytesToHex(cripto.getSharedKey()));
        Log.d("authoriAuthenticator","Valor de Peri: "+cripto.bytesToHex(peripheralPublicKey)) ;
        Log.d("authoriAuthenticator","Valor de Central: "+cripto.bytesToHex(centralPublicKey.toBytes())) ;

        mBluetoothLeService.writePairingService(code);

    }

    /**
     * Metodo que escribe el autenticador del central junto con su tipo de idd su app id su nombre y un nonceABF
     * @throws InterruptedException
     */
    public void authorizationData() throws InterruptedException {

        TimeUnit.SECONDS.sleep(1);
        String code="0600";

        byte[] auth = cripto.getAuthenticator();
        String idType= "00";
        String appId = "00000000";
        String name = "4D61726320285465737429000000000000000000000000000000000000000000";
        byte[] nonceABF = cripto.generateNonce64();


        cripto.generateNonce();
        code = code +  cripto.bytesToHex(auth) + idType + appId + name + cripto.bytesToHex(nonceABF) + cripto.bytesToHex(cripto.getNonce());
        Log.d("authoriData", "EL codigo es: "+ code);
        Log.d("authoriData", "Shared Key: "+ cripto.bytesToHex(cripto.getSharedKey()));
        Log.d("authoriData","Valor de Peri: "+cripto.bytesToHex(peripheralPublicKey)) ;
        Log.d("authoriData","Valor de Central: "+cripto.bytesToHex(centralPublicKey.toBytes())) ;



        mBluetoothLeService.writePairingService(code);
    }

    /**
     * Metodo que lee el authorization ID
     * @throws InterruptedException
     */
    public void authorizationId() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);

        authid = true;
        Log.d("authorizationId", "Detnro del authorization id");

        readAuthorizeCharacteristic();
        TimeUnit.SECONDS.sleep(1);
        authid = false;

    }

    /**
     * Metodo que escribe el codigo de authorizationIdConfrimation para que se confirme si es verdad
     * este autenticador y el autenticador id
     * @throws InterruptedException
     */
    public void authorizationIdConfirmation() throws InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        idconfirmation=true;
        String code="1E00";
        String auth = cripto.bytesToHex(cripto.getAuthenticator());
        String authid= cripto.bytesToHex(cripto.getAuthorizationId());

        code = code + auth + authid;

        mBluetoothLeService.writePairingService(code);
        TimeUnit.SECONDS.sleep(1);

        readAuthorizeCharacteristic();
        TimeUnit.SECONDS.sleep(1);

        idconfirmation=false;
    }

    /**
     * Metodo que encripta los keyturner States y los manda al periferico encriptados
     * @throws InterruptedException
     */
    public void requestDataKeyturnerStates() throws InterruptedException {
        String id = cripto.bytesToHex(cripto.getAuthorizationId());
        cripto.generateNonce();
        //String message = id + "01000C00" + cripto.bytesToHex(cripto.getNonce());
        String message = "0200000001000C00" + cripto.bytesToHex(cripto.getNonce());
        Log.e("RequestDatKeySt", "VALOR DE LA SHARED: " + cripto.bytesToHex(cripto.getSharedKey()));

        byte[] encriptado, desencriptado;
        encriptado = Criptografia.encrypt_message(cripto.getSharedKey(),message.length(),message.getBytes(), null);
        Log.e("ENCRYPT", "MENSAJE ENCRIPTADO: " + cripto.bytesToHex(encriptado) );
        Log.e("ENCRYPT","ENCRIPTADO EN BYTES: "+ encriptado);
        desencriptado = Criptografia.decrypt_message(cripto.getSharedKey(), encriptado);
        Log.e("ENCRYPT", "MENSAJE DESENCRIPTADO: " + cripto.hexToAscii(cripto.bytesToHex(desencriptado)));
        Log.e("ENCRYPT", "MENSAJE EN BYTES: " + message);

        mBluetoothLeService.writePairingServiceByteArray(encriptado);

        TimeUnit.SECONDS.sleep(1);

       // mBluetoothLeService.writePairingService(message);


        TimeUnit.SECONDS.sleep(1);


    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(cripto.getAuthorizationId() != null) {
            outState.putByteArray("authorizationId", cripto.getAuthorizationId());
            Log.e("OnSaveInstanceState", "AuthorizationID guardada");

        }
        if(cripto.getSharedKey() != null){
            outState.putByteArray("sharedKey", cripto.getSharedKey());
            Log.e("OnSaveInstanceState", "Shared guardada");
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        cripto.setAuthorizationId(cripto.bytesToHex(savedInstanceState.getByteArray("authorizationId")));
        cripto.setSharedKey(savedInstanceState.getByteArray("sharedKey"));
        Log.e("RestoreInstanceState", "Shared Key restablecida: " + cripto.getSharedKey());
        Log.e("RestoreInstanceState", "AuthorizationId restablecida: " + cripto.getAuthorizationId());

    }


}
