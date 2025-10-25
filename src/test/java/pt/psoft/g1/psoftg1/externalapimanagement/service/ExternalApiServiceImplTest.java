package pt.psoft.g1.psoftg1.externalapimanagement.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import pt.psoft.g1.psoftg1.externalapimanagement.model.BookIsbnAPI;

public class ExternalApiServiceImplTest {

    @Mock
    private BookIsbnAPI bookIsbnAPI;

    private ExternalApiServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ExternalApiServiceImpl(bookIsbnAPI);
    }

    @Test
    @DisplayName("Delegates call to BookIsbnAPI and returns same value")
    void delegatesGetIsbnToApi() {
        when(bookIsbnAPI.getIsbn("Title A")).thenReturn("9781234567897");

        String result = service.getIsbn("Title A");

        assertEquals("9781234567897", result);
        verify(bookIsbnAPI, times(1)).getIsbn("Title A");
    }

    @Test
    @DisplayName("Returns null when API returns null")
    void returnsNullWhenApiReturnsNull() {
        when(bookIsbnAPI.getIsbn("Unknown")).thenReturn(null);

        String result = service.getIsbn("Unknown");

        assertNull(result);
        verify(bookIsbnAPI, times(1)).getIsbn("Unknown");
    }

    @Test
    @DisplayName("Returns empty string when API returns empty string")
    void returnsEmptyStringWhenApiReturnsEmptyString() {
        when(bookIsbnAPI.getIsbn("EmptyBook")).thenReturn("");

        String result = service.getIsbn("EmptyBook");

        assertEquals("", result);
        verify(bookIsbnAPI).getIsbn("EmptyBook");
    }

    @Test
    @DisplayName("Handles exception thrown by BookIsbnAPI gracefully")
    void handlesExceptionFromApiGracefully() {
        when(bookIsbnAPI.getIsbn("BadBook"))
                .thenThrow(new RuntimeException("API failure"));

        assertThrows(RuntimeException.class, () -> service.getIsbn("BadBook"));

        verify(bookIsbnAPI, times(1)).getIsbn("BadBook");
    }

    @Test
    @DisplayName("Delegates call even with null title")
    void allowsNullTitleDelegation() {
        when(bookIsbnAPI.getIsbn(null)).thenReturn(null);

        String result = service.getIsbn(null);

        assertNull(result);
        verify(bookIsbnAPI).getIsbn(null);
    }

    @Test
    @DisplayName("Multiple calls are delegated properly and independently")
    void multipleCallsDelegatedIndependently() {
        when(bookIsbnAPI.getIsbn("Book1")).thenReturn("ISBN1");
        when(bookIsbnAPI.getIsbn("Book2")).thenReturn("ISBN2");

        String res1 = service.getIsbn("Book1");
        String res2 = service.getIsbn("Book2");

        assertEquals("ISBN1", res1);
        assertEquals("ISBN2", res2);
        verify(bookIsbnAPI, times(1)).getIsbn("Book1");
        verify(bookIsbnAPI, times(1)).getIsbn("Book2");
    }

    @Test
    @DisplayName("BookIsbnAPI mock interaction count is correct")
    void verifiesNoExtraInteractions() {
        when(bookIsbnAPI.getIsbn("BookX")).thenReturn("999");
        service.getIsbn("BookX");

        verify(bookIsbnAPI, times(1)).getIsbn("BookX");
        verifyNoMoreInteractions(bookIsbnAPI);
    }
}
