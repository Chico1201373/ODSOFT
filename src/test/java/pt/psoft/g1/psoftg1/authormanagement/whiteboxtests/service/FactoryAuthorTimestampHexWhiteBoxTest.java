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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
}
