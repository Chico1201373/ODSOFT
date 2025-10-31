package pt.psoft.g1.psoftg1.externalapimanagement.whiteboxtests.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.externalapimanagement.model.GoogleIsbnImplement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {GoogleIsbnImplement.class, GoogleIsbnImplementWhiteBoxTest.TestConfig.class})
@ActiveProfiles("googlebook")
class GoogleIsbnImplementWhiteBoxTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GoogleIsbnImplement impl;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getIsbn_returnsIsbn13WhenPresent() {
        String json = "{" +
                "\"items\": [ { \"volumeInfo\": { \"industryIdentifiers\": [ { \"type\": \"ISBN_13\", \"identifier\": \"9781234567897\" } ] } } ] }";

        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:Test%20Title")))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        String isbn = impl.getIsbn("Test Title");

        assertThat(isbn, is("9781234567897"));
        server.verify();
    }

    @Test
    void getIsbn_fallsBackToIsbn10WhenIsbn13Missing() {
        String json = "{" +
                "\"items\": [ { \"volumeInfo\": { \"industryIdentifiers\": [ { \"type\": \"ISBN_10\", \"identifier\": \"1234567890\" } ] } } ] }";

        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:Other%20Title")))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        String isbn = impl.getIsbn("Other Title");

        assertThat(isbn, is("1234567890"));
        server.verify();
    }

    @Test
    void getIsbn_returnsNullWhenNoItemsOrNulls() {
        String json = "{ \"items\": [] }";

        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:NoResponse")))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        String isbn = impl.getIsbn("NoResponse");
        assertThat(isbn, is(nullValue()));
        server.verify();
    }

    @Test
    void getIsbn_handlesExceptionAndReturnsNull() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:Boom")))
                .andRespond(withServerError());

        String isbn = impl.getIsbn("Boom");
        assertThat(isbn, is(nullValue()));
        server.verify();
    }
}
