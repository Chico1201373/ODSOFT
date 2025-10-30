package pt.psoft.g1.psoftg1.genremanagement.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Optional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class GenreRepositoryIT {
    @Test
    void findTop5GenreByBookCount_returnsGenresOrderedByBookCount() {
        Genre g1 = new Genre("Drama");
        Genre g2 = new Genre("Sci-Fi");
        Genre g3 = new Genre("Fantasy");
        em.persist(g1);
        em.persist(g2);
        em.persist(g3);
        em.flush();

        pt.psoft.g1.psoftg1.authormanagement.model.Author author = new pt.psoft.g1.psoftg1.authormanagement.model.Author(
                "AUTH2", "Author Name", "Bio", "photo");
        em.persist(author);

        // Drama: 3 books, Sci-Fi: 2 books, Fantasy: 1 book
        String[] dramaIsbns = { "9783161484100", "9780306406157", "9781861972712" };
        String[] scifiIsbns = { "9781234567897", "9780470059029" };
        String fantasyIsbn = "9780136091813";
        for (int i = 0; i < 3; i++) {
            em.persist(new pt.psoft.g1.psoftg1.bookmanagement.model.Book("d" + i, dramaIsbns[i], "Drama Book" + i,
                    "desc", g1, List.of(author), null));
        }
        for (int i = 0; i < 2; i++) {
            em.persist(new pt.psoft.g1.psoftg1.bookmanagement.model.Book("s" + i, scifiIsbns[i], "Sci-Fi Book" + i,
                    "desc", g2, List.of(author), null));
        }
        em.persist(new pt.psoft.g1.psoftg1.bookmanagement.model.Book("f0", fantasyIsbn, "Fantasy Book0", "desc", g3,
                List.of(author), null));
        em.flush();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        var res = genreRepository.findTop5GenreByBookCount(pageable);
        assertEquals(3, res.getContent().size());
        assertEquals("Drama", res.getContent().get(0).getGenre());
        assertEquals(3L, res.getContent().get(0).getBookCount());
        assertEquals("Sci-Fi", res.getContent().get(1).getGenre());
        assertEquals(2L, res.getContent().get(1).getBookCount());
        assertEquals("Fantasy", res.getContent().get(2).getGenre());
        assertEquals(1L, res.getContent().get(2).getBookCount());
    }

    @Test
    void getAverageLendingsInMonth_returnsCorrectAverage() {
        Genre genre = new Genre("Drama");
        em.persist(genre);
        pt.psoft.g1.psoftg1.authormanagement.model.Author author = new pt.psoft.g1.psoftg1.authormanagement.model.Author(
                "AUTH3", "Author Name", "Bio", "photo");
        em.persist(author);
        pt.psoft.g1.psoftg1.bookmanagement.model.Book book = new pt.psoft.g1.psoftg1.bookmanagement.model.Book("book2",
                "9780321356680", "Title 2", "desc", genre, List.of(author), null);
        em.persist(book);
        pt.psoft.g1.psoftg1.usermanagement.model.Reader reader = pt.psoft.g1.psoftg1.usermanagement.model.Reader
                .newReader("u2@example.com", "Password1!", "name");
        try {
            Field createdAt = reader.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(reader, LocalDateTime.now());
            Field modifiedAt = reader.getClass().getSuperclass().getDeclaredField("modifiedAt");
            modifiedAt.setAccessible(true);
            modifiedAt.set(reader, LocalDateTime.now());
            Field createdBy = reader.getClass().getSuperclass().getDeclaredField("createdBy");
            createdBy.setAccessible(true);
            createdBy.set(reader, "test");
            Field modifiedBy = reader.getClass().getSuperclass().getDeclaredField("modifiedBy");
            modifiedBy.setAccessible(true);
            modifiedBy.set(reader, "test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        em.persist(reader);
        pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails readerDetails = new pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails(
                2, reader, "2000-01-01", "912345679", true, false, false, null, null);
        em.persist(readerDetails);
        LocalDate month = LocalDate.of(2025, 10, 1);
        // 5 lendings in October
        for (int i = 0; i < 5; i++) {
            pt.psoft.g1.psoftg1.lendingmanagement.model.Lending l = pt.psoft.g1.psoftg1.lendingmanagement.model.Lending
                    .newBootstrappingLending(book, readerDetails, month.getYear(), i + 1, month.plusDays(i), null, 7,
                            0);
            em.persist(l);
        }
        em.flush();
        pt.psoft.g1.psoftg1.shared.services.Page page = new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
        var res = genreRepository.getAverageLendingsInMonth(month, page);
        double expected = Double.valueOf(String.format(Locale.US, "%.1f", 5.0 / 31.0)); // 0.2
        assert (res.get(0).getGenre().equals("Drama"));
        assertEquals(expected, res.get(0).getValue().doubleValue(), 1e-6);

    }

    @Test
    void getLendingsAverageDurationPerMonth_returnsCorrectAverageDuration() {
        Genre genre = new Genre("Drama");
        em.persist(genre);
        pt.psoft.g1.psoftg1.authormanagement.model.Author author = new pt.psoft.g1.psoftg1.authormanagement.model.Author(
                "AUTH4", "Author Name", "Bio", "photo");
        em.persist(author);
        pt.psoft.g1.psoftg1.bookmanagement.model.Book book = new pt.psoft.g1.psoftg1.bookmanagement.model.Book("book3",
                "9780136091813", "Title 3", "desc", genre, List.of(author), null);
        em.persist(book);
        pt.psoft.g1.psoftg1.usermanagement.model.Reader reader = pt.psoft.g1.psoftg1.usermanagement.model.Reader
                .newReader("u3@example.com", "Password1!", "name");
        try {
            Field createdAt = reader.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(reader, LocalDateTime.now());
            Field modifiedAt = reader.getClass().getSuperclass().getDeclaredField("modifiedAt");
            modifiedAt.setAccessible(true);
            modifiedAt.set(reader, LocalDateTime.now());
            Field createdBy = reader.getClass().getSuperclass().getDeclaredField("createdBy");
            createdBy.setAccessible(true);
            createdBy.set(reader, "test");
            Field modifiedBy = reader.getClass().getSuperclass().getDeclaredField("modifiedBy");
            modifiedBy.setAccessible(true);
            modifiedBy.set(reader, "test");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        em.persist(reader);
        pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails readerDetails = new pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails(
                3, reader, "2000-01-01", "912345680", true, false, false, null, null);
        em.persist(readerDetails);
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);
        // 2 lendings in September, returned after 5 and 10 days
        pt.psoft.g1.psoftg1.lendingmanagement.model.Lending l1 = pt.psoft.g1.psoftg1.lendingmanagement.model.Lending
                .newBootstrappingLending(book, readerDetails, startDate.getYear(), 1, startDate, startDate.plusDays(5),
                        7, 0);
        pt.psoft.g1.psoftg1.lendingmanagement.model.Lending l2 = pt.psoft.g1.psoftg1.lendingmanagement.model.Lending
                .newBootstrappingLending(book, readerDetails, startDate.getYear(), 2, startDate.plusDays(1),
                        startDate.plusDays(11), 7, 0);
        em.persist(l1);
        em.persist(l2);
        em.flush();
        var res = genreRepository.getLendingsAverageDurationPerMonth(startDate, endDate);
        // Average duration for Drama in September: (5 + 10) / 2 = 7.5
        boolean found = res.stream()
                .anyMatch(dto -> dto.getYear() == 2025 && dto.getMonth() == 9 && dto.getValues().stream().anyMatch(
                        v -> v.getGenre().equals("Drama") && Math.abs(v.getValue().doubleValue() - 7.5) < 0.1));
        assertTrue(found, "Expected average lending duration for Drama in September");
    }

    @Autowired
    private TestEntityManager em;

    @Autowired
    private GenreRepository genreRepository;

    @Test
    void findByString_returnsMatch_whenExists() {
        em.persist(new Genre("Drama"));
        em.flush();

        Optional<Genre> res = genreRepository.findByString("Drama");

        assertTrue(res.isPresent());
        assertEquals("Drama", res.get().getGenre());
    }

    @Test
    void findByString_returnsEmpty_whenNotExists() {
        em.persist(new Genre("Drama"));
        em.flush();

        Optional<Genre> res = genreRepository.findByString("NonExistentGenre");
        assertTrue(res.isEmpty());
    }

    @Test
    void findByString_returnsEmpty_whenDatabaseIsEmpty() {
        Optional<Genre> res = genreRepository.findByString("AnyGenre");
        assertTrue(res.isEmpty());
    }

    @Test
    void findAll_returnsAllGenres() {
        em.persist(new Genre("Drama"));
        em.persist(new Genre("Sci-Fi"));
        em.persist(new Genre("Fantasy"));
        em.flush();

        List<Genre> res = new ArrayList<>();
        genreRepository.findAll().forEach(res::add);

        assertEquals(3, res.size());
    }

    @Test
    void findAll_returnsEmptyList_whenDatabaseIsEmpty() {
        List<Genre> res = new ArrayList<>();
        genreRepository.findAll().forEach(res::add);
        assertEquals(0, res.size());
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_returnsEmptyList_whenNoData() {
        List<GenreLendingsPerMonthDTO> res = genreRepository.getLendingsPerMonthLastYearByGenre();
        assertEquals(0, res.size());
    }


    @Test
    void delete_removesGenre() {
        Genre g = new Genre("ToDelete");
        em.persist(g);
        em.flush();

        // Ensure present
        Optional<Genre> found = genreRepository.findByString("ToDelete");
        assertTrue(found.isPresent());

        genreRepository.delete(g);

        Optional<Genre> after = genreRepository.findByString("ToDelete");
        assertTrue(after.isEmpty());
    }

}
