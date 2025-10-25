package pt.psoft.g1.psoftg1.genremanagement.api;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDTO;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreService;
import pt.psoft.g1.psoftg1.genremanagement.services.GetAverageLendingsQuery;
import pt.psoft.g1.psoftg1.shared.api.ListResponse;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;

@ExtendWith(MockitoExtension.class)
class GenreControllerTest {
    @InjectMocks
    private GenreController genreController;

    @Mock
    private GenreService genreService;

    @Mock
    private GenreViewMapper genreViewMapper;

    @Mock
    private SearchRequest<GetAverageLendingsQuery> avgReq;
    @Mock
    private GetAverageLendingsQuery avgQuery;
    @Mock
    private Page page;

    @Test
    void getAverageLendings_shouldReturnList() {
        when(avgReq.getQuery()).thenReturn(avgQuery);
        when(avgReq.getPage()).thenReturn(page);
        List<GenreLendingsDTO> genreDto = List.of(
                new GenreLendingsDTO("Drama", 3.0),
                new GenreLendingsDTO("Sci-Fi", 5.0));

        GenreLendingsView v1 = new GenreLendingsView();
        v1.setGenre("Drama");
        v1.setValue(3.0);
        GenreLendingsView v2 = new GenreLendingsView();
        v2.setGenre("Sci-Fi");
        v2.setValue(5.0);
        List<GenreLendingsView> mapped = List.of(v1, v2);

        when(genreService.getAverageLendings(avgReq.getQuery(), avgReq.getPage())).thenReturn(genreDto);
        when(genreViewMapper.toGenreAvgLendingsView(genreDto)).thenReturn(mapped);

        ListResponse<GenreLendingsView> res = genreController.getAverageLendings(avgReq);

        assertNotNull(res);
        assertEquals(2, res.getItems().size());
        assertEquals("Sci-Fi", res.getItems().get(1).getGenre());
        assertEquals(5.0, res.getItems().get(1).getValue().doubleValue(), 1e-9);

        verify(genreService).getAverageLendings(avgReq.getQuery(), avgReq.getPage());
        verify(genreViewMapper).toGenreAvgLendingsView(genreDto);
        verifyNoMoreInteractions(genreService, genreViewMapper);
    }

    @Test
    void getTop_whenNonEmpty_shouldReturnList() {
        List<GenreBookCountDTO> bookCount = List.of(
                new GenreBookCountDTO("Drama", 3),
                new GenreBookCountDTO("Fantasy", 4));

        GenreView gv1 = new GenreView();
        gv1.setGenre("Drama");
        GenreView gv2 = new GenreView();
        gv2.setGenre("Fantasy");

        GenreBookCountView gbcv1 = new GenreBookCountView();
        gbcv1.setGenreView(gv1);
        gbcv1.setBookCount(3L);

        GenreBookCountView gbcv2 = new GenreBookCountView();
        gbcv2.setGenreView(gv2);
        gbcv2.setBookCount(4L);

        List<GenreBookCountView> genreBookCountViews = List.of(
                gbcv1,
                gbcv2);

        when(genreService.findTopGenreByBooks()).thenReturn(bookCount);
        when(genreViewMapper.toGenreBookCountView(bookCount)).thenReturn(genreBookCountViews);

        ListResponse<GenreBookCountView> res = genreController.getTop();

        assertNotNull(res);
        assertEquals(2, res.getItems().size());
        assertEquals("Fantasy", res.getItems().get(1).getGenreView().getGenre());
        assertEquals(4L, res.getItems().get(1).getBookCount());

        verify(genreService).findTopGenreByBooks();
        verify(genreViewMapper).toGenreBookCountView(bookCount);
        verifyNoMoreInteractions(genreService, genreViewMapper);
    }

