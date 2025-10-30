package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.idgenerator.IdTimestampHex;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Black box tests for FactoryAuthor when profile "timestamphex" is active.
 */
@SpringJUnitConfig(classes = {FactoryAuthor.class, IdTimestampHex.class})
@ActiveProfiles("timestamphex")
class FactoryAuthorTimestampHexBlackBoxTest {

    @Autowired
    private FactoryAuthor factoryAuthor;

    @Test
    void generateAuthor_WithValidRequest_ProducesTimestampHexId_AndMapsFields() {
        
        CreateAuthorRequest req = new CreateAuthorRequest();
        req.setName("Haruki Murakami");
        req.setBio("Japanese writer");

        long before = System.currentTimeMillis();

        
        Author a = factoryAuthor.generateAuthor(req);

        long after = System.currentTimeMillis();

        
        assertNotNull(a);
        String id = a.getAuthorNumber();
        assertNotNull(id);
        assertTrue(id.matches("^\\d{10,15}-[0-9a-f]{6}$"), "Id must be '<millis>-<6 hex>' lower-case: " + id);

        
        String millisPart = id.substring(0, id.indexOf('-'));
        long ts = Long.parseLong(millisPart);
        assertTrue(ts >= before && ts <= after, "Timestamp part must be within test window");

        assertEquals("Haruki Murakami", a.getName());
        assertEquals("Japanese writer", a.getBio());
    }

    @Test
    void generateAuthor_Twice_YieldsDifferentIds() {
        
        CreateAuthorRequest r1 = new CreateAuthorRequest();
        r1.setName("A1"); r1.setBio("B1");
        CreateAuthorRequest r2 = new CreateAuthorRequest();
        r2.setName("A2"); r2.setBio("B2");

        
        Author a1 = factoryAuthor.generateAuthor(r1);
        Author a2 = factoryAuthor.generateAuthor(r2);

        
        assertNotEquals(a1.getAuthorNumber(), a2.getAuthorNumber());
        assertTrue(a1.getAuthorNumber().matches("^\\d{10,15}-[0-9a-f]{6}$"));
        assertTrue(a2.getAuthorNumber().matches("^\\d{10,15}-[0-9a-f]{6}$"));
    }

    @Test
    void generateAuthor_ManyIds_AllUnique() {
        
        final int N = 500;
        final Set<String> ids = new HashSet<>(N * 2);

        for (int i = 0; i < N; i++) {
            CreateAuthorRequest r = new CreateAuthorRequest();
            r.setName("Author " + i);
            r.setBio("Bio " + i);

            Author a = factoryAuthor.generateAuthor(r);
            String id = a.getAuthorNumber();

            assertNotNull(id, "Generated id must not be null");
            assertTrue(id.matches("^\\d{10,15}-[0-9a-f]{6}$"), "Invalid id format: " + id);

            boolean added = ids.add(id);
            if (!added) {
                fail("Duplicate id encountered: " + id + " at iteration " + i);
            }
        }

        assertEquals(N, ids.size(), "All generated IDs must be unique");
    }
}
