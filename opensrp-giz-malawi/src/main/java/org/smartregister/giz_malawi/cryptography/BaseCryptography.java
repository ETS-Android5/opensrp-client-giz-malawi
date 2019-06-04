package org.smartregister.giz_malawi.cryptography;

import android.content.Context;
import android.util.Log;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;

/**
 * Created by ndegwamartin on 26/04/2019.
 */
public abstract class BaseCryptography {

    public static final String AndroidKeyStore = "AndroidKeyStore";

    public static final String RSA_MODE = "RSA/ECB/PKCS1Padding";

    public static KeyStore keyStore;

    public static Context context;

    public SecureRandom secureRandom;

    public static class PROVIDER {
        public static final String ANDROID_OPEN_SSL = "AndroidOpenSSL";
        public static final String BOUNCY_CASTLE = "BC";
    }

    public static class ALGORITHIM {
        public static final String AES = "AES";
        public static final String RSA = "RSA";
    }

    public BaseCryptography(Context context) {

        try {
            this.context = context;

            keyStore = KeyStore.getInstance(AndroidKeyStore);
            keyStore.load(null);

            secureRandom = new SecureRandom();

        } catch (Exception e) {
            Log.e(BaseCryptography.class.getCanonicalName(), e.getMessage());
        }
    }

    public void deleteKey(final String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            Log.e(BaseCryptography.class.getCanonicalName(), Log.getStackTraceString(e));
        }
    }

    public abstract String getAESMode();
}
