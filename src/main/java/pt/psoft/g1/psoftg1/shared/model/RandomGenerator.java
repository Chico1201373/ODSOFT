package pt.psoft.g1.psoftg1.shared.model;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;

public class RandomGenerator {

    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a cryptographically secure random BigInteger of the given bit length.
     *
     * @param bits number of bits for the random number
     * @return a positive BigInteger
     */
    public static BigInteger generateRandomBigInteger(int bits) {
        return new BigInteger(bits, secureRandom);
    }


    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }


    public static String generateRandomBase64(int length) {
        byte[] bytes = generateRandomBytes(length);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }


}
