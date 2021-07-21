package com.example.android.bluetoothlegatt;

import android.bluetooth.BluetoothGattDescriptor;

import java.util.UUID;

public class ServerUUIDS {


        public static final String TAG="BLECentral";

        public static UUID UART_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
        //RX, Write characteristic
       // public static UUID RX_WRITE_CHAR = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
        //TX Read Notify
        //readchar=currentime
        public static UUID TX_READ_CHAR = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
        public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
        public final static int DESCRIPTOR_PERMISSION = BluetoothGattDescriptor.PERMISSION_WRITE;

        //AUTHORIZATION SERVICE
        public static UUID KEYTURNER_PAIRING_SERVICE = UUID.fromString("6e400011-b5a3-f393-e0a9-e50e24dcca9e");
        public static UUID AUTHORIZATION_CHAR = UUID.fromString("00002a3b-0000-1000-8000-00805f9b34fb");


}
