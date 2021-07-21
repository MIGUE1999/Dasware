package com.bluetooth.perifericoble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.security.KeyPairGenerator;
import java.util.UUID;


public class DOORProfile {

    private static final String TAG = DOORProfile.class.getSimpleName();
    /*
    //Service UUID to expose our DOOR characteristics
    public static UUID DOOR_SERVICE = UUID.fromString("a1564634-26d8-49fb-9a77-78855c308412");

    //RX, Write characteristic
    public static UUID DOOR_READ_WRITE_CHAR = UUID.fromString("a1564635-26d8-49fb-9a77-78855c308412");
    public static UUID DOOR_READ_WRITE_DESCRIPTOR = UUID.fromString("a1564636-26d8-49fb-9a77-78855c308412");

    public final static int DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE;

*/

    //Service UUID to expose our UART characteristics
    public static UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    //RX, Write characteristic
    public static UUID RX_WRITE_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    //TX Read Notify
    //readchar=currentime
    public static UUID TX_READ_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public final static int DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE;

    public static final String state = "0xAA";      //en hexadecimal(int) es: 30784141
    public static  byte[]  door_state= state.getBytes();  //0 cerrado 1 abierto
    public static final byte abierto = (byte) 1;
    public static final byte cerrado = (byte) 0;

    //AUTHORIZATION APP

    //Service UUID to expose our UART characteristics
    public static UUID KEYTURNER_PAIRING_SERVICE = UUID.fromString("6e400011-b5a3-f393-e0a9-e50e24dcca9e");
    //AUTHORIZATION characteristic Propierties: WRITE & INDICATE
    public static UUID AUTHORIZATION_CHAR = UUID.fromString("00002a3b-0000-1000-8000-00805f9b34fb");




    public static void initDoorState(){
        door_state[0]=1;
    }


    public static String getStateDescription(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "Connected";
            case BluetoothProfile.STATE_CONNECTING:
                return "Connecting";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "Disconnected";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "Disconnecting";
            default:
                return "Unknown State "+state;
        }
    }


    public static String getStatusDescription(int status) {
        switch (status) {
            case BluetoothGatt.GATT_SUCCESS:
                return "SUCCESS";
            default:
                return "Unknown Status "+status;
        }
    }


    public static void changeDoorState(byte[] newstate){
       door_state=newstate;
    }

    public static byte getDoorStateByte(){
        if(door_state[0] == 0) {
            door_state[0] = cerrado;
            Log.d(Constants.TAG, "Door_state se pone a 0 inicializando");
        }
       return door_state[0];
    }

    public static byte[] getDoorState(){
        return door_state;
    }


    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Current Time Service.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static BluetoothGattService createUartService() {
        BluetoothGattService service = new BluetoothGattService(UART_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Current Time characteristic
        BluetoothGattCharacteristic read_char = new BluetoothGattCharacteristic(TX_READ_CHAR,
                //Read-only characteristic, supports notifications
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY| BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        read_char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(CLIENT_CONFIG,
                //Read/write descriptor
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        read_char.addDescriptor(configDescriptor);
        Log.d("CreateUartService", "valor estado"+ getDoorState());
        read_char.setValue(getDoorState());



        service.addCharacteristic(read_char);

        return service;
    }


    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * KeyTurner Service.
     */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public static BluetoothGattService createKeyTurnerPairingService() {

        BluetoothGattService service = new BluetoothGattService(KEYTURNER_PAIRING_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Authorization characteristic
        BluetoothGattCharacteristic authorization_char = new BluetoothGattCharacteristic(AUTHORIZATION_CHAR,
                //Write characteristic, supports indications
                BluetoothGattCharacteristic.PROPERTY_INDICATE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE );
        authorization_char.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        service.addCharacteristic(authorization_char);

        return service;
    }


}