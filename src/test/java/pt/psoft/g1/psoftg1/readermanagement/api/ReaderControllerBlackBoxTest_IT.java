package pt.psoft.g1.psoftg1.readermanagement.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewMapper;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ReaderController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReaderControllerBlackBoxTest_IT {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ReaderService readerService;

    @MockBean
    private pt.psoft.g1.psoftg1.usermanagement.services.UserService userService;

    @MockBean
    private ReaderViewMapper readerViewMapper;

    @MockBean
    private LendingService lendingService;

    @MockBean
    private LendingViewMapper lendingViewMapper;

    @MockBean
    private ConcurrencyService concurrencyService;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private ApiNinjasService apiNinjasService;

    @Test
    @DisplayName("GET /api/readers returns list for librarian")
    void getData_asLibrarian_returnsList() throws Exception {
        Authentication auth = mock(Authentication.class);
        Librarian librarian = Librarian.newLibrarian("maria@gmail.com", "Mariaroberta!123", "Lib");

        when(userService.getAuthenticatedUser(auth)).thenReturn(librarian);

        ReaderDetails rd = mock(ReaderDetails.class);
        when(readerService.findAll()).thenReturn(List.of(rd));
        when(readerViewMapper.toReaderView(anyIterable())).thenReturn(List.of(new ReaderView()));

        mvc.perform(get("/api/readers").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /api/readers/{year}/{seq} returns quote view and ETag")
    void findByReaderNumber_returnsQuoteAndEtag() throws Exception {
        String year = "2000";
        String seq = "1";
        String readerNumber = year + "/" + seq;

        Authentication auth = mock(Authentication.class);
        User user = mock(User.class);
        when(userService.getAuthenticatedUser(auth)).thenReturn(user);

        ReaderDetails rd = mock(ReaderDetails.class);

        BirthDate bd = mock(BirthDate.class);
        when(bd.getBirthDate()).thenReturn(LocalDate.of(2000, 1, 1));
        when(rd.getBirthDate()).thenReturn(bd);

        when(readerService.findByReaderNumber(readerNumber)).thenReturn(Optional.of(rd));

        ReaderQuoteView quoteView = new ReaderQuoteView();
        quoteView.setReaderNumber(readerNumber);
        when(readerViewMapper.toReaderQuoteView(rd)).thenReturn(quoteView);
        when(apiNinjasService.getRandomEventFromYearMonth(any(Integer.class), any(Integer.class)))
                .thenReturn("An event");

        mvc.perform(get("/api/readers/{year}/{seq}", year, seq).principal(auth))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.readerNumber").value(readerNumber));
    }

    @Test
    @DisplayName("GET /api/readers/photo returns 404 when user has no photo")
    void getReaderOwnPhoto_notFoundWhenNoPhoto() throws Exception {
        Authentication auth = mock(Authentication.class);
        User u = mock(User.class);
        when(userService.getAuthenticatedUser(auth)).thenReturn(u);

        when(readerService.findByUsername(any(String.class))).thenReturn(Optional.empty());

        mvc.perform(get("/api/readers/photo").principal(auth))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/readers/{year}/{seq}/photo returns image when present")
    void getSpecificReaderPhoto_returnsImage() throws Exception {
        Authentication auth = mock(Authentication.class);
        Librarian lib = Librarian.newLibrarian("maria@gmail.com","Mariaroberta!123","L");
        when(userService.getAuthenticatedUser(auth)).thenReturn(lib);

        ReaderDetails rd = mock(ReaderDetails.class);
        pt.psoft.g1.psoftg1.shared.model.Photo photo = mock(pt.psoft.g1.psoftg1.shared.model.Photo.class);

        when(photo.getPhotoFile()).thenReturn("/tmp/p.png");
        when(rd.getPhoto()).thenReturn(photo);
        when(rd.getVersion()).thenReturn(0L);
        when(readerService.findByReaderNumber("2020/1")).thenReturn(Optional.of(rd));

        when(fileStorageService.getFile(photo.getPhotoFile())).thenReturn(new byte[]{1,2,3});
        when(fileStorageService.getExtension(photo.getPhotoFile())).thenReturn(Optional.of("png"));

        mvc.perform(get("/api/readers/{year}/{seq}/photo", "2020", "1").principal(auth))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
    }
}