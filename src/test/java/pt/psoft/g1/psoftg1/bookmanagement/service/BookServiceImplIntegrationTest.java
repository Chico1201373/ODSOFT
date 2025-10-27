package pt.psoft.g1.psoftg1.bookmanagement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.*;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDTO;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {"suggestionsLimitPerGenre=3"})
class BookServiceImplIntegrationTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private PhotoRepository photoRepository;

    private Genre fictionGenre;
    private Genre scienceFictionGenre;
    private Author author1;
    private Author author2;

    @BeforeEach
    void setUp() {
        // Setup test data
        fictionGenre = genreRepository.save(new Genre("Fiction"));
        scienceFictionGenre = genreRepository.save(new Genre("Science Fiction"));

        author1 = new Author("A001", "John Doe", "Acclaimed author", null);
        author2 = new Author("A002", "Jane Smith", "Bestselling writer", null);
        
        authorRepository.save(author1);
        authorRepository.save(author2);
    }

    private CreateBookRequest buildCreateBookRequest(String title, String description, 
                                                      String genre, List<String> authors, 
                                                      String photoURI) {
        CreateBookRequest request = new CreateBookRequest();
        // Using reflection or direct field access since setters might not work with @NotBlank
        request.setTitle(title);
        request.setDescription(description);
        request.setGenre(genre);
        request.setAuthors(authors);
        if (photoURI != null) {
            request.setPhotoURI(photoURI);
        }
        return request;
    }

    @Test
    void testCreateBook_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "A test book description",
            "Fiction",
            Arrays.asList("A001", "A002"),
            "http://example.com/photo.jpg"
        );

        // Act
        Book createdBook = bookService.create(request, isbn);

        // Assert
        assertThat(createdBook).isNotNull();
        assertThat(createdBook.getIsbn()).isEqualTo(isbn);
        assertThat(createdBook.getTitle().getTitle()).isEqualTo("Test Book");
        assertThat(createdBook.getDescription()).isEqualTo("A test book description");
        assertThat(createdBook.getAuthors()).hasSize(2);
        assertThat(createdBook.getGenre().toString()).isEqualTo("Fiction");

        // Verify persistence
        Book savedBook = bookRepository.findByIsbn(isbn).orElse(null);
        assertThat(savedBook).isNotNull();
        assertThat(savedBook.getTitle().getTitle()).isEqualTo("Test Book");
    }

    @Test
    void testCreateBook_DuplicateIsbn_ThrowsConflictException() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request1 = buildCreateBookRequest(
            "First Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        bookService.create(request1, isbn);

        CreateBookRequest request2 = buildCreateBookRequest(
            "Second Book",
            "Another description",
            "Fiction",
            Arrays.asList("A002"),
            null
        );

        // Act & Assert
        assertThatThrownBy(() -> bookService.create(request2, isbn))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Book with ISBN " + isbn + " already exists");
    }

    @Test
    void testCreateBook_GenreNotFound_ThrowsNotFoundException() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "NonExistentGenre",
            Arrays.asList("A001"),
            null
        );

        // Act & Assert
        assertThatThrownBy(() -> bookService.create(request, isbn))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Genre not found");
    }

    @Test
    void testCreateBook_WithNonExistentAuthors_SkipsThem() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001", "A999"), // A999 doesn't exist
            null
        );

        // Act
        Book createdBook = bookService.create(request, isbn);

        // Assert
        assertThat(createdBook.getAuthors()).hasSize(1);
        assertThat(createdBook.getAuthors().get(0).getAuthorNumber()).isEqualTo("A001");
    }

    @Test
    void testUpdateBook_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest createRequest = buildCreateBookRequest(
            "Original Title",
            "Original Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        Book originalBook = bookService.create(createRequest, isbn);

        UpdateBookRequest updateRequest = new UpdateBookRequest();
        updateRequest.setIsbn(isbn);
        updateRequest.setTitle("Updated Title");
        updateRequest.setDescription("Updated Description");

        // Act
        Book updatedBook = bookService.update(updateRequest, String.valueOf(originalBook.getVersion()));

        // Assert
        assertThat(updatedBook.getTitle().getTitle()).isEqualTo("Updated Title");
        assertThat(updatedBook.getDescription()).isEqualTo("Updated Description");
    }

    @Test
    void testUpdateBook_ChangeGenre_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest createRequest = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        Book originalBook = bookService.create(createRequest, isbn);

        UpdateBookRequest updateRequest = new UpdateBookRequest();
        updateRequest.setIsbn(isbn);
        updateRequest.setGenre("Science Fiction");

        // Act
        Book updatedBook = bookService.update(updateRequest, String.valueOf(originalBook.getVersion()));

        // Assert
        assertThat(updatedBook.getGenre().toString()).isEqualTo("Science Fiction");
    }

    @Test
    void testUpdateBook_ChangeAuthors_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest createRequest = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        Book originalBook = bookService.create(createRequest, isbn);

        UpdateBookRequest updateRequest = new UpdateBookRequest();
        updateRequest.setIsbn(isbn);
        updateRequest.setAuthors(Arrays.asList("A002"));

        // Act
        Book updatedBook = bookService.update(updateRequest, String.valueOf(originalBook.getVersion()));

        // Assert
        assertThat(updatedBook.getAuthors()).hasSize(1);
        assertThat(updatedBook.getAuthors().get(0).getAuthorNumber()).isEqualTo("A002");
    }

    @Test
    void testFindByIsbn_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        bookService.create(request, isbn);

        // Act
        Book foundBook = bookService.findByIsbn(isbn);

        // Assert
        assertThat(foundBook).isNotNull();
        assertThat(foundBook.getIsbn()).isEqualTo(isbn);
        assertThat(foundBook.getTitle().getTitle()).isEqualTo("Test Book");
    }

    @Test
    void testFindByIsbn_NotFound_ThrowsNotFoundException() {
        // Act & Assert
        assertThatThrownBy(() -> bookService.findByIsbn("999-9-99-999999-9"))
                .isInstanceOf(NotFoundException.class);
    }
