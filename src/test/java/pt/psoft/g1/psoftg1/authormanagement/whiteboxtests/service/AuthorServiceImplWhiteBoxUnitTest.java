package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.service;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplWhiteBoxUnitTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorMapper mapper;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private FactoryAuthor factoryAuthor;

    @InjectMocks
    private AuthorServiceImpl authorService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void create_usesFactoryAndSaves() {
        CreateAuthorRequest req = new CreateAuthorRequest("Alice", "bio", null, null);
        Author generated = new Author("gen-1", "Alice", "bio", null);
        when(factoryAuthor.generateAuthor(req)).thenReturn(generated);
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

        Author result = authorService.create(req);

        verify(factoryAuthor, times(1)).generateAuthor(req);
        verify(authorRepository, times(1)).save(generated);
        assertEquals("gen-1", result.getAuthorNumber());
    }

    @Test
    void findByAuthorNumber_delegatesToRepository() {
        Author a = new Author("1", "Bob", "b", null);
        when(authorRepository.findByAuthorNumber("1")).thenReturn(Optional.of(a));

        Optional<Author> res = authorService.findByAuthorNumber("1");

        assertTrue(res.isPresent());
        assertEquals("1", res.get().getAuthorNumber());
    }

    @Test
    void findByName_delegatesToRepository() {
        Author a = new Author("2", "Carol", "c", null);
        when(authorRepository.searchByNameNameStartsWith("Ca")).thenReturn(List.of(a));

        List<Author> res = authorService.findByName("Ca");

        assertEquals(1, res.size());
        assertEquals("Carol", res.get(0).getName());
    }
    @Test
    void partialUpdate_throwsWhenNotFound() {
        when(authorRepository.findByAuthorNumber("missing")).thenReturn(Optional.empty());

        UpdateAuthorRequest update = new UpdateAuthorRequest();
        update.setName("X");

        assertThrows(RuntimeException.class, () -> authorService.partialUpdate("missing", update, 0L));
    }

    @Test
    void partialUpdate_throwsOnVersionMismatch() {
        String id = "4";
        Author existing = new Author(id, "Old", "oldbio", null);
        when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(existing));

        UpdateAuthorRequest update = new UpdateAuthorRequest();
        update.setName("New");

        assertThrows(StaleObjectStateException.class, () -> authorService.partialUpdate(id, update, 999L));
    }

    @Test
    void removeAuthorPhoto_deletesPhotoFileAndSaves() {
        String id = "5";
        String photoUri = "/tmp/pic.jpg";
        Author existing = new Author(id, "Name", "bio", photoUri);
        when(authorRepository.findByAuthorNumber(id)).thenReturn(Optional.of(existing));
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

        Optional<Author> out = authorService.removeAuthorPhoto(id, 0L);

        assertTrue(out.isPresent());
        verify(photoRepository, times(1)).deleteByPhotoFile(photoUri);
        assertNull(out.get().getPhoto());
    }

    @Test
    void findBooksByAuthorNumber_delegatesToRepository() {
        String id = "7";
        var book = mock(pt.psoft.g1.psoftg1.bookmanagement.model.Book.class);
        when(bookRepository.findBooksByAuthorNumber(id)).thenReturn(List.of(book));

        var res = authorService.findBooksByAuthorNumber(id);

        assertEquals(1, res.size());
        verify(bookRepository, times(1)).findBooksByAuthorNumber(id);
    }
}
