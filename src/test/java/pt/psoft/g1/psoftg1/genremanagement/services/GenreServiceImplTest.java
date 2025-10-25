package pt.psoft.g1.psoftg1.genremanagement.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cglib.core.Local;

import static org.assertj.core.api.Assertions.as;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

@ExtendWith(MockitoExtension.class)
class GenreServiceImplTest {
    @InjectMocks
    private GenreServiceImpl genreService;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private Page page;

    @Test
    void findByString_returnsGenreWhenFound() {
        Genre genre = new Genre("Fantasy");

        when(genreRepository.findByString("Fantasy")).thenReturn(Optional.of(genre));

        Optional<Genre> result = genreService.findByString("Fantasy");

        assert (result.isPresent());
        assert (result.get().getGenre().equals("Fantasy"));
    }

    @Test
    void findByString_returnsEmptyWhenNotFound() {
        when(genreRepository.findByString("NonExistentGenre")).thenReturn(Optional.empty());

        Optional<Genre> result = genreService.findByString("NonExistentGenre");

        assert (result.isEmpty());
    }

    @Test
    void findByString_handlesNullInput() {
        when(genreRepository.findByString(null)).thenReturn(Optional.empty());

        Optional<Genre> result = genreService.findByString(null);

        assert (result.isEmpty());
    }

    @Test
    void findAll_returnsAllGenres() {
        Genre genre1 = new Genre("Fantasy");
        Genre genre2 = new Genre("Sci-Fi");

        when(genreRepository.findAll()).thenReturn(List.of(genre1, genre2));

        Iterable<Genre> result = genreService.findAll();

        assert (result.iterator().hasNext());
        assert (((List<Genre>) result).size() == 2);
        assert (((List<Genre>) result).get(0).getGenre().equals("Fantasy"));
        assert (((List<Genre>) result).get(1).getGenre().equals("Sci-Fi"));
    }

    @Test
    void findAll_returnsEmptyWhenNoGenres() {
        when(genreRepository.findAll()).thenReturn(List.of());

        Iterable<Genre> result = genreService.findAll();

        assert (!result.iterator().hasNext());
    }

    @Test
    void findTopGenreByBooks_returnsTop5Genres() {
        GenreBookCountDTO dto1 = new GenreBookCountDTO("Fantasy", 100);
        GenreBookCountDTO dto2 = new GenreBookCountDTO("Sci-Fi", 80);
        GenreBookCountDTO dto3 = new GenreBookCountDTO("Romance", 60);
        GenreBookCountDTO dto4 = new GenreBookCountDTO("Horror", 40);
        GenreBookCountDTO dto5 = new GenreBookCountDTO("Mystery", 20);

        when(genreRepository.findTop5GenreByBookCount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(dto1, dto2, dto3, dto4, dto5)));

        List<GenreBookCountDTO> result = genreService.findTopGenreByBooks();

