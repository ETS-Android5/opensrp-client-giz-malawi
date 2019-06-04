package org.smartregister.giz_malawi.cryptography;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.security.Key;

/**
 * Created by ndegwamartin on 26/04/2019.
 * <p>
 * This class wraps the two version encryption classes to provide one class for interaction
 */
public class CryptographicHelper {


    private static WeakReference<Context> context;

    private static CryptographicHelper cryptographicHelper;
    private static AndroidLegacyCryptography legacyCryptography;
    private static AndroidMCryptography mCryptography;

    private CryptographicHelper(Context context) {

        this.context = new WeakReference<>(context);

        legacyCryptography = new AndroidLegacyCryptography(this.context.get());
        mCryptography = new AndroidMCryptography(this.context.get());


    }

    public static CryptographicHelper getInstance(Context context) {

        if (cryptographicHelper == null) {
            try {
                cryptographicHelper = new CryptographicHelper(context);
            } catch (Exception e) {
                Log.e(CryptographicHelper.class.getCanonicalName(), e.getMessage());
            }
        }

        return cryptographicHelper;
    }

    public static byte[] encrypt(byte[] data, String keyAlias) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            return legacyCryptography.encrypt(data, keyAlias);
        } else {
            return mCryptography.encrypt(data, keyAlias);
        }
    }

    public static byte[] decrypt(byte[] data, String keyAlias) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            return legacyCryptography.decrypt(data, keyAlias);
        } else {
            return mCryptography.decrypt(data, keyAlias);
        }
    }


    public void generateKey(String keyAlias) {

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            legacyCryptography.generateKey(keyAlias);
        } else {
            mCryptography.generateKey(keyAlias);
        }

    }

    public Key getKey(String keyAlias) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {

            return legacyCryptography.getKey(keyAlias);
        } else {
            return mCryptography.getKey(keyAlias);
        }
    }
}
