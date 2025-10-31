package pt.psoft.g1.psoftg1.externalapimanagement.blackboxtests.model;

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

class GoogleIsbnImplementBlackBoxTest {

    private final String googleBooksUrl = "https://www.googleapis.com/books/v1/volumes";

    @Test
    void getIsbn_returnsIsbn13WhenPresent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String json = "{" +
                "\"items\": [ { \"volumeInfo\": { \"industryIdentifiers\": [ { \"type\": \"ISBN_13\", \"identifier\": \"9781234567897\" } ] } } ] }";

        server.expect(once(), requestTo(containsString("intitle:Test%20Title")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleIsbnImplement impl = new GoogleIsbnImplement(restTemplate);
        ReflectionTestUtils.setField(impl, "googleBooksUrl", googleBooksUrl);
        ReflectionTestUtils.setField(impl, "apiKey", "MY_KEY");

        String isbn = impl.getIsbn("Test Title");

        assertThat(isbn, is("9781234567897"));
        server.verify();
    }

    @Test
    void getIsbn_fallsBackToIsbn10WhenIsbn13Missing() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String json = "{" +
                "\"items\": [ { \"volumeInfo\": { \"industryIdentifiers\": [ { \"type\": \"ISBN_10\", \"identifier\": \"1234567890\" } ] } } ] }";

        server.expect(once(), requestTo(containsString("intitle:Other")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleIsbnImplement impl = new GoogleIsbnImplement(restTemplate);
        ReflectionTestUtils.setField(impl, "googleBooksUrl", googleBooksUrl);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        String isbn = impl.getIsbn("Other");

        assertThat(isbn, is("1234567890"));
        server.verify();
    }

    @Test
    void getIsbn_returnsNullWhenNoItems() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        String json = "{ \"items\": [] }";

        server.expect(once(), requestTo(containsString("intitle:NoItems")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

        GoogleIsbnImplement impl = new GoogleIsbnImplement(restTemplate);
        ReflectionTestUtils.setField(impl, "googleBooksUrl", googleBooksUrl);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        String isbn = impl.getIsbn("NoItems");

        assertThat(isbn, is(nullValue()));
        server.verify();
    }

    @Test
    void getIsbn_returnsNullWhenServerError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.createServer(restTemplate);

        server.expect(once(), requestTo(containsString("intitle:Boom")))
                .andRespond(withServerError());

        GoogleIsbnImplement impl = new GoogleIsbnImplement(restTemplate);
        ReflectionTestUtils.setField(impl, "googleBooksUrl", googleBooksUrl);
        ReflectionTestUtils.setField(impl, "apiKey", "");

        String isbn = impl.getIsbn("Boom");

        assertThat(isbn, is(nullValue()));
        server.verify();
    }
}
