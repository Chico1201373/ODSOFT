package pt.psoft.g1.psoftg1.externalapimanagement.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

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
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(googleBooksUrl)
                    .queryParam("q", "intitle:" + title);

            /*if (apiKey != null && !apiKey.isEmpty()) {
                builder.queryParam("key", apiKey);
            } */
            

            String url = builder.toUriString();

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("items")) {
                return null;
            }

            List<Map<String, Object>> items = (List<Map<String, Object>>) response.get("items");
            if (items.isEmpty()) {
                return null;
            }

            Map<String, Object> volumeInfo = (Map<String, Object>) items.get(0).get("volumeInfo");
            if (volumeInfo == null || !volumeInfo.containsKey("industryIdentifiers")) {
                return null;
            }

            List<Map<String, String>> identifiers =
                    (List<Map<String, String>>) volumeInfo.get("industryIdentifiers");

            // Return the first available ISBN (prefer ISBN_13)
            for (Map<String, String> id : identifiers) {
                if ("ISBN_13".equals(id.get("type"))) {
                    return id.get("identifier");
                }
            }
            // fallback to ISBN_10
            for (Map<String, String> id : identifiers) {
                if ("ISBN_10".equals(id.get("type"))) {
                    return id.get("identifier");
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("Error fetching ISBN from Google Books: " + e.getMessage());
            return null;
        }
    }
}