    @Test
    void getTop_whenEmpty_throwsNotFound() {
        when(genreService.findTopGenreByBooks()).thenReturn(List.of());

        assertThrows("No genres to show", NotFoundException.class, () -> genreController.getTop());

        verify(genreService).findTopGenreByBooks();
        verifyNoInteractions(genreViewMapper);
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_whenNonEmpty_returnList() {
        List<GenreLendingsDTO> genreLendingsDTOs = List.of(
                new GenreLendingsDTO("Romance", 3),
                new GenreLendingsDTO("Fantasy", 5));

        List<GenreLendingsPerMonthDTO> genreLendingsPerMonthDTOs = List.of(
                new GenreLendingsPerMonthDTO(2025, 9, null),
                new GenreLendingsPerMonthDTO(2025, 10, genreLendingsDTOs));

        GenreLendingsView glv1 = new GenreLendingsView();
        glv1.setGenre("Romance");
        glv1.setValue(3);
        GenreLendingsView glv2 = new GenreLendingsView();
        glv2.setGenre("Fantasy");
        glv2.setValue(5);

        List<GenreLendingsView> lendingsCount = List.of(
                glv1, glv2);

        List<GenreLendingsCountPerMonthView> genreLendingsCountPerMonthViews = List.of(
                new GenreLendingsCountPerMonthView(2025, 9, null),
                new GenreLendingsCountPerMonthView(2025, 9, lendingsCount));

        when(genreService.getLendingsPerMonthLastYearByGenre()).thenReturn(genreLendingsPerMonthDTOs);
        when(genreViewMapper.toGenreLendingsCountPerMonthView(genreLendingsPerMonthDTOs))
                .thenReturn(genreLendingsCountPerMonthViews);

        ListResponse<GenreLendingsCountPerMonthView> res = genreController.getLendingsPerMonthLastYearByGenre();

        assertNotNull(res);
        assertEquals(2, res.getItems().size());
        assertEquals(2025, res.getItems().get(0).getYear());
        assertEquals(9, res.getItems().get(0).getMonth());
        assertNull(res.getItems().get(0).getLendingsCount());
        assertEquals("Romance", res.getItems().get(1).getLendingsCount().get(0).getGenre());

        verify(genreService).getLendingsPerMonthLastYearByGenre();
        verify(genreViewMapper).toGenreLendingsCountPerMonthView(genreLendingsPerMonthDTOs);
        verifyNoMoreInteractions(genreService, genreViewMapper);
    }

    @Test
    void getLendingsPerMonthLastYearByGenre_whenEmpty_throwsNotFound() {
        when(genreService.getLendingsPerMonthLastYearByGenre()).thenReturn(List.of());

        assertThrows("No genres to show", NotFoundException.class,
                () -> genreController.getLendingsPerMonthLastYearByGenre());

        verify(genreService).getLendingsPerMonthLastYearByGenre();
        verifyNoInteractions(genreViewMapper);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenNonEmpty_returnList() {
        String start = "2025-01-01";
        String end = "2025-3-31";

        List<GenreLendingsDTO> glDTOs = List.of(
                new GenreLendingsDTO("Romance", 3),
                new GenreLendingsDTO("Fantasy", 5));

        List<GenreLendingsPerMonthDTO> glpmDTOs = List.of(
                new GenreLendingsPerMonthDTO(2025, 1, null),
                new GenreLendingsPerMonthDTO(2025, 2, glDTOs),
                new GenreLendingsPerMonthDTO(2025, 3, null));

        GenreLendingsView glv1 = new GenreLendingsView();
        glv1.setGenre("Romance");
        glv1.setValue(3);
        GenreLendingsView glv2 = new GenreLendingsView();
        glv2.setGenre("Fantasy");
        glv2.setValue(5);

        List<GenreLendingsView> lendingsCount = List.of(
                glv1, glv2);

        List<GenreLendingsAvgPerMonthView> genreLendingsAvgPerMonthViews = List.of(
                new GenreLendingsAvgPerMonthView(2025, 1, null),
                new GenreLendingsAvgPerMonthView(2025, 2, lendingsCount),
                new GenreLendingsAvgPerMonthView(2025, 3, null));

        when(genreService.getLendingsAverageDurationPerMonth(start, end)).thenReturn(glpmDTOs);
        when(genreViewMapper.toGenreLendingsAveragePerMonthView(glpmDTOs)).thenReturn(genreLendingsAvgPerMonthViews);

        ListResponse<GenreLendingsAvgPerMonthView> res = genreController.getLendingsAverageDurationPerMonth(start, end);

        assertNotNull(res);
        assertEquals(3, res.getItems().size());
        assertEquals(2025, res.getItems().get(0).getYear());
        assertEquals(1, res.getItems().get(0).getMonth());
        assertEquals(2, res.getItems().get(1).getMonth());
        assertEquals(3, res.getItems().get(2).getMonth());
        assertNull(res.getItems().get(0).getDurationAverages());
        assertEquals("Romance", res.getItems().get(1).getDurationAverages().get(0).getGenre());

        verify(genreService).getLendingsAverageDurationPerMonth(start, end);
        verify(genreViewMapper).toGenreLendingsAveragePerMonthView(glpmDTOs);
        verifyNoMoreInteractions(genreService, genreViewMapper);
    }

    @Test
    void getLendingsAverageDurationPerMonth_whenEmpty_throwsNotFound() {
        String start = "2025-01-01";
        String end = "2025-3-31";
        when(genreService.getLendingsAverageDurationPerMonth(start, end)).thenReturn(List.of());

        assertThrows("No genres to show", NotFoundException.class,
                () -> genreController.getLendingsAverageDurationPerMonth(start, end));

        verify(genreService).getLendingsAverageDurationPerMonth(start, end);
        verifyNoInteractions(genreViewMapper);
    }

}