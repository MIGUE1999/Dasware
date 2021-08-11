package com.example.android.bluetoothlegatt;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import java.io.FileWriter;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.logging.Level;

import javax.crypto.KeyAgreement;

public class Criptografia {

    private static final String TAG = "Criptografia";
    private final int NONCE_LENGHT = 2;
    private final int NONCE_64_LENGHT = 32;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private PublicKey peripheralPublicKey;
    private PrivateKey peripheralPrivateKey;
    private byte[] nonce;
    private byte[] sharedKey;
    private byte[] challenge;
    private byte[] authenticator;
    private byte[] authorizationId;




    public Criptografia(){
        publicKey = null;
        privateKey = null;
        peripheralPrivateKey = null;
        peripheralPublicKey = null;
        nonce = null;
        sharedKey = null;
        challenge=null;
        authenticator=null;
    }

    public void generateKeyPair(){
        NaCl.sodium();
        byte[] seed = new Random().randomBytes(SodiumConstants.SECRETKEY_BYTES);
        org.libsodium.jni.keys.KeyPair key = new org.libsodium.jni.keys.KeyPair(seed);
        if(key.getPrivateKey() != null){
            privateKey = key.getPrivateKey();
            Log.d(TAG, "Private Key "+key.getPrivateKey());
        }
        if(key.getPublicKey() != null){
            publicKey = key.getPublicKey();
            Log.d(TAG, "Public Key "+key.getPublicKey());
        }
    }

    public org.libsodium.jni.keys.PrivateKey getPrivateKey(){
        return privateKey;
    }

    public org.libsodium.jni.keys.PublicKey getPublicKey(){
        return publicKey;
    }


    public void generateNonce(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ bytesToHex(nonc));
        nonce= nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    public byte[] generateNonce64(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_64_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_64_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ bytesToHex(nonc));
        return nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    public byte[] getNonce(){ return nonce; }

    public byte[] diffieHellman(byte[] peripheralPubKey){
        NaCl.sodium();
        byte[] shared_key = new byte[Sodium.crypto_core_hsalsa20_outputbytes()];

        byte[] dhk = privateKey.toBytes();
        if(Sodium.crypto_scalarmult_curve25519(shared_key,dhk ,peripheralPubKey) != 0){
            Log.e(TAG, "Crypto_scalarmult_curve25519 FAILED");
            return null;
        }
        byte[] inv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] sigma = "expand 32-byte k".getBytes();
        if (sigma.length != 16) {
            Log.e(TAG, "wrong sigma length");
            return null;
        }
        if (Sodium.crypto_core_hsalsa20(dhk, inv, shared_key, sigma) != 0) {
            Log.e(TAG, "crypto_core_hsalsa20 failed");
            return null;
        }
        Log.d(TAG, "El valor de la shared key es: "+ bytesToHex(shared_key));
        Log.d(TAG, "El valor del derivate shared key es: "+  bytesToHex(dhk));
        return dhk;
    }

    public void setSharedKey(byte[] sKey){
        sharedKey = sKey;
    }
    public byte[] getSharedKey(){
        return sharedKey;
    }
    public void calculateAuthenticator(String r){
        Log.d("Calculate Authenticator", "Valor de R: " + r);
        NaCl.sodium();
        byte[] authenticator = new byte[Sodium.crypto_auth_hmacsha256_bytes()];
        byte[] in = r.getBytes();
        Sodium.crypto_auth_hmacsha256(authenticator, in,in.length, sharedKey );
        Log.d("Calculate Authenticator", "Valor de Central Authenticator: " + bytesToHex(authenticator ));
        Log.d("Calculate Authenticator", "Valor de Shared Key: " + bytesToHex(sharedKey));

        this.setAuthenticator(authenticator);


    }

    public void setPeripheralPrivateKey(PrivateKey peripheralPrivateKey) {
        this.peripheralPrivateKey = peripheralPrivateKey;
    }

    public void setPeripheralPublicKey(PublicKey peripheralPublicKey) {
        this.peripheralPublicKey = peripheralPublicKey;
    }

    public void setPeripheralPublicKey(byte[] peripheralPublicKey) {
        this.peripheralPublicKey = new PublicKey(peripheralPublicKey);
    }

    public PrivateKey getPeripheralPrivateKey() {
        return peripheralPrivateKey;
    }

    public PublicKey getPeripheralPublicKey() {
        return peripheralPublicKey;
    }

    public byte[] getPeripheralPublicKeyBytes() {
        return peripheralPublicKey.toBytes();
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    public byte[] getChallenge(){
        return challenge;
    }

    public void setAuthenticator(byte[] authenticator) {
        this.authenticator = authenticator;
    }
    public byte[] getAuthenticator(){
        return authenticator;
    }

    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId.getBytes();
    }

    public byte[] getAuthorizationId() {
        return authorizationId;
    }

    //Conversores de datos

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

    private static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }

        return hex.toString();
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }
}
