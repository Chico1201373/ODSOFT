package pt.psoft.g1.psoftg1.shared.model;

import java.math.BigInteger;
import java.util.Arrays;

public class Base65 {

    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+-?".toCharArray();
    private static final BigInteger BASE = BigInteger.valueOf(ALPHABET.length);

    public static String encode(byte[] data) {
        BigInteger num = new BigInteger(1, data); // treat bytes as unsigned integer
        StringBuilder sb = new StringBuilder();

        while (num.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divmod = num.divideAndRemainder(BASE);
            sb.append(ALPHABET[divmod[1].intValue()]);
            num = divmod[0];
        }

        return sb.length() == 0 ? "A" : sb.reverse().toString(); // "A" represents 0
    }

    public static byte[] decode(String base65) {
        BigInteger num = BigInteger.ZERO;
        for (int i = 0; i < base65.length(); i++) {
            int index = -1;
            for (int j = 0; j < ALPHABET.length; j++) {
                if (ALPHABET[j] == base65.charAt(i)) {
                    index = j;
                    break;
                }
            }
            if (index == -1) throw new IllegalArgumentException("Invalid Base65 character: " + base65.charAt(i));
            num = num.multiply(BASE).add(BigInteger.valueOf(index));
        }

        byte[] bytes = num.toByteArray();
        if (bytes[0] == 0) {
            bytes = Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return bytes;
    }


}
