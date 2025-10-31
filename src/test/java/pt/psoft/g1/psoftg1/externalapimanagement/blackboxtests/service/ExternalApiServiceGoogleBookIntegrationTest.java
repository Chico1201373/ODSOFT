package pt.psoft.g1.psoftg1.externalapimanagement.blackboxtests.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import pt.psoft.g1.psoftg1.externalapimanagement.model.GoogleIsbnImplement;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiServiceImpl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {ExternalApiServiceImpl.class, GoogleIsbnImplement.class, ExternalApiServiceGoogleBookIntegrationTest.TestConfig.class})
@ActiveProfiles("googlebook")
class ExternalApiServiceGoogleBookIntegrationTest {

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
    private ExternalApiService externalApiService;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    void getIsbn_delegatesToGoogleAndReturnsIsbn13() {
        String json = "{" +
                "\"items\": [ { \"volumeInfo\": { \"industryIdentifiers\": [ { \"type\": \"ISBN_13\", \"identifier\": \"9782222222222\" } ] } } ] }";

        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:Encoded%20Title")))
                .andRespond(withSuccess(json, org.springframework.http.MediaType.APPLICATION_JSON));

        String isbn = externalApiService.getIsbn("Encoded Title");

        assertThat(isbn, is("9782222222222"));
        server.verify();
    }

    @Test
    void getIsbn_returnsNullOnServerError() {
        server.expect(once(), requestTo(org.hamcrest.Matchers.containsString("intitle:Boom%20Title")))
                .andRespond(withServerError());

        String isbn = externalApiService.getIsbn("Boom Title");

        assertThat(isbn, is(nullValue()));
        server.verify();
    }
}
