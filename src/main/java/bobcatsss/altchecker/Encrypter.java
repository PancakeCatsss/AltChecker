package bobcatsss.altchecker;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;

public class Encrypter {
    private static final String ALGORITHM = "AES";
    private static final byte[] SALT = "3F73B1896A957D6D".getBytes(); // This is the Encryption key (DO NOT CHANGE)

    public static String getEncrypted(String plainText) {
        if (plainText == null) return null;

        Key salt = getKey();
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, salt);
            byte[] encodedValue = cipher.doFinal(plainText.getBytes());
            return Base64.encode(encodedValue)
                    .replace("+", "-")
                    .replace("/", "_")
                    .replace("=", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("Failed to encrypt data");
    }

    public static String getDecrypted(String encodedText) {
        if (encodedText == null) return null;

        Key salt = getKey();
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, salt);
            byte[] decodedValue = Base64.decode(encodedText.replace("-", "+")
                    .replace("_", "/"));
            byte[] decValue = cipher.doFinal(decodedValue);
            return new String(decValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Key getKey() {
        return new SecretKeySpec(SALT, ALGORITHM);
    }
}