package pt.psoft.g1.psoftg1.externalapimanagement.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import pt.psoft.g1.psoftg1.externalapimanagement.api.GoogleBooksResponse;
import pt.psoft.g1.psoftg1.externalapimanagement.model.BookIsbnAPI;

@Component
@Profile("googlebook")
public class GoogleIsbnImplement implements BookIsbnAPI {

    @Value("${api.url}")
    private String googleBooksUrl;

    @Value("${api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public GoogleIsbnImplement(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public String getIsbn(String title) {
        try {
            String url = UriComponentsBuilder.fromHttpUrl(googleBooksUrl)
                    .queryParam("q", "intitle:{title}")
                    .queryParam("key", apiKey)  
                    .buildAndExpand(title)
                    .toUriString();

            System.out.println("Google Books URL: " + url);

            GoogleBooksResponse response = restTemplate.getForObject(url, GoogleBooksResponse.class);

            if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
                return null;
            }

            for (GoogleBooksResponse.IndustryIdentifier id : response.getItems()
                    .get(0)
                    .getVolumeInfo()
                    .getIndustryIdentifiers()) {

                if ("ISBN_13".equals(id.getType())) return id.getIdentifier();
            }

            // fallback to ISBN_10
            for (GoogleBooksResponse.IndustryIdentifier id : response.getItems()
                    .get(0)
                    .getVolumeInfo()
                    .getIndustryIdentifiers()) {

                if ("ISBN_10".equals(id.getType())) return id.getIdentifier();
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error fetching ISBN from Google Books: " + e.getMessage());
            return null;
        }
    }
}
