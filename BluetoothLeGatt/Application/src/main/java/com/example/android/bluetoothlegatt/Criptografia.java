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


    /**
     * Constructor de la clase criptografia inicia todos los atributos a null
     */

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
        Log.d(TAG, "Nonce Creado, es: "+ bytesToHex(nonc));
        nonce= nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    /**
     * Crea un nonce de 64 bits aleatorios
     * @return nonc nonce de 64 bits
     */
    public byte[] generateNonce64(){
        NaCl.sodium();
        byte[] nonc= new byte[NONCE_64_LENGHT];
        Sodium.randombytes_buf(nonc, NONCE_64_LENGHT);   //creacion del nonce
        Log.d(TAG, "Nonce Creado, es: "+ bytesToHex(nonc));
        return nonc;

        //Sodium.crypto_scalarmult_curve25519(q,n,p)
    }

    /**
     * Obtiene el nonce de 4 bits
     * @return nonce de 4 bits
     */
    public byte[] getNonce(){ return nonce; }

    /**
     * Metodo que crea la shared_key mediante el algoritmo de Diffie-Hellman
     * @param peripheralPubKey llave publica del periferico
     * @return dhk que es la shared key obtenida mediante Diffie-Hellman
     */

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
        Log.d("Calculate Authenticator", "Valor de R: " + r);
        NaCl.sodium();
        byte[] authenticator = new byte[Sodium.crypto_auth_hmacsha256_bytes()];
        byte[] in = r.getBytes();
        Sodium.crypto_auth_hmacsha256(authenticator, in,in.length, sharedKey );
        Log.d("Calculate Authenticator", "Valor de Central Authenticator: " + bytesToHex(authenticator ));
        Log.d("Calculate Authenticator", "Valor de Shared Key: " + bytesToHex(sharedKey));

        this.setAuthenticator(authenticator);


    }

    /**
     * Metodo que modifica la llave privada del periferico
     * @param peripheralPrivateKey llave privada del periferico
     */

    public void setPeripheralPrivateKey(PrivateKey peripheralPrivateKey) {
        this.peripheralPrivateKey = peripheralPrivateKey;
    }

    /**
     * Metodo que modifica la llave publica del periferico
     * @param peripheralPublicKey llave publica del periferico
     */

    public void setPeripheralPublicKey(PublicKey peripheralPublicKey) {
        this.peripheralPublicKey = peripheralPublicKey;
    }
    /**
     * Metodo que modifica la llave publica del periferico
     * @param peripheralPublicKey llave publica del periferico
     */
    public void setPeripheralPublicKey(byte[] peripheralPublicKey) {
        this.peripheralPublicKey = new PublicKey(peripheralPublicKey);
    }

    public PrivateKey getPeripheralPrivateKey() {
        return peripheralPrivateKey;
    }

    public PublicKey getPeripheralPublicKey() {
        return peripheralPublicKey;
    }
    /**
     * Metodo que obtiene la llave publica del periferico en bytes
     *
     */
    public byte[] getPeripheralPublicKeyBytes() {
        return peripheralPublicKey.toBytes();
    }

    /**
     * Metodo que modifica el challenge
     * @param challenge
     */
    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    /**
     * Metodo que obtiene el challenge
     * @return challenge obtenido
     */
    public byte[] getChallenge(){
        return challenge;
    }

    /**
     * Metodo que modifica el authenticator
     * @param authenticator
     */
    public void setAuthenticator(byte[] authenticator) {
        this.authenticator = authenticator;
    }

    /**
     * Metodo que obtiene el authenticator
     * @return athenticator
     */
    public byte[] getAuthenticator(){
        return authenticator;
    }

    /**
     * Metodo que modifica el authorization id
     * @param authorizationId
     */
    public void setAuthorizationId(String authorizationId) {
        this.authorizationId = authorizationId.getBytes();
    }

    /**
     * Metodo que obtiene el authorizationId
     * @return authorizationId
     */
    public byte[] getAuthorizationId() {
        return authorizationId;
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


    //Conversores de datos

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /**
     *     Helper function converts byte array to hex string
     *     for priting
     */

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     *     Helper function converts ASCII array to hex string
     *     for priting
     */
    private static String asciiToHex(String asciiStr) {
        char[] chars = asciiStr.toCharArray();
        StringBuilder hex = new StringBuilder();
        for (char ch : chars) {
            hex.append(Integer.toHexString((int) ch));
        }

        return hex.toString();
    }

    /**
     *     Helper function converts hex string to byte array
     *     for priting
     */
    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }


}
