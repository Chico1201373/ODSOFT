package pt.psoft.g1.psoftg1.externalapimanagement.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pt.psoft.g1.psoftg1.externalapimanagement.api.IsbnResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@Profile("openlibrary")
public class OpenLibraryImplement implements BookIsbnAPI {
    @Value("${api.url}")
    private String openLibraryUrl;

    private final RestTemplate restTemplate;

    public OpenLibraryImplement(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getIsbn(String title) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(openLibraryUrl)
                    .queryParam("q", "{title}")
                    .queryParam("fields", "isbn")
                    .buildAndExpand(title)
                    .toUriString();

            System.out.println("OpenLibrary URL: " + url);

            IsbnResponse response = restTemplate.getForObject(url, IsbnResponse.class);

            if (response != null
                    && response.getDocs() != null
                    && !response.getDocs().isEmpty()
                    && response.getDocs().get(0).getIsbn() != null
                    && !response.getDocs().get(0).getIsbn().isEmpty()) {
                return response.getDocs().get(0).getIsbn().get(0);
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error fetching ISBN from OpenLibrary: " + e.getMessage());
            return null;
        }
    }

}
