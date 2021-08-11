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
    private String challenge;
    private byte[] authenticator;
    private final int NONCE_64_LENGHT = 32;



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

    public byte[] generateNonce64(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_64_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_64_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ MainActivity.bytesToHex(nonc));
        return nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }


    public byte[] diffieHellman(byte[] centralPublicKey){
        NaCl.sodium();
        byte[] shared_key = new byte[Sodium.crypto_core_hsalsa20_outputbytes()];

        byte[] dhk = privateKey.toBytes();
        if(Sodium.crypto_scalarmult_curve25519(shared_key,dhk ,centralPublicKey) != 0){
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
        Log.d(TAG, "El valor de la shared key es: "+ MainActivity.bytesToHex(shared_key));
        Log.d(TAG, "El valor de la derivate shared key es: "+  MainActivity.bytesToHex(dhk));
        return dhk;
    }

    public void setChallenge(String challen){
        challenge=challen;
    }

    public String getChallenge(){
        return challenge;
    }

    public void setAuthenticator(byte[] auth){
        authenticator = auth;
    }

    public byte[] getAuthenticator(){
        return authenticator;
    }

    public void setSharedKey(byte[] sKey){
        sharedKey = sKey;
    }
    public byte[] getSharedKey(){
        return sharedKey;
    }

    public void calculateAuthenticator(String r){

        NaCl.sodium();
        byte[] authenticator = new byte[Sodium.crypto_auth_hmacsha256_bytes()];
        byte[] in = r.getBytes();
        Sodium.crypto_auth_hmacsha256(authenticator, in,in.length, sharedKey );
        Log.d("Calculate Authenticator", "Valor de Peripherico Authenticator: " + MainActivity.bytesToHex(authenticator ));
        Log.d("Calculate Authenticator", "Valor de Peripherico Shared Key: " + MainActivity.bytesToHex(sharedKey));

        this.setAuthenticator(authenticator);


    }


    }
