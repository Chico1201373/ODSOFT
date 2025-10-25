package pt.psoft.g1.psoftg1.externalapimanagement.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;


public class GoogleIsbnImplementTest {
    @Mock
    private RestTemplate restTemplate;

    private GoogleIsbnImplement google;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // construct with mocked RestTemplate
        google = new GoogleIsbnImplement(restTemplate);
        // set private fields via reflection (googleBooksUrl)
        try {
            java.lang.reflect.Field f = GoogleIsbnImplement.class.getDeclaredField("googleBooksUrl");
            f.setAccessible(true);
            f.set(google, "https://www.googleapis.com/books/v1/volumes");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void returnsIsbn13WhenPresent() {
        Map<String, Object> id1 = new HashMap<>();
        id1.put("type", "ISBN_10");
        id1.put("identifier", "1111111111");

        Map<String, Object> id2 = new HashMap<>();
        id2.put("type", "ISBN_13");
        id2.put("identifier", "2222222222222");

        Map<String, Object> volumeInfo = new HashMap<>();
        volumeInfo.put("industryIdentifiers", Arrays.asList(id1, id2));

        Map<String, Object> item = new HashMap<>();
        item.put("volumeInfo", volumeInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("items", Arrays.asList(item));

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        String isbn = google.getIsbn("Some Title");
        assertEquals("2222222222222", isbn);
    }

    @Test
    void fallsBackToIsbn10WhenIsbn13Missing() {
        Map<String, Object> id1 = new HashMap<>();
        id1.put("type", "ISBN_10");
        id1.put("identifier", "1111111111");

        Map<String, Object> volumeInfo = new HashMap<>();
        volumeInfo.put("industryIdentifiers", Arrays.asList(id1));

        Map<String, Object> item = new HashMap<>();
        item.put("volumeInfo", volumeInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("items", Arrays.asList(item));

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        String isbn = google.getIsbn("Another Title");
        assertEquals("1111111111", isbn);
    }

    @Test
    void returnsNullWhenNoItems() {
        Map<String, Object> response = new HashMap<>();
        response.put("items", Collections.emptyList());
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);

        assertNull(google.getIsbn("No Items Title"));
    }

    @Test
    void returnsNullWhenResponseNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(null);
        assertNull(google.getIsbn("Null Response Title"));
    }

    @Test
    void handlesExceptionsAndReturnsNull() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenThrow(new RuntimeException("boom"));
        assertNull(google.getIsbn("Explode Title"));
    }


    @Test
    @DisplayName("Returns null when industryIdentifiers list is empty")
    void returnsNullWhenIdentifiersEmpty() {
        Map<String, Object> volumeInfo = Map.of("industryIdentifiers", Collections.emptyList());
        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        assertNull(google.getIsbn("Empty Identifiers"));
    }

    @Test
    @DisplayName("Returns null when industryIdentifiers have unknown types")
    void returnsNullWhenUnknownTypes() {
        Map<String, Object> id = Map.of("type", "OTHER", "identifier", "9999999999");
        Map<String, Object> volumeInfo = Map.of("industryIdentifiers", List.of(id));
        Map<String, Object> item = Map.of("volumeInfo", volumeInfo);
        Map<String, Object> response = Map.of("items", List.of(item));

        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        assertNull(google.getIsbn("Weird Type"));
    }

    @Test
    @DisplayName("Handles missing 'items' key gracefully")
    void returnsNullWhenItemsKeyMissing() {
        Map<String, Object> response = new HashMap<>(); // no "items"
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        assertNull(google.getIsbn("No Items Key"));
    }

    @Test
    @DisplayName("Handles when items[0] is null or malformed")
    void returnsNullWhenFirstItemNull() {
        Map<String, Object> response = Map.of("items", Arrays.asList((Object) null));
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(response);
        assertNull(google.getIsbn("Null First Item"));
    }

    @Test
    @DisplayName("Returns null when RestTemplate throws IllegalArgumentException")
    void returnsNullWhenIllegalArgumentException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new IllegalArgumentException("Invalid URL"));
        assertNull(google.getIsbn("Bad URL Title"));
    }
}