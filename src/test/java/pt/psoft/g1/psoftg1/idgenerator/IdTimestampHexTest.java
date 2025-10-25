package pt.psoft.g1.psoftg1.idgenerator;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class IdTimestampHexTest {

    @Test
    void formatContainsTimestampAndHex() {
        IdTimestampHex gen = new IdTimestampHex();
        String id = gen.generateId();

        assertTrue(id.contains("-"));
        String[] parts = id.split("-", 2);
        assertEquals(2, parts.length);

        long ts = Long.parseLong(parts[0]);
        assertTrue(ts > 0);

        assertEquals(6, parts[1].length());
        // hex chars only
        assertTrue(parts[1].matches("[0-9a-f]{6}"));
    }

    @RepeatedTest(10)
    void multipleCallsProduceUniqueSuffixMostTimes() {
        IdTimestampHex gen = new IdTimestampHex();
        Set<String> s = new HashSet<>();
        for (int i = 0; i < 1000000; i++) {
            s.add(gen.generateId());
        }
        // collisions are possible across timestamps, but expect high uniqueness
        assertTrue(s.size() > 975000, "expected most ids to be unique");
    }

    @Test
    void concurrentGenerationProducesManyUniqueIds() throws InterruptedException, ExecutionException {
        IdTimestampHex gen = new IdTimestampHex();

        int threads = 8;
        int perThread = 500;

        ExecutorService ex = Executors.newFixedThreadPool(threads);
        Set<String> all = ConcurrentHashMap.newKeySet();

        Callable<Void> task = () -> {
            for (int i = 0; i < perThread; i++) {
                all.add(gen.generateId());
            }
            return null;
        };

        Set<Future<Void>> futures = new HashSet<>();
        for (int i = 0; i < threads; i++) futures.add(ex.submit(task));

        for (Future<Void> f : futures) f.get();

        ex.shutdown();
        assertEquals(threads * perThread, all.size(), "expected no collisions under concurrent generation");
    }
}
