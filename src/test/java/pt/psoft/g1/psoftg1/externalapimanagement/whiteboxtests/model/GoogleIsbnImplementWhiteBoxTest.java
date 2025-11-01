package pt.psoft.g1.psoftg1.externalapimanagement.whiteboxtests.model;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import pt.psoft.g1.psoftg1.externalapimanagement.model.GoogleIsbnImplement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;


class GoogleIsbnImplementWhiteBoxTest {
    private static final String GOOGLE_BOOKS_URL = "https://www.googleapis.com/books/v1/volumes";

    private void runIsbnScenario(GoogleIsbnImplement impl, String title, String json, String expectedIsbn) {
        MockRestServiceServer server = MockRestServiceServer.createServer(
                (RestTemplate) ReflectionTestUtils.getField(impl, "restTemplate"));
        server.expect(once(), requestTo(containsString("intitle:" + title.replace(" ", "%20"))))
                .andRespond(json != null ? withSuccess(json, MediaType.APPLICATION_JSON) : withServerError());

        String isbn = impl.getIsbn(title);
        assertThat(isbn, is(expectedIsbn));
        server.verify();
    }

    @Test
    void getIsbn_returnsIsbn13WhenPresent() {
        GoogleIsbnImplement impl = new GoogleIsbnImplement(new RestTemplate());
        ReflectionTestUtils.setField(impl, "googleBooksUrl", GOOGLE_BOOKS_URL);
        ReflectionTestUtils.setField(impl, "apiKey", "MY_KEY");

        String json = "{\"items\": [{\"volumeInfo\": {\"industryIdentifiers\": [{\"type\": \"ISBN_13\", \"identifier\": \"9781234567897\"}]}}]}";
        runIsbnScenario(impl, "Test Title", json, "9781234567897");
    }

    @Test
    void getIsbn_fallsBackToIsbn10WhenIsbn13Missing() {
        GoogleIsbnImplement impl = new GoogleIsbnImplement(new RestTemplate());
        ReflectionTestUtils.setField(impl, "googleBooksUrl", GOOGLE_BOOKS_URL);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        String json = "{\"items\": [{\"volumeInfo\": {\"industryIdentifiers\": [{\"type\": \"ISBN_10\", \"identifier\": \"1234567890\"}]}}]}";
        runIsbnScenario(impl, "Other", json, "1234567890");
    }

    @Test
    void getIsbn_returnsNullWhenNoItems() {
        GoogleIsbnImplement impl = new GoogleIsbnImplement(new RestTemplate());
        ReflectionTestUtils.setField(impl, "googleBooksUrl", GOOGLE_BOOKS_URL);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        String json = "{\"items\": []}";
        runIsbnScenario(impl, "NoItems", json, null);
    }

    @Test
    void getIsbn_returnsNullWhenServerError() {
        GoogleIsbnImplement impl = new GoogleIsbnImplement(new RestTemplate());
        ReflectionTestUtils.setField(impl, "googleBooksUrl", GOOGLE_BOOKS_URL);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        runIsbnScenario(impl, "Boom", null, null);
    }
}