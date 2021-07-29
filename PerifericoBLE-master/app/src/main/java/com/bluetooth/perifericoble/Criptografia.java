package com.bluetooth.perifericoble;

import android.util.Log;

import org.libsodium.jni.NaCl;
import org.libsodium.jni.Sodium;
import org.libsodium.jni.SodiumConstants;
import org.libsodium.jni.crypto.Random;
import org.libsodium.jni.keys.PrivateKey;
import org.libsodium.jni.keys.PublicKey;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyAgreement;

public class Criptografia {

    private static final String TAG = "Criptografia";
    private final int NONCE_LENGHT = 2;
    private PublicKey publicKey;
    private PrivateKey privateKey;
    private byte[] nonce;
    private byte[] sharedKey;



    public Criptografia(){
        publicKey = null;
        privateKey = null;
        nonce = null;
        sharedKey = null;
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
        Log.d(TAG, "Nonce Creado, es: "+ MainActivity.bytesToHex(nonc));
        nonce= nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    public byte[] getNonce(){ return nonce; }

    public byte[] diffieHellman(byte[] centralPublicKey){
        NaCl.sodium();
        byte[] shared_key = new byte[Sodium.crypto_core_hsalsa20_outputbytes()];

        byte[] dhk = new byte[Sodium.crypto_scalarmult_curve25519_bytes()];
        if(Sodium.crypto_scalarmult_curve25519(dhk,shared_key ,centralPublicKey) != 0){
            Log.e(TAG, "Crypto_scalarmult_curve25519 FAILED");
            return null;
        }
        byte[] inv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        byte[] sigma = "expand 32-byte k".getBytes();
        if (sigma.length != 16) {
            Log.e(TAG, "wrong sigma length");
            return null;
        }
        if (Sodium.crypto_core_hsalsa20(shared_key, inv, dhk, sigma) != 0) {
            Log.e(TAG, "crypto_core_hsalsa20 failed");
            return null;
        }
        Log.d(TAG, "El valor de la shared key es: "+ MainActivity.bytesToHex(shared_key));
        Log.d(TAG, "El valor del dhk es: "+  MainActivity.bytesToHex(dhk));
        return shared_key;
    }


    }
