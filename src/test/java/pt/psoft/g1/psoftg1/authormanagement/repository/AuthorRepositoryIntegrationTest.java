package pt.psoft.g1.psoftg1.authormanagement.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.idgenerator.IdBase65;
import pt.psoft.g1.psoftg1.idgenerator.IdGenerator;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Based on https:
 * <p>Adaptations to Junit 5 with ChatGPT
 */
@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import({FactoryAuthor.class, IdBase65.class})
public class AuthorRepositoryIntegrationTest {
    /*
    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private AuthorRepository authorRepository;
    @Autowired
    private FactoryAuthor factoryAuthor;


    @Test
    public void whenFindByName_thenReturnAuthor() {
        CreateAuthorRequest createAuthorRequest = new CreateAuthorRequest();
        createAuthorRequest.setBio("Some bio");
        createAuthorRequest.setName("Some name");
        Author alex= factoryAuthor.generateAuthor(createAuthorRequest);
        
        entityManager.persist(alex);
        entityManager.flush();

        
        List<Author> list = authorRepository.searchByNameName(alex.getName());

        
        assertThat(list).isNotEmpty();
        assertThat(list.get(0).getName())
                .isEqualTo(alex.getName());
    }*/
}
