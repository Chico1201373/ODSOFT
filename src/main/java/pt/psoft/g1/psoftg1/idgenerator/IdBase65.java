package pt.psoft.g1.psoftg1.idgenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.Base65;

import java.math.BigInteger;

@Profile("base65")
@Component
public class IdBase65 implements IdGenerator {

    @Override
    public String generateId() {
        long timestamp = System.currentTimeMillis();
        byte[] timestampBytes = BigInteger.valueOf(timestamp).toByteArray();
        return Base65.encode(timestampBytes);
    }

}
