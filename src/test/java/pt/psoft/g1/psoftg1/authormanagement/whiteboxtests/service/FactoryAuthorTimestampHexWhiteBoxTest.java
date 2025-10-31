package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.service;

import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.idgenerator.IdGenerator;
import pt.psoft.g1.psoftg1.idgenerator.IdTimestampHex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.Set;

@SpringBootTest(classes = {FactoryAuthor.class, IdTimestampHex.class})
@ActiveProfiles("timestamphex")
class FactoryAuthorTimestampHexWhiteBoxTest {

    @Autowired
    private FactoryAuthor factoryAuthor;

    @Autowired
    private IdGenerator idGenerator;

    @SpyBean
    private IdTimestampHex idTimestampHex;

    @Test
    void shouldWireIdTimestampHexAsIdGenerator() {
        assertThat(idGenerator).isInstanceOf(IdTimestampHex.class);
    }

    @Test
    void generateAuthor_delegatesToIdTimestampHex_andMapsFields() {
        BDDMockito.given(idTimestampHex.generateId()).willReturn("1740878400000-00ab1c");

        CreateAuthorRequest req = new CreateAuthorRequest();
        req.setName("Alice");
        req.setBio("Bio");
        req.setPhotoURI("uploads/pic.png");

        Author created = factoryAuthor.generateAuthor(req);

        verify(idTimestampHex, times(1)).generateId();
        assertThat(created.getAuthorNumber()).isEqualTo("1740878400000-00ab1c");
        assertThat(created.getName()).isEqualTo("Alice");
        assertThat(created.getBio()).isEqualTo("Bio");
        assertThat(created.getPhoto()).isNotNull();
    }
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
