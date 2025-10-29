package pt.psoft.g1.psoftg1.idgenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.Base65;
import pt.psoft.g1.psoftg1.shared.model.RandomGenerator;

import java.math.BigInteger;

@Profile("base65")
@Component
public class IdBase65 implements IdGenerator {

    private static final int RANDOM_BITS = 128;

    @Override
    public String generateId() {
        BigInteger randomNumber = RandomGenerator.generateRandomBigInteger(RANDOM_BITS);
        return Base65.encode(randomNumber.toByteArray());
    }
}