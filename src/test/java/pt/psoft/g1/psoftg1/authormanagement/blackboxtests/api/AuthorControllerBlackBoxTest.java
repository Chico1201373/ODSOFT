package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorController;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewMapper;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthorControllerBlackBoxTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorService authorService;

    @MockBean
    private AuthorViewMapper authorViewMapper;

    @MockBean
    private ConcurrencyService concurrencyService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private BookViewMapper bookViewMapper;

    @Test
    public void testCreateAuthor_WithValidData_ReturnsCreated() throws Exception {
        var request = new CreateAuthorRequest();
        request.setName("John Doe");
        request.setBio("A simple bio");

        var author = new Author("A1", "John Doe", "A simple bio", null);

        var view = new AuthorView();
        view.setAuthorNumber("A1");
        view.setName("John Doe");
        view.setBio("A simple bio");

        when(authorService.create(any(CreateAuthorRequest.class))).thenReturn(author);
        when(authorViewMapper.toAuthorView(author)).thenReturn(view);

        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.authorNumber").value("A1"));
    }

    @Test
    public void testGetAuthorByNumber_ReturnsOk() throws Exception {
        var author = new Author("A2", "Jane", "Bio", null);
        var view = new AuthorView();
        view.setAuthorNumber("A2");
        view.setName("Jane");
        view.setBio("Bio");

        when(authorService.findByAuthorNumber("A2")).thenReturn(Optional.of(author));
        when(authorViewMapper.toAuthorView(author)).thenReturn(view);

        mockMvc.perform(get("/api/authors/A2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorNumber").value("A2"));
    }

    @Test
    public void testGetAuthorByNumber_NotFound() throws Exception {
        when(authorService.findByAuthorNumber("NOPE")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/authors/NOPE"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindByName_ReturnsList() throws Exception {
        var a1 = new Author("A3", "Alpha", "Bio A", null);
        var a2 = new Author("A4", "Alpha Two", "Bio B", null);

        var v1 = new AuthorView(); v1.setAuthorNumber("A3"); v1.setName("Alpha"); v1.setBio("Bio A");
        var v2 = new AuthorView(); v2.setAuthorNumber("A4"); v2.setName("Alpha Two"); v2.setBio("Bio B");

        when(authorService.findByName("Alpha")).thenReturn(List.of(a1, a2));
        when(authorViewMapper.toAuthorView(List.of(a1, a2))).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/api/authors").param("name", "Alpha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].authorNumber").value("A3"))
                .andExpect(jsonPath("$.items[1].authorNumber").value("A4"));
    }

    @Test
    public void testGetBooksByAuthorNumber_ReturnsList() throws Exception {
        var author = new Author("A5", "Writer", "Bio", null);
        when(authorService.findByAuthorNumber("A5")).thenReturn(Optional.of(author));

        var bookView = new BookView();
        bookView.setPk("1");
        bookView.setIsbn("ISBN1");
        bookView.setTitle("Title");
        bookView.setAuthors(List.of("Writer"));
        bookView.setGenre("Fiction");

    when(authorService.findBooksByAuthorNumber("A5")).thenReturn(List.of());
    when(bookViewMapper.toBookView(anyList())).thenReturn(List.of(bookView));

        mockMvc.perform(get("/api/authors/A5/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].isbn").value("ISBN1"));
    }

}
