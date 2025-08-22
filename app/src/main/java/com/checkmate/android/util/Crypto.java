package com.checkmate.android.util;

import java.security.*;

import javax.crypto.*;
import javax.crypto.spec.*;

import android.util.Base64;
import android.util.Log;

public class Crypto {
//    "EncryptionKey": "fp!hF#m=qBp4J47fEq8*Z_sG",
//            "IV": "k8sU-#6z"

    public static final String encrypt_key = "fp!hF#m=qBp4J47fEq8*Z_sG";
    public static final String encrypt_IV = "k8sU-#6z";

    public static String Decrypt(String text, String key) {
        try {
            Cipher cipher = Cipher.getInstance
                    ("AES/CBC/PKCS5Padding"); //this parameters should not be changed
            byte[] keyBytes = new byte[16];
            byte[] b = key.getBytes("UTF-8");
            int len = b.length;
            if (len > keyBytes.length)
                len = keyBytes.length;
            System.arraycopy(b, 0, keyBytes, 0, len);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] results = new byte[text.length()];
            try {
                results = cipher.doFinal(Base64.decode(text, Base64.DEFAULT));
            } catch (Exception e) {
                Log.i("Error in Decryption", e.toString());
            }
            Log.i("Data", new String(results, "UTF-8"));
            return new String(results, "UTF-8"); // it returns the result as a String
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String Encrypt(String text, String key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] keyBytes = new byte[16];
        byte[] b = key.getBytes("UTF-8");
        int len = b.length;
        if (len > keyBytes.length)
            len = keyBytes.length;
        System.arraycopy(b, 0, keyBytes, 0, len);
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(keyBytes);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        byte[] results = cipher.doFinal(text.getBytes("UTF-8"));
        return Base64.encodeToString(results, Base64.DEFAULT);
    }
}