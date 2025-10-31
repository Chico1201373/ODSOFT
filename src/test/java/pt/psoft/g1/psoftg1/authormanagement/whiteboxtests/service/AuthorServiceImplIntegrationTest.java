package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AuthorServiceImplIntegrationTest {
    @Autowired
    private AuthorService authorService;
    @MockBean
    private AuthorRepository authorRepository;

    @MockBean
    private BookRepository bookRepository;

    @MockBean
    private PhotoRepository photoRepository;

    @MockBean
    private FactoryAuthor factoryAuthor;

    @MockBean
    private AuthorMapper mapper;

    @BeforeEach
    public void setUp() {
    Author alex = new Author("1", "Alex", "O Alex escreveu livros", null);
    alex = authorRepositorySaveStub(alex);

    List<Author> list = new ArrayList<>();
    list.add(alex);

    Mockito.when(authorRepository.searchByNameName(alex.getName()))
        .thenReturn(list);

    Mockito.when(authorRepository.save(Mockito.any(Author.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    Mockito.when(factoryAuthor.generateAuthor(Mockito.any(CreateAuthorRequest.class)))
        .thenAnswer(inv -> {
            CreateAuthorRequest req = inv.getArgument(0);
            return new Author("generated-id", req.getName(), req.getBio(), req.getPhotoURI());
        });
    }

    @Test
    public void whenValidId_thenAuthorShouldBeFound() {
        String id = "1";
        Author existing = new Author(id, "Alex", "O Alex escreveu livros", null);
        Mockito.when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(existing));

        Optional<Author> found = authorService.findByAuthorNumber(id);

        assertTrue(found.isPresent(), "Author should be present");
        assertEquals(id, found.get().getAuthorNumber());
    }

    @Test
    public void create_shouldUseFactoryAndSaveAuthor() {
        CreateAuthorRequest req = new CreateAuthorRequest("New Author", "New bio", null, null);

        Author created = authorService.create(req);

        Mockito.verify(factoryAuthor, Mockito.times(1)).generateAuthor(Mockito.any(CreateAuthorRequest.class));
        Mockito.verify(authorRepository, Mockito.times(1)).save(Mockito.any(Author.class));
        assertNotNull(created);
        assertEquals("generated-id", created.getAuthorNumber());
        assertEquals("New Author", created.getName());
    }

    @Test
    public void partialUpdate_shouldApplyPatchAndSave() {
        String id = "2";
        Author existing = new Author(id, "Old name", "Old bio", null);
        Mockito.when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(existing));
        Mockito.when(authorRepository.save(Mockito.any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateAuthorRequest update = new UpdateAuthorRequest();
        update.setName("Updated Name");

        Author updated = authorService.partialUpdate(id, update, 0L);

        assertNotNull(updated);
        assertEquals("Updated Name", updated.getName());
        Mockito.verify(authorRepository, Mockito.times(1)).save(existing);
    }

    private Author authorRepositorySaveStub(Author a) {
        Mockito.when(authorRepository.findByAuthorNumber(a.getAuthorNumber())).thenReturn(Optional.of(a));
        Mockito.when(authorRepository.save(Mockito.eq(a))).thenReturn(a);
        return a;
    }
}
