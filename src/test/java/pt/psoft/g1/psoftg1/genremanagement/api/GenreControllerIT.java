package pt.psoft.g1.psoftg1.genremanagement.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureMockMvc
public class GenreControllerIT {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenreRepository genreRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;


    @Autowired
    private LendingService lendingService;

    @Autowired
    private ReaderRepository readerRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createQuery("DELETE FROM Lending").executeUpdate();
        em.createQuery("DELETE FROM Book").executeUpdate();
        em.createQuery("DELETE FROM Genre").executeUpdate();
        em.createQuery("DELETE FROM Author").executeUpdate();
        em.createQuery("DELETE FROM ReaderDetails").executeUpdate();
        em.createQuery("DELETE FROM Reader").executeUpdate();
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnNotFound_whenNoTop5GenresExist() throws Exception {
        mockMvc.perform(get("/api/genres/top5"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnTop5Genres_whenDataExists() throws Exception {

        // arrange
        Genre sciFi = new Genre("Science Fiction");
        genreRepository.save(sciFi);
        sciFi = genreRepository.save(sciFi);

        Author author = new Author("author-1", "John Doe", "An author bio", null);
        author = authorRepository.save(author);

        Book book = new Book(
                "book-1",
                "9780306406157",
                "Book A",
                "A description",
                sciFi,
                List.of(author),
                null);

        bookRepository.save(book);

        mockMvc.perform(get("/api/genres/top5"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnNotFound_whenNoLendingsLastTwelveMonths() throws Exception {

        mockMvc.perform(get("/api/genres/lendingsPerMonthLastTwelveMonths"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnOk_whenLendingsLastTwelveMonthsExist() throws Exception {

        // Seed minimal : Genre + Author + Book + Reader + ReaderDetails + Lending(now)
        Genre history = genreRepository.save(new Genre("History"));
        Author a = authorRepository.save(new Author("author-1", "Alice", "bio", null));
        Book b = bookRepository.save(new Book("book-1", "9783161484100", "H1", "desc", history, List.of(a), null));

        Reader reader = userRepository.save(new Reader("user-1@test.com", "Password123!"));
        ReaderDetails rd = readerRepository
                .save(new ReaderDetails(1001, reader, "1990-01-01", "912345678", true, false, false, null, null));

        lendingService.create(new CreateLendingRequest(b.getIsbn(), rd.getReaderNumber()));

        mockMvc.perform(get("/api/genres/lendingsPerMonthLastTwelveMonths"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnNotFound_whenAvgDurationNoDataInRange() throws Exception {

        mockMvc.perform(get("/api/genres/lendingsAverageDurationPerMonth")
                .param("startDate", LocalDate.now().minusMonths(1).withDayOfMonth(1).toString())
                .param("endDate",
                        LocalDate.now().minusMonths(1).withDayOfMonth(LocalDate.now().minusMonths(1).lengthOfMonth())
                                .toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not found"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = { "USER", "LIBRARIAN" })
    void shouldReturnOk_whenAvgDurationDataExistsInRange() throws Exception {

        Genre poetry = genreRepository.save(new Genre("Poetry"));
        Author a = authorRepository.save(new Author("auth-1", "Bob", "bio", null));
        Book b = bookRepository.save(new Book("book-1", "9780134685991", "P1", "desc", poetry, List.of(a), null));

        Reader reader = userRepository.save(new Reader("user-1@test.com", "Password123!"));
        ReaderDetails rd = readerRepository
                .save(new ReaderDetails(1001, reader, "1990-01-01", "912345678", true, false, false, null, null));

        
        Lending l1 = lendingService.create(new CreateLendingRequest(b.getIsbn(), rd.getReaderNumber())); 

        l1 = lendingService.setReturned(l1.getLendingNumber(),
                new pt.psoft.g1.psoftg1.lendingmanagement.services.SetLendingReturnedRequest("Returned on time"),
                l1.getVersion()); // mark returned today
       

        String start = LocalDate.now().withDayOfMonth(1).toString();
        String end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).toString();

        mockMvc.perform(get("/api/genres/lendingsAverageDurationPerMonth")
                .param("startDate", start)
                .param("endDate", end))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }
}
