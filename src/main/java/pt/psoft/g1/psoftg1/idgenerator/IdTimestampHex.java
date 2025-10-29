package pt.psoft.g1.psoftg1.idgenerator;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.RandomGenerator;

import java.math.BigInteger;

@Profile("timestamphex")
@Component
public class IdTimestampHex implements IdGenerator {

    private static final int HEX_BITS = 24;

    @Override
    public String generateId() {
        long timestamp = System.currentTimeMillis();
        BigInteger randomNumber = RandomGenerator.generateRandomBigInteger(HEX_BITS);
        String hex = String.format("%06x", randomNumber.intValue());
        return timestamp + "-" + hex;
    }
}
