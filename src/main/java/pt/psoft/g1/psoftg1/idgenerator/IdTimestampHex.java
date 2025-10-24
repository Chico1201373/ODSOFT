package pt.psoft.g1.psoftg1.idgenerator;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Profile("timestamphex")
@Component
public class IdTimestampHex implements IdGenerator {

    // 6 hex digits = 24 bits
    private static final int HEX_DIGITS = 6;
    private static final int BITS = HEX_DIGITS * 4; // 24
    private static final int BOUND = 1 << BITS; // 2^24

    // Per-thread SecureRandom to avoid contention in high-throughput scenarios
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    @Override
    public String generateId() {
        long timestamp = System.currentTimeMillis();
        int random24 = SECURE_RANDOM.get().nextInt(BOUND);
        // zero-pad to 6 hex digits
        String hex = String.format("%06x", random24);
        return timestamp + "-" + hex;
    }
}
