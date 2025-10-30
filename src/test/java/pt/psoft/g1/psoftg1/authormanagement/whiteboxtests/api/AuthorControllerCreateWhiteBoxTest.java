package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.api;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorController;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorView;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewMapper;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = AuthorController.class)
@AutoConfigureMockMvc
class AuthorControllerCreateWhiteBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private AuthorService authorService;
    @MockBean private AuthorViewMapper authorViewMapper;
    @MockBean private ConcurrencyService concurrencyService;
    @MockBean private FileStorageService fileStorageService;
    @MockBean private BookViewMapper bookViewMapper;

    @Test
    void create_shouldOverrideClientPhotoUri_callStorage_andService_andReturn201() throws Exception {
        
        String requestJson = "{\n" +
                "  \"name\": \"John Smith\",\n" +
                "  \"bio\": \"Short bio\"\n" +
                "}";


        Author domain = new Author("ts-ffffff", "John Smith", "Short bio");

        given(authorService.create(any(CreateAuthorRequest.class))).willReturn(domain);

        AuthorView view = new AuthorView();
        view.setAuthorNumber("ts-ffffff");
        view.setName("John Smith");
        view.setBio("Short bio");
        given(authorViewMapper.toAuthorView(any(Author.class))).willReturn(view);

        
        mockMvc.perform(post("/api/authors")
                        .with(user("lib").roles(Role.LIBRARIAN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))

                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Location", "http://localhost/api/authors"))
                .andExpect(jsonPath("$.authorNumber").value("ts-ffffff"))
                
                .andExpect(jsonPath("$.name").value("John Smith"))
                .andExpect(jsonPath("$.bio").value("Short bio"))
                .andReturn();

        ArgumentCaptor<CreateAuthorRequest> captor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(captor.capture());
        CreateAuthorRequest passed = captor.getValue();


        assertThat(passed.getName()).isEqualTo("John Smith");
        assertThat(passed.getBio()).isEqualTo("Short bio");

    }

    @Test
    @WithMockUser(username = "lib", roles = Role.LIBRARIAN)
    void create_whenStorageReturnsNull_shouldForwardNullPhotoUri() throws Exception {
        String requestJson = "{\n" +
                "  \"name\": \"Alice Johnson\",\n" +
                "  \"bio\": \"Bio\"\n" +
                "}";

        given(fileStorageService.getRequestPhoto(null)).willReturn(null);

        Author domain = new Author("ts-123abc", "Alice Johnson", "Bio", null);
        given(authorService.create(any(CreateAuthorRequest.class))).willReturn(domain);

        AuthorView view = new AuthorView();
        view.setAuthorNumber("ts-123abc");
        view.setName("Alice Johnson");
        view.setBio("Bio");
        given(authorViewMapper.toAuthorView(any(Author.class))).willReturn(view);

        
        mockMvc.perform(post("/api/authors")
                        .with(user("lib").roles(Role.LIBRARIAN))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(header().string("Location", "http://localhost/api/authors"))
                .andExpect(jsonPath("$.authorNumber").value("ts-123abc"))
                .andExpect(jsonPath("$.name").value("Alice Johnson"))
                .andExpect(jsonPath("$.bio").value("Bio"));

        ArgumentCaptor<CreateAuthorRequest> captor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(authorService).create(captor.capture());
        CreateAuthorRequest passed = captor.getValue();
        assertThat(passed.getPhotoURI()).isNull();
    }
}
