package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDTO;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDTO;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.services.Page;

import jakarta.persistence.Tuple;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringDataGenreRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<Tuple> tupleQuery;

    @Mock
    private CriteriaQuery<GenreLendingsDTO> dtoQuery;

    @Mock
    private Root<Lending> lendingRoot;

    @Mock
    private Join<Object, Object> bookJoin;

    @Mock
    private Join<Object, Object> genreJoin;

    @Mock
    private TypedQuery<Tuple> typedTupleQuery;

    @Mock
    private TypedQuery<GenreLendingsDTO> typedDtoQuery;

    private GenreRepoCustomImpl genreRepoCustom;

    @BeforeEach
    void setUp() {
        genreRepoCustom = new GenreRepoCustomImpl(entityManager);
    }

    @Test
    void getAverageLendingsInMonth() {
        // Given
        LocalDate month = LocalDate.of(2025, 10, 1);
        Page page = new Page(1, 10);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(GenreLendingsDTO.class)).thenReturn(dtoQuery);
        when(dtoQuery.from(Lending.class)).thenReturn(lendingRoot);
        when(lendingRoot.join("book", JoinType.LEFT)).thenReturn(bookJoin);
        when(bookJoin.join("genre", JoinType.LEFT)).thenReturn(genreJoin);
        when(entityManager.createQuery(dtoQuery)).thenReturn(typedDtoQuery);

        List<GenreLendingsDTO> expectedResults = Arrays.asList(
            new GenreLendingsDTO("Drama", 0.2),
            new GenreLendingsDTO("Fiction", 0.1)
        );
        when(typedDtoQuery.getResultList()).thenReturn(expectedResults);

        // When
        List<GenreLendingsDTO> result = genreRepoCustom.getAverageLendingsInMonth(month, page);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Drama", result.get(0).getGenre());
        assertEquals(0.2, result.get(0).getValue().doubleValue());
        verify(entityManager).createQuery(dtoQuery);
        verify(typedDtoQuery).setFirstResult(0);
        verify(typedDtoQuery).setMaxResults(10);
    }

    @Test
    void getLendingsPerMonthLastYearByGenre() {
        // Given
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(tupleQuery);
        when(tupleQuery.from(Lending.class)).thenReturn(lendingRoot);
        when(lendingRoot.join("book")).thenReturn(bookJoin);
        when(bookJoin.join("genre")).thenReturn(genreJoin);
        when(entityManager.createQuery(tupleQuery)).thenReturn(typedTupleQuery);

        Tuple mockTuple = mock(Tuple.class);
        when(mockTuple.get(0, String.class)).thenReturn("Drama");
        when(mockTuple.get(1, Integer.class)).thenReturn(2025);
        when(mockTuple.get(2, Integer.class)).thenReturn(10);
        when(mockTuple.get(3, Long.class)).thenReturn(5L);

        when(typedTupleQuery.getResultList()).thenReturn(List.of(mockTuple));

        // When
        List<GenreLendingsPerMonthDTO> result = genreRepoCustom.getLendingsPerMonthLastYearByGenre();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        GenreLendingsPerMonthDTO dto = result.get(0);
        assertEquals(2025, dto.getYear());
        assertEquals(10, dto.getMonth());
        assertEquals(1, dto.getValues().size());
        assertEquals("Drama", dto.getValues().get(0).getGenre());
        assertEquals(5.0, dto.getValues().get(0).getValue().doubleValue());
    }

    @Test
    void getLendingsAverageDurationPerMonth() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 12, 31);

        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createTupleQuery()).thenReturn(tupleQuery);
        when(tupleQuery.from(Lending.class)).thenReturn(lendingRoot);
        when(lendingRoot.join("book")).thenReturn(bookJoin);
        when(bookJoin.join("genre")).thenReturn(genreJoin);
        when(entityManager.createQuery(tupleQuery)).thenReturn(typedTupleQuery);

        Tuple mockTuple = mock(Tuple.class);
        when(mockTuple.get(0, String.class)).thenReturn("Drama");
        when(mockTuple.get(1, Integer.class)).thenReturn(2025);
        when(mockTuple.get(2, Integer.class)).thenReturn(10);
        when(mockTuple.get(3, Double.class)).thenReturn(7.5);

        when(typedTupleQuery.getResultList()).thenReturn(List.of(mockTuple));

        // When
        List<GenreLendingsPerMonthDTO> result = genreRepoCustom.getLendingsAverageDurationPerMonth(startDate, endDate);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        GenreLendingsPerMonthDTO dto = result.get(0);
        assertEquals(2025, dto.getYear());
        assertEquals(10, dto.getMonth());
        assertEquals(1, dto.getValues().size());
        assertEquals("Drama", dto.getValues().get(0).getGenre());
        assertEquals(7.5, dto.getValues().get(0).getValue().doubleValue());
    }
}