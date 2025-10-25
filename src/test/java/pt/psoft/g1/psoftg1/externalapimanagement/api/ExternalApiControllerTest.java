package pt.psoft.g1.psoftg1.externalapimanagement.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@WebMvcTest(ExternalApiController.class)  
public class ExternalApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiService externalApiService;

    @MockBean
    private BookExternalMapper bookExternalMapper;

    @Test
    @WithMockUser(username = "testuser", roles = Role.LIBRARIAN)
    @DisplayName("GET /api/external/book/isbn/{title} should return BookExternalView when ISBN found")
    void receiveBooks_returnsBookExternalView_whenIsbnFound() throws Exception {
        String title = "The Hobbit";
        String isbn = "9780261103344";

        BookExternalView view = new BookExternalView();
        view.setTitle(title);
        view.setIsbn(isbn);

        when(externalApiService.getIsbn(title)).thenReturn(isbn);
        when(bookExternalMapper.toBookExternal(title, isbn)).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn/{title}", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(title)))
                .andExpect(jsonPath("$.isbn", is(isbn)));
    }

    @Test
    @WithMockUser(username = "testuser", roles = Role.LIBRARIAN)
    @DisplayName("GET /api/external/book/isbn/{title} should return null isbn when not found")
    void receiveBooks_returnsNullIsbn_whenIsbnNotFound() throws Exception {
        String title = "Unknown Book";
        String isbn = null;

        BookExternalView view = new BookExternalView();
        view.setTitle(title);
        view.setIsbn(null);

        when(externalApiService.getIsbn(title)).thenReturn(isbn);
        when(bookExternalMapper.toBookExternal(title, isbn)).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn/{title}", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(title)))
                .andExpect(jsonPath("$.isbn", nullValue()));
    }

    @Test
    @WithMockUser(username = "testuser", roles = Role.LIBRARIAN)
    @DisplayName("GET /api/external/book/isbn/{title} should return 400 for empty title")
    void receiveBooks_returnsBadRequest_whenTitleIsEmpty() throws Exception {
        mockMvc.perform(get("/api/external/book/isbn/{title}", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser", roles = Role.LIBRARIAN)
    @DisplayName("GET /api/external/book/isbn/{title} should handle ExternalApiService exception gracefully")
    void receiveBooks_handlesExternalApiException() throws Exception {
        String title = "ErrorBook";

        when(externalApiService.getIsbn(title))
                .thenThrow(new RuntimeException("External API error"));

        mockMvc.perform(get("/api/external/book/isbn/{title}", title))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("GET /api/external/book/isbn/{title} should return 401 when no user is authenticated")
    void receiveBooks_returnsUnauthorized_whenNoUser() throws Exception {
        mockMvc.perform(get("/api/external/book/isbn/{title}", "The Hobbit"))
                .andExpect(status().isUnauthorized());
    }

    /* 
    @Test
    @WithMockUser(username = "adminuser", roles = Role.ADMIN)
    @DisplayName("GET /api/external/book/isbn/{title} should return 403 when user is not a librarian or reader")
    void receiveBooks_returnsForbidden_whenUserNotLibrarian() throws Exception {
        mockMvc.perform(get("/api/external/book/isbn/{title}", "The Hobbit"))
                .andExpect(status().isForbidden());
    }
    */
    

    @Test
    @WithMockUser(username = "testuser", roles = Role.LIBRARIAN)
    @DisplayName("GET /api/external/book/isbn/{title} should be case-insensitive")
    void receiveBooks_returnsResult_whenTitleCaseDifferent() throws Exception {
        String title = "The Hobbit";
        String lowerCaseTitle = "the hobbit";
        String isbn = "9780261103344";

        BookExternalView view = new BookExternalView();
        view.setTitle(title);
        view.setIsbn(isbn);

        when(externalApiService.getIsbn(lowerCaseTitle)).thenReturn(isbn);
        when(bookExternalMapper.toBookExternal(lowerCaseTitle, isbn)).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn/{title}", lowerCaseTitle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is(title)))
                .andExpect(jsonPath("$.isbn", is(isbn)));
    }
    
}
