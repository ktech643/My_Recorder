package com.checkmate.android.util;

import android.content.Context;
import android.util.Base64;

import com.checkmate.android.AppPreference;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import static com.checkmate.android.AppConstant.CIPHER_ALGORITHM;
import static com.checkmate.android.AppConstant.KEY_SPEC_ALGORITHM;
import static com.checkmate.android.AppConstant.OUTPUT_KEY_LENGTH;
import static com.checkmate.android.AppConstant.PROVIDER;

/**
 * Created by James From CoderzHeaven on 5/2/18.
 */

public class EncryptDecryptUtils {

    public static EncryptDecryptUtils instance = null;

    public static EncryptDecryptUtils getInstance(Context context) {

        if (null == instance)
            instance = new EncryptDecryptUtils();

        return instance;
    }

    public static byte[] encode(SecretKey yourKey, byte[] fileData)
            throws Exception {
        byte[] data = yourKey.getEncoded();
        SecretKeySpec skeySpec = new SecretKeySpec(data, 0, data.length, KEY_SPEC_ALGORITHM);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        return cipher.doFinal(fileData);
    }

    public static boolean encode(SecretKey yourKey, File srcFile, String outPath)
            throws Exception {
        byte[] data = yourKey.getEncoded();
        SecretKeySpec skeySpec = new SecretKeySpec(data, 0, data.length, KEY_SPEC_ALGORITHM);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(new byte[cipher.getBlockSize()]));

        FileInputStream inFile = new FileInputStream(srcFile.getAbsolutePath());
        FileOutputStream outFile = new FileOutputStream(outPath);
        CipherInputStream cis = new CipherInputStream(inFile, cipher);

        doCopy(cis, outFile);

        return true;
    }

    static void doCopy(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[64];
        int numBytes;
        while ((numBytes = is.read(bytes)) != -1) {
            os.write(bytes, 0, numBytes);
        }
        os.flush();
        os.close();
        is.close();
    }

    public static boolean decode(SecretKey yourKey, File srcFile, String outPath)
            throws Exception {

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, yourKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));

        FileInputStream inFile = new FileInputStream(srcFile.getAbsolutePath());
        FileOutputStream outFile = new FileOutputStream(outPath);
        CipherOutputStream cis = new CipherOutputStream(outFile, cipher);

        doCopy(inFile, cis);
        return true;
    }

    public static byte[] decode(SecretKey yourKey, byte[] fileData)
            throws Exception {
        byte[] decrypted;
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, yourKey, new IvParameterSpec(new byte[cipher.getBlockSize()]));
        decrypted = cipher.doFinal(fileData);
        return decrypted;
    }

    public void saveSecretKey(SecretKey secretKey) {
        String encodedKey = Base64.encodeToString(secretKey.getEncoded(), Base64.NO_WRAP);
        AppPreference.setStr(AppPreference.KEY.SECRET_KEY, encodedKey);
    }

    public SecretKey getSecretKey() {
        String encodedKey = AppPreference.getStr(AppPreference.KEY.SECRET_KEY, "");
        if (null == encodedKey || encodedKey.isEmpty()) {
            SecureRandom secureRandom = new SecureRandom();
            KeyGenerator keyGenerator = null;
            try {
                keyGenerator = KeyGenerator.getInstance(KEY_SPEC_ALGORITHM);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            keyGenerator.init(OUTPUT_KEY_LENGTH, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            saveSecretKey(secretKey);
            return secretKey;
        }

        byte[] decodedKey = Base64.decode(encodedKey, Base64.NO_WRAP);
        SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, KEY_SPEC_ALGORITHM);
        return originalKey;
    }

    public SecretKey getSecretKey(String key) {
        try {
            byte[] bytes = key.getBytes("UTF-8");
            SecretKey originalKey = new SecretKeySpec(bytes, 0, bytes.length, KEY_SPEC_ALGORITHM);
            return originalKey;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    byte[] salt = {
            (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
            (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
    };

    public SecretKey getCustomKey(String password) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, OUTPUT_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");
            return secret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