        assert (result.size() == 5);
        assert (result.get(0).getGenre().equals("Fantasy"));
        assert (result.get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(2).getGenre().equals("Romance"));
        assert (result.get(3).getGenre().equals("Horror"));
        assert (result.get(4).getGenre().equals("Mystery"));
    }

    @Test
    void findTopGenreByBooks_returnsLessThan5WhenNotEnoughGenres() {
        GenreBookCountDTO dto1 = new GenreBookCountDTO("Fantasy", 100);
        GenreBookCountDTO dto2 = new GenreBookCountDTO("Sci-Fi", 80);

        when(genreRepository.findTop5GenreByBookCount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(dto1, dto2)));
        List<GenreBookCountDTO> result = genreService.findTopGenreByBooks();
        assert (result.size() == 2);
        assert (result.get(0).getGenre().equals("Fantasy"));
        assert (result.get(1).getGenre().equals("Sci-Fi"));
    }

    @Test
    void findTopGenreByBooks_returnsEmptyWhenNoGenres() {
        when(genreRepository.findTop5GenreByBookCount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        List<GenreBookCountDTO> result = genreService.findTopGenreByBooks();
        assert (result.isEmpty());
    }

    @Test
    void save_savesAndReturnsGenre() {
        Genre genre = new Genre("Fantasy");

        when(genreRepository.save(genre)).thenReturn(genre);

        Genre result = genreService.save(genre);
        assert (result.getGenre().equals("Fantasy"));
    }

    @Test
    void save_handlesNullGenre() {
        when(genreRepository.save(null)).thenReturn(null);
        Genre result = genreService.save(null);
        assert (result == null);
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_returnList() {
        Genre genre1 = new Genre("Fantasy");
        Genre genre2 = new Genre("Sci-Fi");

        GenreLendingsPerMonthDTO dto1 = new GenreLendingsPerMonthDTO(2023, 1,
                List.of(new GenreLendingsDTO(genre1.getGenre(), 10), new GenreLendingsDTO(genre2.getGenre(), 5)));
        GenreLendingsPerMonthDTO dto2 = new GenreLendingsPerMonthDTO(2023, 2,
                List.of(new GenreLendingsDTO(genre1.getGenre(), 8), new GenreLendingsDTO(genre2.getGenre(), 7)));

        when(genreRepository.getLendingsPerMonthLastYearByGenre())
                .thenReturn(List.of(dto1, dto2));

        List<GenreLendingsPerMonthDTO> result = genreService.getLendingsPerMonthLastYearByGenre();

        assert (result.size() == 2);
        assert (result.get(0).getYear() == 2023);
        assert (result.get(0).getMonth() == 1);
        assert (result.get(0).getValues().size() == 2);
        assert (result.get(0).getValues().get(0).getGenre().equals("Fantasy"));
        assert (result.get(0).getValues().get(0).getValue().doubleValue() == 10);
        assert (result.get(0).getValues().get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(0).getValues().get(1).getValue().doubleValue() == 5);

        assert (result.get(1).getYear() == 2023);
        assert (result.get(1).getMonth() == 2);
        assert (result.get(1).getValues().size() == 2);
        assert (result.get(1).getValues().get(0).getGenre().equals("Fantasy"));
        assert (result.get(1).getValues().get(0).getValue().doubleValue() == 8);
        assert (result.get(1).getValues().get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(1).getValues().get(1).getValue().doubleValue() == 7);
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_returnsEmptyWhenNoData() {
        when(genreRepository.getLendingsPerMonthLastYearByGenre())
                .thenReturn(List.of());
        List<GenreLendingsPerMonthDTO> result = genreService.getLendingsPerMonthLastYearByGenre();
        assert (result.isEmpty());
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_handlesNullReturn() {
        when(genreRepository.getLendingsPerMonthLastYearByGenre())
                .thenReturn(null);
        List<GenreLendingsPerMonthDTO> result = genreService.getLendingsPerMonthLastYearByGenre();
        assert (result == null);
    }

    @Test
    void getAverageLendingsInMonth_returnList() {
        GenreLendingsDTO dto1 = new GenreLendingsDTO("Fantasy", 10);
        GenreLendingsDTO dto2 = new GenreLendingsDTO("Sci-Fi", 5);

        GetAverageLendingsQuery avgQuery = new GetAverageLendingsQuery(2025, 3);

        var month = java.time.LocalDate.of(avgQuery.getYear(), avgQuery.getMonth(), 1);
        when(genreRepository.getAverageLendingsInMonth(
                month, page))
                .thenReturn(List.of(dto1, dto2));

        List<GenreLendingsDTO> result = genreService.getAverageLendings(
                avgQuery, page);

        assert (result.size() == 2);
        assert (result.get(0).getGenre().equals("Fantasy"));
        assert (result.get(0).getValue().doubleValue() == 10);
        assert (result.get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(1).getValue().doubleValue() == 5);
    }

    @Test
    void getAverageLendingsInMonth_returnsEmptyWhenNoData() {
        GetAverageLendingsQuery avgQuery = new GetAverageLendingsQuery(2025, 5);

        var month = java.time.LocalDate.of(avgQuery.getYear(), avgQuery.getMonth(), 1);
        when(genreRepository.getAverageLendingsInMonth(
                month, page))
                .thenReturn(List.of());
        List<GenreLendingsDTO> result = genreService.getAverageLendings(
                avgQuery, page);
        assert (result.isEmpty());
    }

    @Test
    void getAverageLendingsInMonth_whenPageNull_useDefaultPage() {
        Genre genre1 = new Genre("Fantasy");
        GenreLendingsDTO dto1 = new GenreLendingsDTO(genre1, 12.0);
        GetAverageLendingsQuery avgQuery = new GetAverageLendingsQuery();
        avgQuery.setYear(2025);
        avgQuery.setMonth(5);

        var month = java.time.LocalDate.of(avgQuery.getYear(), avgQuery.getMonth(), 1);

        when(genreRepository.getAverageLendingsInMonth(
                month, new Page(1, 10)))
                .thenReturn(List.of(dto1));

        List<GenreLendingsDTO> result = genreService.getAverageLendings(
                avgQuery, null);

        assert (result.size() == 1);
        assert (result.get(0).getGenre().equals("Fantasy"));
        assert (result.get(0).getValue().doubleValue() == 12);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenStartAfterEnd_shouldThrowsIllegalArgument() {
        assertThrows("Start date cannot be after end date", IllegalArgumentException.class,
                () -> genreService.getLendingsAverageDurationPerMonth("2023-09-01",
                        "2023-08-31"));

        verifyNoInteractions(genreRepository);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenCorrectInput_shouldReturnList() {
        Genre genre1 = new Genre("Fantasy");
        Genre genre2 = new Genre("Sci-Fi");

        GenreLendingsPerMonthDTO dto1 = new GenreLendingsPerMonthDTO(2023, 6,
                List.of(new GenreLendingsDTO(genre1.getGenre(), 15.5), new GenreLendingsDTO(genre2.getGenre(), 12.0)));
        GenreLendingsPerMonthDTO dto2 = new GenreLendingsPerMonthDTO(2023, 7,
                List.of(new GenreLendingsDTO(genre1.getGenre(), 10.0), new GenreLendingsDTO(genre2.getGenre(), 8.5)));

        when(genreRepository.getLendingsAverageDurationPerMonth(
                java.time.LocalDate.of(2023, 6, 1),
                java.time.LocalDate.of(2023, 7, 31)))
                .thenReturn(List.of(dto1, dto2));

        List<GenreLendingsPerMonthDTO> result = genreService.getLendingsAverageDurationPerMonth("2023-06-01",
                "2023-07-31");

        assert (result.size() == 2);
        assert (result.get(0).getYear() == 2023);
        assert (result.get(0).getMonth() == 6);
        assert (result.get(0).getValues().size() == 2);
        assert (result.get(0).getValues().get(0).getGenre().equals("Fantasy"));
        assert (result.get(0).getValues().get(0).getValue().doubleValue() == 15.5);
        assert (result.get(0).getValues().get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(0).getValues().get(1).getValue().doubleValue() == 12.0);

        assert (result.get(1).getYear() == 2023);
        assert (result.get(1).getMonth() == 7);
        assert (result.get(1).getValues().size() == 2);
        assert (result.get(1).getValues().get(0).getGenre().equals("Fantasy"));
        assert (result.get(1).getValues().get(0).getValue().doubleValue() == 10.0);
        assert (result.get(1).getValues().get(1).getGenre().equals("Sci-Fi"));
        assert (result.get(1).getValues().get(1).getValue().doubleValue() == 8.5);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenNoData_shouldThrowsNotFound() {
        LocalDate startDate = LocalDate.of(2023, 8, 1);
        LocalDate endDate = LocalDate.of(2023, 8, 31);

        when(genreRepository.getLendingsAverageDurationPerMonth(startDate, endDate)).thenReturn(List.of());

        assertThrows(NotFoundException.class,
                () -> genreService.getLendingsAverageDurationPerMonth("2023-08-01",
                        "2023-08-31"));

        verify(genreRepository).getLendingsAverageDurationPerMonth(startDate, endDate);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenInvalidDateFormat_shouldThrowsIllegalArgument() {
        assertThrows("Expected format is YYYY-MM-DD", IllegalArgumentException.class,
                () -> genreService.getLendingsAverageDurationPerMonth("2023/08/01",
                        "2023-08-31"));

        verifyNoInteractions(genreRepository);
    }

}
