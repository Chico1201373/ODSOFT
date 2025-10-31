package pt.psoft.g1.psoftg1.externalapimanagement.blackboxtests.api;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalMapper;
import pt.psoft.g1.psoftg1.externalapimanagement.api.BookExternalView;
import pt.psoft.g1.psoftg1.externalapimanagement.api.ExternalApiController;
import pt.psoft.g1.psoftg1.externalapimanagement.service.ExternalApiService;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExternalApiController.class)
class ExternalApiControllerBlackBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExternalApiService bookExternalService;

    @MockBean
    private BookExternalMapper bookExternalMapper;

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void receiveBooks_returnsMappedIsbn() throws Exception {
        when(bookExternalService.getIsbn("Test Title")).thenReturn("9781234567897");
        BookExternalView view = new BookExternalView();
        view.setIsbn("9781234567897");
        when(bookExternalMapper.toBookExternal("9781234567897")).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn").param("title", "Test Title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9781234567897"));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void receiveBooks_whenServiceReturnsNull_returnsNullIsbn() throws Exception {
        when(bookExternalService.getIsbn("NoResult")).thenReturn(null);
        BookExternalView view = new BookExternalView();
        view.setIsbn(null);
        when(bookExternalMapper.toBookExternal(null)).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn").param("title", "NoResult"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value(Matchers.nullValue()));
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void receiveBooks_missingTitleParam_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/external/book/isbn"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiveBooks_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/external/book/isbn").param("title", "Test Title"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void receiveBooks_verifiesMapperInvocation() throws Exception {
        when(bookExternalService.getIsbn("VerifyTitle")).thenReturn("9780000000000");
        BookExternalView view = new BookExternalView();
        view.setIsbn("9780000000000");
        when(bookExternalMapper.toBookExternal("9780000000000")).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn").param("title", "VerifyTitle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9780000000000"));

        verify(bookExternalMapper, times(1)).toBookExternal("9780000000000");
    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void receiveBooks_titleWithSpecialCharacters_returnsOk() throws Exception {
        String title = "C++ Primer";
        when(bookExternalService.getIsbn(title)).thenReturn("9789999999999");
        BookExternalView view = new BookExternalView();
        view.setIsbn("9789999999999");
        when(bookExternalMapper.toBookExternal("9789999999999")).thenReturn(view);

        mockMvc.perform(get("/api/external/book/isbn").param("title", title))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isbn").value("9789999999999"));
    }
}
