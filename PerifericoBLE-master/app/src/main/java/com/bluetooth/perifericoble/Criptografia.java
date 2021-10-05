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



    /**
     * Constructor de la clase criptografia inicia todos los atributos a null
     */

    public Criptografia(){
        publicKey = null;
        privateKey = null;
        nonce = null;
        sharedKey = null;
    }

    /**
     * Metodo que genera una pareja de llaves publica y privada
     */
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

    /**
     * Obitene la private key
     * @return privateKey llave privada de la clase criptografia
     */
    public org.libsodium.jni.keys.PrivateKey getPrivateKey(){
        return privateKey;
    }
    /**
     * Obitene la public key
     * @return publicKey llave publica de la clase criptografia
     */
    public org.libsodium.jni.keys.PublicKey getPublicKey(){
        return publicKey;
    }

    /**
     * Crea un nonce de 4 bits aleatorios
     */
    public void generateNonce(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ MainActivity.bytesToHex(nonc));
        nonce= nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    /**
     * Obtiene el nonce de 4 bits
     * @return nonce de 4 bits
     */
    public byte[] getNonce(){ return nonce; }

    /**
     * Crea un nonce de 64 bits aleatorios
     * @return nonc nonce de 64 bits
     */
    public byte[] generateNonce64(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_64_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_64_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ MainActivity.bytesToHex(nonc));
        return nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    /**
     * Metodo que crea la shared_key mediante el algoritmo de Diffie-Hellman
     * @param centralPublicKey llave publica del periferico
     * @return dhk que es la shared key obtenida mediante Diffie-Hellman
     */

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
    /**
     * Modifica la sharedKey mediante el parametro
     * @param sKey shared key nueva
     */
    public void setSharedKey(byte[] sKey){
        sharedKey = sKey;
    }
    /**
     * Obtiene la sharedKey
     * @return shared Key de la clase
     */
    public byte[] getSharedKey(){
        return sharedKey;
    }


    /**
     * Metodo que calcula el authenticator del sender del mensaje
     * @param r
     */
    public void calculateAuthenticator(String r){

        NaCl.sodium();
        byte[] authenticator = new byte[Sodium.crypto_auth_hmacsha256_bytes()];
        byte[] in = r.getBytes();
        Sodium.crypto_auth_hmacsha256(authenticator, in,in.length, sharedKey );
        Log.d("Calculate Authenticator", "Valor de Peripherico Authenticator: " + MainActivity.bytesToHex(authenticator ));
        Log.d("Calculate Authenticator", "Valor de Peripherico Shared Key: " + MainActivity.bytesToHex(sharedKey));

        this.setAuthenticator(authenticator);


    }

    /**
     * Metodo para encriptar un mensaje mediante la sharedkey
     * @param shared_key llave compartida entre periferico y central
     * @param auth_id authorizationid del sender
     * @param pdata payload data, es decir los datos limpios sin ningun tipo
     * @param nonce nonce para aumentar la seguridad
     * @return mensaje encriptado
     */
    static byte[] encrypt_message(byte[] shared_key, long auth_id, byte[] pdata, byte[] nonce) {
        // nonce is provide only for testing purposes!
        if (nonce == null) {
            nonce = new byte[Sodium.crypto_secretbox_noncebytes()];
            Sodium.randombytes(nonce, nonce.length);
        }

        if (nonce.length != Sodium.crypto_secretbox_noncebytes()) {
            Log.e("encrypt_message", "incorrect nonce length: " + nonce.length + " (expected " + Sodium.crypto_secretbox_noncebytes() + ")");
            return null;
        }

        // write auth_id
        byte[] message = new byte[4 + pdata.length + 2];
        NukiTools.write32_auth_id(message, 0, auth_id);

        // write command_id + payload
        System.arraycopy(pdata, 0, message, 4, pdata.length);

        // write crc
        int crc = NukiTools.crc16(message, 0, message.length - 2);
        NukiTools.write16(message, message.length - 2, crc);

        // encrypt
        byte[] encrypted = new byte[Sodium.crypto_secretbox_macbytes() + message.length];
        if (Sodium.crypto_secretbox_easy(encrypted, message, message.length, nonce, shared_key) != 0) {
            Log.e("encrypt_message", "crypto_secretbox_easy failed");
            return null;
        }

        // assemble encrypted message
        return NukiTools.concat(nonce, NukiTools.from32_auth_id(auth_id), NukiTools.from16(encrypted.length), encrypted);
    }



        /**
     * Metodo para desencriptar un mensaje
     * @param shared_key llave compartida entre periferico y central
     * @param msg mensaje a desencriptar
     * @return command_id + payload (without auth_id/crc fields)
     */

    static byte[] decrypt_message(byte[] shared_key, byte[] msg) {
        int nonce_length = Sodium.crypto_secretbox_noncebytes();
        int header_length = nonce_length + 4 + 2; // nonce + auth_id + length field size

        // nonce + auth_id + length + encrypted(macbytes + auth_id + command_id + crc)
        int min_msg_length = Sodium.crypto_secretbox_noncebytes() + 4 + 2 + Sodium.crypto_secretbox_macbytes() + 8;
        if (msg == null || msg.length < min_msg_length) {
            return null;
        }

        int length = NukiTools.read16(msg, nonce_length + 4);

        if (msg.length != (header_length + length)) {
            return null;
        }

        byte[] nonce = new byte[nonce_length];
        System.arraycopy(msg, 0, nonce, 0, nonce.length);
        long auth_id = NukiTools.read32_auth_id(msg, nonce.length);

        byte[] encrypted = new byte[length];
        System.arraycopy(msg, nonce_length + 4 + 2, encrypted, 0, encrypted.length);

        byte[] decrypted = new byte[length - Sodium.crypto_secretbox_macbytes()];
        if (Sodium.crypto_secretbox_open_easy(decrypted, encrypted, encrypted.length, nonce, shared_key) != 0) {
            Log.e("decrypt_message", "crypto_secretbox_easy failed");
            return null;
        }

        if (decrypted.length < 6) {
            return null;
        }

        // check auth_id
        if (auth_id != NukiTools.read32_auth_id(decrypted, 0)) {
            Log.e("decrypt_message", "auth_id mismatch");
            return null;
        }

        // check crc
        int crc_calc = NukiTools.crc16(decrypted, 0, decrypted.length - 2);
        int crc_read = NukiTools.read16(decrypted, decrypted.length - 2);
        if (crc_calc != crc_read) {
            Log.e("decrypt_message", "crc mismatch");
            return null;
        }

        // strip auth_id and crc
        byte[] ret = new byte[decrypted.length - 6];
        System.arraycopy(decrypted, 4, ret, 0, ret.length);

        // return command_id + payload
        return ret;
    }





}
