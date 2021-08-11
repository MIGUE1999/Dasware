package com.bluetooth.perifericoble;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.Bundle;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.keys.KeyPair;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;


import static com.bluetooth.perifericoble.DOORProfile.TX_READ_CHAR;
import static com.bluetooth.perifericoble.DOORProfile.door_state;
import static com.bluetooth.perifericoble.DOORProfile.getDoorState;
import static com.bluetooth.perifericoble.DOORProfile.initDoorState;


public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private TextView mLocalTimeView;
    private TextView mPairingCodeView;

    private BluetoothManager mBluetoothManager;
    //private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;

    private Set<BluetoothDevice> mRegisteredDevices = new HashSet<>();

   private Criptografia cripto = new Criptografia();
    private byte[] centralPublicKey = null;
    private byte[] sharedKey = null;
    private byte[] nonce64 = null;

    User usuario;

    private boolean verifyauth= false;
    private ArrayList<User> usuarios = new ArrayList<User>();

    public final int REQUEST_COMAND_SIZE=4;
    public final int PUBLIC_KEY_COMAND_SIZE=8;
    public final int KEY_SIZE = 68;
    public final int NONCE_SIZE = 72;
    public final int ID_TYPE_SIZE = 70;
    public final int APP_ID_SIZE = 78;
    public final int NAME_SIZE = 142;
    public final int NONCE_ABF = 206;
    public final int ID_CONFIRMATION = 76;
    public boolean authid = false;




    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static BluetoothGattService pairingService = DOORProfile.createKeyTurnerPairingService();


    private static final int REQUEST_LOCATION = 0;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocalTimeView = (TextView) findViewById(R.id.text_time);

        usuario = new User();


        // Devices with a display should not go to sleep
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        // We can't continue without proper Bluetooth support
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            try {
                startServer();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        cripto.generateKeyPair();


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register for system clock events
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(mTimeReceiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mTimeReceiver);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopAdvertising();
            }
        }

        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @param bluetoothAdapter System {@link BluetoothAdapter}.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }


    /**
     * Listens for system time changes and triggers a notification to
     * Bluetooth subscribers.
     */
    private BroadcastReceiver mTimeReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {

            notifyRegisteredDevices();
            updateLocalUi();

        }
    };

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    try {
                        startServer();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        stopAdvertising();
                    }
                    break;
                default:
                    // Do nothing
            }

        }
    };
    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        }
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();
        }

        AdvertiseData data = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(false)
                    .addServiceUuid(new ParcelUuid(DOORProfile.UART_SERVICE))
                    .build();
        }



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothLeAdvertiser
                    .startAdvertising(settings, data, mAdvertiseCallback);
            //mBluetoothLeAdvertiser
              //      .startAdvertising(settings,datapairing,mAdvertiseCallback);
        }
    }

    /**
     * Stop Bluetooth advertisements.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);

    }

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void startServer() throws InterruptedException {
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mGattServer.addService(DOORProfile.createUartService());
        TimeUnit.SECONDS.sleep(1);
        mGattServer.addService(pairingService);


        // Initialize the local UI
        updateLocalUi();


    }

    /**
     * Shut down the GATT server.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void stopServer() {
        if (mGattServer == null) return;

        mGattServer.close();
    }

    /**
     * Update graphical UI on devices that support it with the current time.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void updateLocalUi() {

        final byte[] data = DOORProfile.getDoorState();
        Log.d(TAG, "Valor del door: "+ bytesToHex(data));
        Log.d(TAG, "Valor de la public key en la carateristica: "+ bytesToHex(storage));
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            mLocalTimeView.setText(bytesToHex(data));


        }
    }






    /**
     * Callback to receive information about the advertisement process.
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void notifyRegisteredDevices() {
        if (mRegisteredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered");
            return;
        }
        byte[] doorstate = DOORProfile.getDoorState();

        Log.i(TAG, "Sending update to " + mRegisteredDevices.size() + " subscribers");
        for (BluetoothDevice device : mRegisteredDevices) {
            BluetoothGattCharacteristic stateCharacteristic = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                stateCharacteristic = mGattServer
                        .getService(DOORProfile.UART_SERVICE)
                        .getCharacteristic(DOORProfile.TX_READ_CHAR);
            }
            stateCharacteristic.setValue(doorstate);
            mGattServer.notifyCharacteristicChanged(device, stateCharacteristic, false);
        }
    }



    /**
     * Callback to handle incoming requests to the GATT server.
     * All read/write requests for characteristics and descriptors are handled here.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mRegisteredDevices.remove(device);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            long now = System.currentTimeMillis();
            Log.d("OnCharReadReq", "Valor de la caracteristica: " + characteristic.getUuid());
            //String sendval = "0xAA";

            if(authid){
                String comando = "0700";
                String UUID = "12345678901234567890123456789012";
                cripto.generateNonce();
                comando = comando + bytesToHex(cripto.getAuthenticator()) + usuario.getAuthorizationId() + UUID + bytesToHex(cripto.generateNonce64()) + bytesToHex(cripto.getNonce());
                Log.d("ONcharReadReq", "EL valor de 0700 es: " + comando);
                characteristic.setValue(comando.getBytes());

                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        comando.getBytes());

                authid=false;
            }
            else if (DOORProfile.TX_READ_CHAR.equals(characteristic.getUuid()) || DOORProfile.AUTHORIZATION_CHAR.equals(characteristic.getUuid())) {

                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        characteristic.getValue());
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            Log.i("onCharWriteReq", "Characteristic UUID: " + characteristic.getUuid().toString());
            Log.d("onCharWriteReq", "Comando: " + hexToAscii(bytesToHex(value)).toUpperCase());


            if (DOORProfile.TX_READ_CHAR.equals(characteristic.getUuid())) {

                //IMP: Copy the received value to storage
                storage = value;
                DOORProfile.changeDoorState(value);
                if (responseNeeded) {
                    characteristic.setValue(value);
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            value);
                    Log.d("OnCharWriteTX_R_CHAR", "Received data" + bytesToHex(value));
                    Log.d("OnCharWriteTX_R_CHAR", "Valor Final de door_read_write: " + bytesToHex(characteristic.getValue()));

                }

            }

            if (DOORProfile.AUTHORIZATION_CHAR.equals(characteristic.getUuid())) {
                //cadena en la que compruebo el comando
                String val = hexToAscii(bytesToHex(value)).toUpperCase();
                String comando = "";
                //IMP: Copy the received value to storage
                storage = value;

                for (int i = 0; i < REQUEST_COMAND_SIZE; i++) {
                    comando = comando + val.charAt(i);
                }



                switch (comando) {
                    case "0100":

                        comando = "";

                        for (int i = REQUEST_COMAND_SIZE; i < PUBLIC_KEY_COMAND_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        if (comando.equals("0300")) {
                            if (responseNeeded) {
                                characteristic.setValue(cripto.getPublicKey().toBytes());
                                mGattServer.sendResponse(device,
                                        requestId,
                                        BluetoothGatt.GATT_SUCCESS,
                                        0,
                                        cripto.getPublicKey().toBytes());
                                Log.d("OnCharWriteReq 01000300", "Valor de la Peripheral public key: " + bytesToHex(characteristic.getValue()));
                                storage = characteristic.getValue();
                                comando="";
                            }
                        }
                        break;

                    case "0300":
                        comando = "";

                        for (int i = REQUEST_COMAND_SIZE; i < KEY_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        if (responseNeeded) {
                            characteristic.setValue(hexStringToByteArray(comando));
                            mGattServer.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    hexStringToByteArray(comando));
                            Log.d(TAG, "Received  data on " + characteristic.getUuid().toString());
                            Log.d("OnCharWriteReq 0300", "Valor de la Central public key: " + bytesToHex(characteristic.getValue()));
                            centralPublicKey = characteristic.getValue();

                            sharedKey = cripto.diffieHellman(centralPublicKey);
                            cripto.setSharedKey(sharedKey);

                        }
                        comando = "";

                        for (int i = KEY_SIZE; i < NONCE_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        Log.d("OnCharWriteReq 0300", "El nonce es: "+ comando);
                        break;
                    case "0400":
                        comando = "";

                        for (int i = REQUEST_COMAND_SIZE; i < KEY_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        Log.d("OnCharWrite 0400", "Nonce 64: "+ comando);
                        cripto.setChallenge(comando);

                        if (responseNeeded) {
                            characteristic.setValue(hexStringToByteArray(comando));
                            mGattServer.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    hexStringToByteArray(comando));
                        }
                        nonce64= hexStringToByteArray(comando);
                        comando = "";
                        for (int i = KEY_SIZE; i < NONCE_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        Log.d("OnCharWriteReq 0400", "El nonce es: "+ comando);

                        String r = bytesToHex(centralPublicKey) + bytesToHex(cripto.getPublicKey().toBytes()) + bytesToHex(nonce64);
                        Log.d("OnCharWriteReq 0400","El r es: "+ r);
                        cripto.calculateAuthenticator(r);

                        break;
                    case "0500":
                        comando = "";

                        for (int i = REQUEST_COMAND_SIZE; i < KEY_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        Log.d("OnCharWrite 0500", "Authorization: "+ comando);
                        cripto.setChallenge(comando);


                        if (responseNeeded) {
                            characteristic.setValue(hexStringToByteArray(comando));
                            mGattServer.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    hexStringToByteArray(comando));
                        }

                        byte[] auth= hexStringToByteArray(comando);
                        Log.d("OnCharWriteReq 0500 ", "AUTH que llega: "+ bytesToHex(auth));
                        Log.d("OnCharWriteReq 0500 ", "AUTH del periferico "+ bytesToHex(cripto.getAuthenticator()));


                        if(bytesToHex(auth).equals(bytesToHex(cripto.getAuthenticator()))){
                            verifyauth = true;
                            comando = "";
                            for (int i = KEY_SIZE; i < NONCE_SIZE; i++) {
                                comando += val.charAt(i);
                            }
                            Log.d("OnCharWriteReq 0500 ", "El nonce es: "+ comando);
                            Log.d("OnCharWriteReq 0500 ", "Verificacion Realizada con Exito");

                        }
                        else{
                            verifyauth=false;
                            Log.d("OnCharWriteReq 0500", "Error de Verificacion");
                        }

                        break;
                    case "0600":
                        comando = "";

                        for (int i = REQUEST_COMAND_SIZE; i < KEY_SIZE; i++) {
                            comando += val.charAt(i);
                        }
                        Log.d("OnCharWrite 0600", "Central Authorization: "+ comando);
                        //cripto.setAuthenticator(hexStringToByteArray(comando));


                        if (responseNeeded) {
                            characteristic.setValue(hexStringToByteArray(comando));
                            mGattServer.sendResponse(device,
                                    requestId,
                                    BluetoothGatt.GATT_SUCCESS,
                                    0,
                                    hexStringToByteArray(comando));
                        }

                        byte[] autho= hexStringToByteArray(comando);
                        Log.d("OnCharWriteReq 0600 ", "AUTH que llega: "+ bytesToHex(autho));
                        Log.d("OnCharWriteReq 0600 ", "AUTH del periferico "+ bytesToHex(cripto.getAuthenticator()));
                        Log.d("OnCharWriteReq 0600 ", "Shared Key "+ bytesToHex(cripto.getSharedKey()));


                        if(bytesToHex(autho).equals(bytesToHex(cripto.getAuthenticator()))){
                            verifyauth = true;
                            comando = "";

                            //A paritr de aquí comienza el cambio.


                            for (int i = KEY_SIZE; i < ID_TYPE_SIZE; i++) {
                                comando += val.charAt(i);
                            }
                            Log.d("OnCharWriteReq 0600 ", "El ID TYPE es: "+ comando);
                            usuario.setIdType(comando);

                            comando="";
                            for (int i = ID_TYPE_SIZE; i < APP_ID_SIZE; i++) {
                                comando += val.charAt(i);
                            }
                            Log.d("OnCharWriteReq 0600 ", "El APP ID es: "+ comando);
                            usuario.setAppId(comando);

                            comando="";
                            for (int i = APP_ID_SIZE; i < NAME_SIZE; i++) {
                                comando += val.charAt(i);
                            }
                            Log.d("OnCharWriteReq 0600 ", "El Name es: "+ comando);
                            usuario.setName(comando);

                            comando="";
                            for (int i = NAME_SIZE; i < NONCE_ABF; i++) {
                                comando += val.charAt(i);
                            }
                            Log.d("OnCharWriteReq 0600 ", "El NonceABF es: "+ comando);
                            usuario.setNonceABF(comando);
                            usuario.setAuthorizationId("2");
                            usuarios.add(usuario);

                            Log.d("OnCharWriteReq 0600 ", "Verificacion Realizada con Exito");
                            authid=true;
                        }
                        else{
                            verifyauth=false;
                            Log.d("OnCharWriteReq 0500", "Error de Verificacion");
                        }

                        break;
                    case "1E00":
                        String statuscompleted= "0E0000";
                        cripto.generateNonce();
                        String nonc= bytesToHex(cripto.getNonce());
                        statuscompleted+= nonc;
                        if (responseNeeded) {
                                mGattServer.sendResponse(device,
                                        requestId,
                                        BluetoothGatt.GATT_SUCCESS,
                                        0,
                                        hexStringToByteArray(statuscompleted));
                                characteristic.setValue(hexStringToByteArray(statuscompleted));
                        Log.d(TAG,"1E00 completado con exito") ;
                        }

                        break;

                    default:
                        Log.d("OnCHarWriteReq", "No coje bien Comando");
                        break;

                }

            }
        }


        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                            BluetoothGattDescriptor descriptor) {
            if (DOORProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                Log.d(TAG, "Config descriptor read");
                byte[] returnValue;
                if (mRegisteredDevices.contains(device)) {
                    returnValue = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
                } else {
                    returnValue = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                }
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        returnValue);
            } else {
                Log.w(TAG, "Unknown descriptor read request");
                mGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite, boolean responseNeeded,
                                             int offset, byte[] value) {
            if (DOORProfile.CLIENT_CONFIG.equals(descriptor.getUuid())) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: " + device);
                    mRegisteredDevices.add(device);
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: " + device);
                    mRegisteredDevices.remove(device);
                }

                if (responseNeeded) {
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request");
                if (responseNeeded) {
                    mGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }
        }
    };



/*

    //Send notification to all the devices once you write
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void sendOurResponse() {


        for (BluetoothDevice device : mConnectedDevices) {
            BluetoothGattCharacteristic readCharacteristic = mGattServer.getService(DOORProfile.UART_SERVICE)
                    .getCharacteristic(DOORProfile.TX_READ_CHAR);

            byte[] notify_msg = storage;
            String hexStorage = bytesToHex(storage);
            Log.d(TAG, "received string = " + bytesToHex(storage));


            if (hexStorage.equals("77686F616D69")) {

                notify_msg = "I am echo an machine".getBytes();

            } else if (bytesToHex(storage).equals("64617465")) {
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                Date date = new Date();
                notify_msg = dateFormat.format(date).getBytes();

            } else {
                //TODO: Do nothing send what you received. Basically echo
            }
            readCharacteristic.setValue(notify_msg);
            Log.d(TAG, "Sending Notifications" + notify_msg);
            boolean is_notified = mGattServer.notifyCharacteristicChanged(device, readCharacteristic, false);
            Log.d(TAG, "Notifications =" + is_notified);

        }

    }
*/








    private byte[] storage = hexStringToByteArray("1111");


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    //Helper function converts byte array to hex string
    //for priting
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    //Helper function converts hex string into
    //byte array
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static byte[] bigIntToByteArray( final int i ) {
        BigInteger bigInt = BigInteger.valueOf(i);
        return bigInt.toByteArray();
    }


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void requestLocationPermission() {
        Log.i(Constants.TAG, "Location permission has NOT yet been granted. Requesting permission.");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)){
            Log.i(Constants.TAG, "Displaying location permission rationale to provide additional context.");
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Permission Required");
            builder.setMessage("Please grant Location access so this application can perform Bluetooth scanning");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    Log.d(Constants.TAG, "Requesting permissions after explanation");
                    ActivityCompat.requestPermissions(MainActivity.this, new
                            String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
                }
            });
            builder.show();
        } else {
            ActivityCompat.requestPermissions(this, new
                    String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION) {
            Log.i(Constants.TAG, "Received response for location permission request.");
// Check if the only required permission has been granted
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
// Location permission has been granted
                Log.i(Constants.TAG, "Location permission has now been granted. Advertising.....");
                }
            else{
                Log.i(Constants.TAG, "Location permission was NOT granted.");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }

        return hex.toString();
    }

    private static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }

}