/* 
   @Test
    void testFindByGenre_Success() {
        // Arrange
        CreateBookRequest request1 = buildCreateBookRequest(
            "Fiction Book 1",
            "Description 1",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        CreateBookRequest request2 = buildCreateBookRequest(
            "Fiction Book 2",
            "Description 2",
            "Fiction",
            Arrays.asList("A002"),
            null
        );
        CreateBookRequest request3 = buildCreateBookRequest(
            "SciFi Book",
            "Description 3",
            "Science Fiction",
            Arrays.asList("A001"),
            null
        );

        bookService.create(request1, "9789725680001");
        bookService.create(request2, "9780306406157");
        bookService.create(request3, "9781861972712");

        // Act
        List<Book> fictionBooks = bookService.findByGenre("Fiction");

        // Assert
        assertThat(fictionBooks).hasSize(2);
        assertThat(fictionBooks).extracting(book -> book.getTitle().getTitle().toString())
                .containsExactlyInAnyOrder("Fiction Book 1", "Fiction Book 2");
    }
*/
    @Test
    void testFindByTitle_Success() {
        // Arrange
        CreateBookRequest request = buildCreateBookRequest(
            "Unique Title",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        bookService.create(request, "9789725680001");

        // Act
        List<Book> books = bookService.findByTitle("Unique Title");

        // Assert
        assertThat(books).hasSize(1);
        assertThat(books.get(0).getTitle().getTitle()).isEqualTo("Unique Title");
    }

    @Test
    void testFindByAuthorName_Success() {
        // Arrange
        CreateBookRequest request1 = buildCreateBookRequest(
            "Book 1",
            "Description 1",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        CreateBookRequest request2 = buildCreateBookRequest(
            "Book 2",
            "Description 2",
            "Fiction",
            Arrays.asList("A002"),
            null
        );

        bookService.create(request1, "9789725680001");
        bookService.create(request2, "9780306406157");

        // Act
        List<Book> booksByJohn = bookService.findByAuthorName("John");

        // Assert
        assertThat(booksByJohn).hasSize(1);
        assertThat(booksByJohn.get(0).getTitle().getTitle()).isEqualTo("Book 1");
    }

    @Test
    void testSearchBooks_WithQuery() {
        // Arrange
        CreateBookRequest request1 = buildCreateBookRequest(
            "The Great Book",
            "Description 1",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        CreateBookRequest request2 = buildCreateBookRequest(
            "Another Book",
            "Description 2",
            "Science Fiction",
            Arrays.asList("A002"),
            null
        );

        bookService.create(request1, "9789725680001");
        bookService.create(request2, "9780306406157");

        Page page = new Page(1, 10);
        SearchBooksQuery query = new SearchBooksQuery("Great", "", "");

        // Act
        List<Book> results = bookService.searchBooks(page, query);

        // Assert
        assertThat(results).isNotEmpty();
    }

    @Test
    void testSearchBooks_WithNullParameters() {
        // Arrange
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        bookService.create(request, "9789725680001");

        // Act
        List<Book> results = bookService.searchBooks(null, null);

        // Assert
        assertThat(results).isNotEmpty();
    }
    /* */
    @Test
    void testRemoveBookPhoto_Success() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            "http://example.com/photo.jpg"
        );
        Book book = bookService.create(request, isbn);

        // Act
        Book updatedBook = bookService.removeBookPhoto(isbn, book.getVersion());

        // Assert
        assertThat(updatedBook.getPhoto()).isNull();
    }

    @Test
    void testRemoveBookPhoto_NoPhoto_ThrowsNotFoundException() {
        // Arrange
        String isbn = "9789725680001";
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        Book book = bookService.create(request, isbn);

        // Act & Assert
        assertThatThrownBy(() -> bookService.removeBookPhoto(isbn, book.getVersion()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Book did not have a photo assigned to it");
    }
    /* 
    @Test
    void testGetBooksSuggestionsForReader_Success() {
        // Arrange
        ReaderDetails reader = new ReaderDetails();
        reader.setReaderNumber("R001");
        List<Genre> interests = new ArrayList<>();
        interests.add(fictionGenre);
        interests.add(scienceFictionGenre);
        reader.setInterestList(interests);
        readerRepository.save(reader);

        // Create books in interested genres
        for (int i = 1; i <= 5; i++) {
            CreateBookRequest request = buildCreateBookRequest(
                "Fiction Book " + i,
                "Description " + i,
                "Fiction",
                Arrays.asList("A001"),
                null
            );
            bookService.create(request, "978-1-11-11111" + i + "-1");
        }

        for (int i = 1; i <= 3; i++) {
            CreateBookRequest request = buildCreateBookRequest(
                "SciFi Book " + i,
                "Description " + i,
                "Science Fiction",
                Arrays.asList("A002"),
                null
            );
            bookService.create(request, "978-2-22-22222" + i + "-2");
        }

        // Act
        List<Book> suggestions = bookService.getBooksSuggestionsForReader("R001");

        // Assert
        assertThat(suggestions).isNotEmpty();
        // Should have 3 fiction books (limit) + 3 sci-fi books
        assertThat(suggestions).hasSize(6);
    }
        */
    /* 
    @Test
    void testGetBooksSuggestionsForReader_NoInterests_ThrowsNotFoundException() {
        // Arrange
        Reader readerUser = new Reader("reader2", "password123", "Jane Reader");
        ReaderDetails reader = new ReaderDetails(
            2,
            readerUser,
            "1995/06/15",
            "923456789",
            true,
            false,
            false,
            null,
            new ArrayList<>() // Empty interest list
        );
        readerRepository.save(reader);
        // Act & Assert
        assertThatThrownBy(() -> bookService.getBooksSuggestionsForReader("R001"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reader has no interests");
    }
    */
    @Test
    void testGetBooksSuggestionsForReader_ReaderNotFound_ThrowsNotFoundException() {
        // Act & Assert
        assertThatThrownBy(() -> bookService.getBooksSuggestionsForReader("R999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reader not found");
    }

    @Test
    void testSaveBook_Success() {
        // Arrange
        CreateBookRequest request = buildCreateBookRequest(
            "Test Book",
            "Description",
            "Fiction",
            Arrays.asList("A001"),
            null
        );
        Book book = bookService.create(request, "9789725680001");
        
        // Modify the book
        UpdateBookRequest updateRequest = new UpdateBookRequest();
        updateRequest.setTitle("Modified Title");
        book.applyPatch(book.getVersion(), updateRequest);

        // Act
        Book savedBook = bookService.save(book);

        // Assert
        assertThat(savedBook.getTitle().getTitle()).isEqualTo("Modified Title");
        Book retrievedBook = bookRepository.findByIsbn("9789725680001").orElse(null);
        assertThat(retrievedBook).isNotNull();
        assertThat(retrievedBook.getTitle().getTitle()).isEqualTo("Modified Title");
    }

    @Test
    void testFindTop5BooksLent() {
        // Note: This test requires Lending entity setup and data
        // The actual implementation depends on your Lending model
        
        // Act
        List<BookCountDTO> topBooks = bookService.findTop5BooksLent();

        // Assert
        assertThat(topBooks).isNotNull();
        assertThat(topBooks).hasSizeLessThanOrEqualTo(5);
    }
}