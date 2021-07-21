package com.bluetooth.perifericoble;

import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.KeyAgreement;

public class Criptografia {

    private PublicKey pubKey = null;
    private PrivateKey privKey = null;
    private KeyPair keyPair = null;
    private KeyAgreement clientKeyAgree = null;

    public Criptografia(){
        try {

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            SecureRandom random = new SecureRandom();
            keyPairGenerator.initialize(256, random);
            keyPair = keyPairGenerator.generateKeyPair();

            pubKey = keyPair.getPublic();
            privKey = keyPair.getPrivate();

            Log.d("TAG","Public Key:" + pubKey.toString());


            Log.d("TAG","Private Key: " + privKey.toString());

            boolean keyPairMatches = privKey.equals(pubKey);
            if(keyPairMatches)
                Log.d("TAG", "keypairMATCHES");
            else
                Log.d("TAG", "KeypairNO");


        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }

    public PublicKey getPubKey(){
        return pubKey;
    }

}
