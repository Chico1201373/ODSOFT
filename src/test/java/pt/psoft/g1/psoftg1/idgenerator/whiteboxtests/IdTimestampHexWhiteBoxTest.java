package pt.psoft.g1.psoftg1.idgenerator.whiteboxtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import pt.psoft.g1.psoftg1.idgenerator.IdGenerator;
import pt.psoft.g1.psoftg1.idgenerator.IdTimestampHex;
import pt.psoft.g1.psoftg1.shared.model.RandomGenerator;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class IdTimestampHexWhiteBoxTest {

    private IdTimestampHex idGenerator;

    @BeforeEach
    void setUp() {
        idGenerator = new IdTimestampHex();
    }


    @Test
    void generateId_mainPath_shouldReturnTimestampHexFormat() {
        long beforeTimestamp = System.currentTimeMillis();

        String id = idGenerator.generateId();

        long afterTimestamp = System.currentTimeMillis();
        
        assertThat(id).isNotNull();
        assertThat(id).matches("\\d+-[0-9a-f]{6}"); 
        
        String[] parts = id.split("-");
        assertThat(parts).hasSize(2);
        
        long generatedTimestamp = Long.parseLong(parts[0]);
        assertThat(generatedTimestamp).isBetween(beforeTimestamp, afterTimestamp);
        
        String hexPart = parts[1];
        assertThat(hexPart).hasSize(6);
        assertThat(hexPart).matches("[0-9a-f]{6}");
    }

    @Test
    void generateId_withSmallRandomValue_shouldHaveLeadingZeros() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(1));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("000001");
        }
    }

    @Test
    void generateId_withZeroRandomValue_shouldBeAllZeros() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.ZERO);

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("000000");
        }
    }

    @Test
    void generateId_withMaxRandomValue_shouldBeAllFs() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(16777215));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("ffffff");
        }
    }

    @Test
    void generateId_withMidRangeValue_shouldFormatCorrectly() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(0xABC123));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("abc123");
        }
    }

    @Test
    void generateId_shouldRequest24BitsFromRandomGenerator() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(12345));

            String id = idGenerator.generateId();

            mockedRandom.verify(() -> RandomGenerator.generateRandomBigInteger(24));
            assertThat(id).isNotNull();
        }
    }

    @Test
    void generateId_timestampPrecision_shouldBeCloseToCurrentTime() {
        long before = System.currentTimeMillis();

        String id = idGenerator.generateId();
        
        long after = System.currentTimeMillis();

        String[] parts = id.split("-");
        long timestamp = Long.parseLong(parts[0]);
        
        assertThat(timestamp).isGreaterThanOrEqualTo(before);
        assertThat(timestamp).isLessThanOrEqualTo(after);
    }

    @Test
    void generateId_shouldUseDashSeparator() {
        String id = idGenerator.generateId();

        assertThat(id).contains("-");
        assertThat(id.split("-")).hasSize(2);
    }

    @Test
    void generateId_randomBigIntegerConversion_shouldWorkCorrectly() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            BigInteger testValue = new BigInteger("1048575"); 
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(testValue);

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("0fffff"); 
        }
    }

    @Test
    void generateId_sequentialCalls_shouldProduceDifferentIds() throws InterruptedException {
        String id1 = idGenerator.generateId();
        Thread.sleep(5); 
        String id2 = idGenerator.generateId();
        String id3 = idGenerator.generateId();

        assertThat(id1).isNotEqualTo(id2);
        assertThat(id2).isNotEqualTo(id3);
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    void generateId_hexFormatting_shouldBeLowercase() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(0xABCDEF));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("abcdef");
            assertThat(parts[1]).isLowerCase();
        }
    }

    @Test
    void generateId_hexLength_shouldAlwaysBeSixCharacters() {
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            String[] parts = id.split("-");
            
            assertThat(parts[1]).hasSize(6);
        }
    }

    @Test
    void generateId_withAlternatingBitPattern_shouldFormatCorrectly() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(0xAAAAAA));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("aaaaaa");
        }
    }

    @Test
    void generateId_withSparseBitPattern_shouldFormatWithLeadingZeros() {
        try (MockedStatic<RandomGenerator> mockedRandom = mockStatic(RandomGenerator.class)) {
            mockedRandom.when(() -> RandomGenerator.generateRandomBigInteger(24))
                    .thenReturn(BigInteger.valueOf(256));

            String id = idGenerator.generateId();

            String[] parts = id.split("-");
            assertThat(parts[1]).isEqualTo("000100");
        }
    }

    @Test
    void generateId_multipleCallsFormat_shouldBeConsistent() {
        for (int i = 0; i < 50; i++) {
            String id = idGenerator.generateId();
            assertThat(id).matches("\\d+-[0-9a-f]{6}");
        }
    }
      @Test
    @DisplayName("generateId should never return null")
    void generateId_shouldNeverReturnNull() {
        for (int i = 0; i < 1000; i++) {
            String id = idGenerator.generateId();
            assertThat(id).isNotNull();
        }
    }

    @Test
    @DisplayName("generateId should never return empty string")
    void generateId_shouldNeverReturnEmptyString() {
        for (int i = 0; i < 1000; i++) {
            String id = idGenerator.generateId();
            assertThat(id).isNotEmpty();
        }
    }

    @Test
    @DisplayName("generateId should not return blank strings")
    void generateId_shouldNotReturnBlankString() {
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            assertThat(id).isNotBlank();
        }
    }

    @Test
    @DisplayName("generateId should match timestamp-hex format")
    void generateId_shouldMatchExpectedFormat() {
        Pattern expectedPattern = Pattern.compile("^\\d+-[0-9a-f]{6}$");

        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            assertThat(id).matches(expectedPattern);
        }
    }

    @Test
    @DisplayName("Timestamp part should be parseable as long")
    void generateId_timestampPart_shouldBeParseable() {
        String id = idGenerator.generateId();
        String timestampPart = id.split("-")[0];

        assertThat(timestampPart).matches("\\d+");
        long timestamp = Long.parseLong(timestampPart);
        assertThat(timestamp).isPositive();
    }

    @Test
    @DisplayName("Hex part should contain only valid hex characters")
    void generateId_hexPart_shouldBeValidHexadecimal() {
        String id = idGenerator.generateId();
        String hexPart = id.split("-")[1];

        assertThat(hexPart).matches("[0-9a-f]{6}");
    }

    @Test
    @DisplayName("Hex part should always be 6 characters long")
    void generateId_hexPart_shouldBeSixCharacters() {
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            String hexPart = id.split("-")[1];
            assertThat(hexPart).hasSize(6);
        }
    }

    @Test
    @DisplayName("generateId should produce unique IDs")
    void generateId_shouldProduceUniqueIds() {
        Set<String> generatedIds = new HashSet<>();
        int iterations = 10;

        for (int i = 0; i < iterations; i++) {
            String id = idGenerator.generateId();
            generatedIds.add(id);
        }

        assertThat(generatedIds).hasSize(iterations);
    }

    @Test
    @DisplayName("Rapid ID generation should maintain uniqueness")
    void generateId_rapidGeneration_shouldMaintainUniqueness() {
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < 10; i++) {
            ids.add(idGenerator.generateId());
        }

        assertThat(ids).hasSize(10);
    }

    @RepeatedTest(10)
    @DisplayName("Repeated test runs should not produce duplicate IDs")
    void generateId_repeatedTest_shouldNotProduceDuplicates() {
        String id1 = idGenerator.generateId();
        String id2 = idGenerator.generateId();

        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Timestamps should be monotonically increasing over time")
    void generateId_timestamps_shouldIncreaseOverTime() throws InterruptedException {
        String id1 = idGenerator.generateId();
        Thread.sleep(10);
        String id2 = idGenerator.generateId();

        long timestamp1 = Long.parseLong(id1.split("-")[0]);
        long timestamp2 = Long.parseLong(id2.split("-")[0]);
        
        assertThat(timestamp2).isGreaterThanOrEqualTo(timestamp1);
    }

    @Test
    @DisplayName("Timestamp should represent current time")
    void generateId_timestamp_shouldBeCurrentTime() {
        long before = System.currentTimeMillis();

        String id = idGenerator.generateId();

        long after = System.currentTimeMillis();

        long timestamp = Long.parseLong(id.split("-")[0]);
        assertThat(timestamp).isBetween(before, after);
    }

    @Test
    @DisplayName("Hex values should show statistical variation")
    void generateId_hexValues_shouldShowVariation() {
        Set<String> hexValues = new HashSet<>();

        for (int i = 0; i < 1000; i++) {
            String id = idGenerator.generateId();
            String hexPart = id.split("-")[1];
            hexValues.add(hexPart);
        }

        assertThat(hexValues.size()).isGreaterThan(950);
    }

    @Test
    @DisplayName("All hex digits should appear in generated IDs")
    void generateId_hexValues_shouldContainAllDigits() {
        Set<Character> observedDigits = new HashSet<>();

        for (int i = 0; i < 10000; i++) {
            String id = idGenerator.generateId();
            String hexPart = id.split("-")[1];
            for (char c : hexPart.toCharArray()) {
                observedDigits.add(c);
            }
        }

        assertThat(observedDigits).hasSize(16);
        assertThat(observedDigits).containsAll(Set.of('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'));
    }

    @Test
    @DisplayName("Consecutive IDs should have different hex parts")
    void generateId_consecutiveCalls_shouldHaveDifferentHexParts() {
        String id1 = idGenerator.generateId();
        String id2 = idGenerator.generateId();
        String id3 = idGenerator.generateId();

        String hex1 = id1.split("-")[1];
        String hex2 = id2.split("-")[1];
        String hex3 = id3.split("-")[1];

        assertThat(hex1).isNotEqualTo(hex2);
        assertThat(hex2).isNotEqualTo(hex3);
        assertThat(hex1).isNotEqualTo(hex3);
    }

    @Test
    @DisplayName("ID length should be reasonable")
    void generateId_length_shouldBeReasonable() {
        String id = idGenerator.generateId();

        assertThat(id.length()).isGreaterThanOrEqualTo(20);
    }

    @Test
    @DisplayName("ID should contain exactly one dash separator")
    void generateId_shouldContainOneDashSeparator() {
        String id = idGenerator.generateId();

        long dashCount = id.chars().filter(c -> c == '-').count();
        assertThat(dashCount).isEqualTo(1);
    }

    @Test
    @DisplayName("IdTimestampHex instance should be created successfully")
    void instance_shouldBeCreatedSuccessfully() {
        assertThat(idGenerator).isNotNull();
        assertThat(idGenerator).isInstanceOf(IdTimestampHex.class);
        assertThat(idGenerator).isInstanceOf(IdGenerator.class);
    }

    @Test
    @DisplayName("IdGenerator instance should generate valid IDs")
    void instance_shouldGenerateValidIds() {
        String id = idGenerator.generateId();

        assertThat(id).isNotNull();
        assertThat(id).matches("\\d+-[0-9a-f]{6}");
    }

    @Test
    @DisplayName("ID generation should be performant")
    void generateId_performance_shouldBeReasonablyFast() {
        int iterations = 10000;
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            idGenerator.generateId();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertThat(duration).isLessThan(1000);
    }

    @Test
    @DisplayName("Concurrent ID generation should be safe")
    void generateId_concurrent_shouldBeSafe() throws InterruptedException {
        Set<String> ids = new HashSet<>();
        int threadCount = 10;
        int idsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < idsPerThread; j++) {
                    synchronized (ids) {
                        ids.add(idGenerator.generateId());
                    }
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(ids).hasSize(threadCount * idsPerThread);
    }

    @Test
    @DisplayName("IDs should be valid across multiple calls")
    void generateId_multipleCalls_shouldAlwaysBeValid() {
        for (int i = 0; i < 100; i++) {
            String id = idGenerator.generateId();
            assertThat(id).matches("\\d+-[0-9a-f]{6}");
            assertThat(id).isNotNull().isNotEmpty();
        }
    }

    @Test
    @DisplayName("IDs should be safe for use as database keys")
    void generateId_shouldBeSafeForDatabaseKeys() {
        String id = idGenerator.generateId();

        assertThat(id).matches("^[0-9a-f-]+$");
    }

    @Test
    @DisplayName("IDs should be URL-safe")
    void generateId_shouldBeUrlSafe() {
        String id = idGenerator.generateId();

        assertThat(id).doesNotContain(" ", "?", "&", "=", "/", "\\", "%");
    }
}
