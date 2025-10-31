package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class AuthorServiceImplBlackBoxTest {

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
    void setup() {
    }

    @Test
    void testCreate_shouldCallFactoryAndSave() {
        CreateAuthorRequest req = new CreateAuthorRequest();
        req.setName("New Author");
        req.setBio("Bio");

        Author generated = new Author("NEW1","New Author","Bio", null);
        when(factoryAuthor.generateAuthor(any(CreateAuthorRequest.class))).thenReturn(generated);
        when(authorRepository.save(generated)).thenReturn(generated);

        Author result = authorService.create(req);

        assertNotNull(result);
        assertEquals("NEW1", result.getAuthorNumber());
        verify(factoryAuthor, times(1)).generateAuthor(req);
        verify(authorRepository, times(1)).save(generated);
    }

    @Test
    void testFindByAuthorNumber_returnsOptional() {
        Author a = new Author("A10","Name","Bio", null);
        when(authorRepository.findByAuthorNumber("A10")).thenReturn(Optional.of(a));

        Optional<Author> res = authorService.findByAuthorNumber("A10");
        assertTrue(res.isPresent());
        assertEquals("A10", res.get().getAuthorNumber());
    }

    @Test
    void testFindByName_delegatesToRepository() {
        Author a1 = new Author("A11","Alpha","Bio", null);
        when(authorRepository.searchByNameNameStartsWith("Alp")).thenReturn(List.of(a1));

        List<Author> res = authorService.findByName("Alp");
        assertEquals(1, res.size());
        assertEquals("A11", res.get(0).getAuthorNumber());
    }

    @Test
    void testPartialUpdate_appliesPatchAndSaves() {
        Author existing = new Author("A20","Old","OldBio", null);
        when(authorRepository.findByAuthorNumber("A20")).thenReturn(Optional.of(existing));
        when(authorRepository.save(existing)).thenReturn(existing);

    pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest req = new pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest();
    req.setName("NewName");

    Author updated = authorService.partialUpdate("A20", req, existing.getVersion());

        assertNotNull(updated);
        assertEquals("NewName", updated.getName());
        verify(authorRepository, times(1)).save(existing);
    }

    @Test
    void testFindTopAuthorByLendings_returnsList() {
        AuthorLendingView v1 = new AuthorLendingView();
        // AuthorLendingView has fields; we only need to return it in the page
        PageImpl<AuthorLendingView> page = new PageImpl<>(List.of(v1));
        when(authorRepository.findTopAuthorByLendings(any(Pageable.class))).thenReturn(page);

        List<AuthorLendingView> result = authorService.findTopAuthorByLendings();
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testFindBooksByAuthorNumber_delegatesToBookRepo() {
        when(bookRepository.findBooksByAuthorNumber("A30")).thenReturn(List.of());

        List<Book> books = authorService.findBooksByAuthorNumber("A30");
        assertNotNull(books);
        assertEquals(0, books.size());
    }

    @Test
    void testRemoveAuthorPhoto_deletesPhotoAndSaves() {
        Author withPhoto = new Author("A40","Name","Bio","/tmp/pic.jpg");
        when(authorRepository.findByAuthorNumber("A40")).thenReturn(Optional.of(withPhoto));
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<Author> res = authorService.removeAuthorPhoto("A40", withPhoto.getVersion());

        assertTrue(res.isPresent());
        verify(photoRepository, times(1)).deleteByPhotoFile("/tmp/pic.jpg");
    }

}
