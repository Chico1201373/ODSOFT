package pt.psoft.g1.psoftg1.idgenerator.whiteboxtests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import pt.psoft.g1.psoftg1.idgenerator.IdTimestampHex;
import pt.psoft.g1.psoftg1.shared.model.RandomGenerator;

import java.math.BigInteger;

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
    
}
