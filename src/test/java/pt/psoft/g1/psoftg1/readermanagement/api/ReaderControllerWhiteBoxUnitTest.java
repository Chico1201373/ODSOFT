package pt.psoft.g1.psoftg1.readermanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderView;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderQuoteView;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewMapper;

import pt.psoft.g1.psoftg1.readermanagement.model.BirthDate;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.external.service.ApiNinjasService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReaderControllerWhiteBoxUnitTest {

    @Mock ReaderService readerService;
    @Mock pt.psoft.g1.psoftg1.usermanagement.services.UserService userService;
    @Mock ReaderViewMapper readerViewMapper;
    @Mock pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService lendingService;
    @Mock pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewMapper lendingViewMapper;
    @Mock ConcurrencyService concurrencyService;
    @Mock FileStorageService fileStorageService;
    @Mock ApiNinjasService apiNinjasService;

    @InjectMocks ReaderController controller;

    @BeforeEach
    void beforeEach() {
    }

    @Test
    void getData_asReader_returnsReaderViewAndEtag() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var mockUser = mock(User.class);
        when(userService.getAuthenticatedUser(auth)).thenReturn(mockUser);
        when(mockUser.getUsername()).thenReturn("readerUser");

        ReaderDetails rd = mock(ReaderDetails.class);
        when(readerService.findByUsername("readerUser")).thenReturn(Optional.of(rd));

        ReaderView rv = new ReaderView();
        when(readerViewMapper.toReaderView(rd)).thenReturn(rv);

        var resp = controller.getData(auth);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo(rv);
    }

    @Test
    void getData_asLibrarian_returnsListBody() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        Librarian lib = Librarian.newLibrarian("lib@example.com", "Valid1Pass", "Lib");
        when(userService.getAuthenticatedUser(auth)).thenReturn(lib);

        ReaderDetails rd = mock(ReaderDetails.class);
        when(readerService.findAll()).thenReturn(java.util.List.of(rd));
        when(readerViewMapper.toReaderView(java.util.List.of(rd)))
                .thenReturn(java.util.List.of(new ReaderView()));

        var resp = controller.getData(auth);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isInstanceOf(java.util.List.class);
    }

    @Test
    void findByReaderNumber_returnsQuoteAndEtag() {
        String year = "1990";
        String seq = "7";
        String rn = year + "/" + seq;

        ReaderDetails rd = mock(ReaderDetails.class);
        BirthDate bd = mock(BirthDate.class);
        when(bd.getBirthDate()).thenReturn(java.time.LocalDate.of(1990, 1, 1));
        when(rd.getBirthDate()).thenReturn(bd);

        when(readerService.findByReaderNumber(rn)).thenReturn(Optional.of(rd));

        ReaderQuoteView quote = new ReaderQuoteView();
        when(readerViewMapper.toReaderQuoteView(rd)).thenReturn(quote);
        when(apiNinjasService.getRandomEventFromYearMonth(anyInt(), anyInt()))
                .thenReturn("an event");

        var resp = controller.findByReaderNumber(Integer.parseInt(year), Integer.parseInt(seq));

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo(quote);
        assertThat(resp.getHeaders().getETag()).isNotNull();
    }

    @Test
    void getSpecificReaderPhoto_returnsImageBytes_whenLibrarian() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        Librarian lib = Librarian.newLibrarian("lib@example.com", "Valid1Pass", "L");
        when(userService.getAuthenticatedUser(auth)).thenReturn(lib);

        ReaderDetails rd = mock(ReaderDetails.class);
        var photo = mock(pt.psoft.g1.psoftg1.shared.model.Photo.class);
        when(photo.getPhotoFile()).thenReturn("/tmp/f.png");
        when(rd.getPhoto()).thenReturn(photo);
        when(readerService.findByReaderNumber("2020/1")).thenReturn(Optional.of(rd));

        when(fileStorageService.getFile("/tmp/f.png")).thenReturn(new byte[]{1, 2, 3});
        when(fileStorageService.getExtension("/tmp/f.png")).thenReturn(Optional.of("png"));

        var resp = controller.getSpecificReaderPhoto(2020, 1, auth);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo(new byte[]{1, 2, 3});
    }

    @Test
    void getReaderOwnPhoto_throwsAccessDenied_whenNoReaderFound() {
        var auth = mock(org.springframework.security.core.Authentication.class);
        var user = mock(User.class);
        when(userService.getAuthenticatedUser(auth)).thenReturn(user);
        when(user.getUsername()).thenReturn("readerUser");
        when(readerService.findByUsername("readerUser")).thenReturn(Optional.empty());

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.getReaderOwnPhoto(auth));
    }
}