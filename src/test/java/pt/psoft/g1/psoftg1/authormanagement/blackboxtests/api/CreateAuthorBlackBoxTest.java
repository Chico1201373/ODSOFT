package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorView;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.testutils.JsonHelper;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class CreateAuthorBlackBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/authors";

    @BeforeEach
    void setUp() {
        
    }

    

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithValidData_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("JK Rowling");
        request.setBio("British author, best known for the Harry Potter series.");

        
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.authorNumber").exists())
                .andExpect(jsonPath("$.name").value("JK Rowling"))
                .andExpect(jsonPath("$.bio").value("British author, best known for the Harry Potter series."))
                .andReturn();

        
        AuthorView createdAuthor = JsonHelper.fromJson(objectMapper,
                result.getResponse().getContentAsString(), AuthorView.class);
        assertNotNull(createdAuthor.getAuthorNumber(), "Author number should not be null");
        assertEquals("JK Rowling", createdAuthor.getName());
        assertEquals("British author, best known for the Harry Potter series.", createdAuthor.getBio());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithMinimalValidData_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("A"); 
        request.setBio("B"); 

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.authorNumber").exists())
                .andExpect(jsonPath("$.name").value("A"))
                .andExpect(jsonPath("$.bio").value("B"));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithMaxLengthName_ReturnsCreated() throws Exception {
        
        String maxLengthName = "A".repeat(150);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(maxLengthName);
        request.setBio("A valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(maxLengthName));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithMaxLengthBio_ReturnsCreated() throws Exception {
        
        String maxLengthBio = "B".repeat(4096);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(maxLengthBio);

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bio").value(maxLengthBio));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithSpecialCharactersInName_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("José Saramago O'Connor-Smith");
        request.setBio("Portuguese Nobel Prize winner");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("José Saramago O'Connor-Smith"));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithUnicodeCharacters_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("村上春樹"); 
        request.setBio("Japanese writer known for surrealist fiction");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("村上春樹"));
    }

    

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithNullName_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(null);
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithEmptyName_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithNameTooLong_ReturnsBadRequest() throws Exception {
        
        String tooLongName = "A".repeat(151);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(tooLongName);
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithNullBio_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(null);

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithEmptyBio_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithBioTooLong_ReturnsBadRequest() throws Exception {
        
        String tooLongBio = "B".repeat(4097);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(tooLongBio);

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithBothFieldsNull_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(null);
        request.setBio(null);

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithBothFieldsEmpty_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("");
        request.setBio("");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithWhitespaceOnlyName_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("   ");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithWhitespaceOnlyBio_ReturnsBadRequest() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("   ");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isBadRequest());
    }

    

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithEmptyBody_ReturnsBadRequest() throws Exception {
        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithMalformedJson_ReturnsBadRequest() throws Exception {
        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithoutContentType_ReturnsUnsupportedMediaType() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithWrongContentType_ReturnsUnsupportedMediaType() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_XML)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithExtraFields_IgnoresAndReturnsCreated() throws Exception {
        
        String jsonWithExtraFields = """
                {
                    "name": "George Orwell",
                    "bio": "English novelist and essayist",
                    "extraField1": "should be ignored",
                    "extraField2": 12345
                }
                """;

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithExtraFields))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("George Orwell"))
                .andExpect(jsonPath("$.bio").value("English novelist and essayist"))
                .andExpect(jsonPath("$.extraField1").doesNotExist())
                .andExpect(jsonPath("$.extraField2").doesNotExist());
    }

    

    @Test
    void testCreateAuthor_WithoutAuthentication_ReturnsUnauthorized() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "reader", roles = {"READER"})
    void testCreateAuthor_WithInsufficientRole_ReturnsForbidden() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("Valid bio");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isForbidden());
    }

    

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithNameContainingNumbers_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Author 123");
        request.setBio("Bio with numbers 456");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Author 123"));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_WithBioContainingNewlines_ReturnsCreated() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Multi Line Bio Author");
        request.setBio("First paragraph.\n\nSecond paragraph with details.");

        
        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bio").value("First paragraph.\n\nSecond paragraph with details."));
    }


    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_MultipleCreations_GeneratesDifferentAuthorNumbers() throws Exception {
        
        CreateAuthorRequest request1 = new CreateAuthorRequest();
        request1.setName("First Author");
        request1.setBio("First bio");

        CreateAuthorRequest request2 = new CreateAuthorRequest();
        request2.setName("Second Author");
        request2.setBio("Second bio");

        
        MvcResult result1 = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request1)))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult result2 = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request2)))
                .andExpect(status().isCreated())
                .andReturn();

        
        AuthorView author1 = JsonHelper.fromJson(objectMapper,
                result1.getResponse().getContentAsString(), AuthorView.class);
        AuthorView author2 = JsonHelper.fromJson(objectMapper,
                result2.getResponse().getContentAsString(), AuthorView.class);

        assertNotEquals(author1.getAuthorNumber(), author2.getAuthorNumber(),
                "Different authors should have different author numbers");
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_ResponseContainsETag() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("ETag Test Author");
        request.setBio("Testing ETag header");

        
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andReturn();

        String eTag = result.getResponse().getHeader(HttpHeaders.ETAG);
        assertNotNull(eTag, "ETag header should be present");
        assertFalse(eTag.isEmpty(), "ETag should not be empty");
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void testCreateAuthor_ResponseContainsLocationHeader() throws Exception {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Location Header Test");
        request.setBio("Testing Location header");

        
        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonHelper.toJson(objectMapper, request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertNotNull(location, "Location header should be present");
        assertTrue(location.contains("/api/authors"), "Location should contain the authors endpoint");
    }
}
