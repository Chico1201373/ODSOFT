package pt.psoft.g1.psoftg1.readermanagement.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReaderServiceImplWhiteBoxUnitTest {

    @Mock
    ReaderRepository readerRepo;

    @Mock
    UserRepository userRepo;

    @Mock
    ReaderMapper readerMapper;

    @Mock
    GenreRepository genreRepo;

    @Mock
    ForbiddenNameRepository forbiddenNameRepository;

    @Mock
    PhotoRepository photoRepository;

    @InjectMocks
    ReaderServiceImpl service;

    @BeforeEach
    void setUp() {
        // default no-op
    }

    @Test
    void create_shouldThrowConflict_whenUsernameExists() {
        CreateReaderRequest req = new CreateReaderRequest();
        req.setUsername("existing");
        req.setFullName("John Doe");

        when(userRepo.findByUsername("existing")).thenReturn(Optional.of(mock(Reader.class)));

        assertThrows(ConflictException.class, () -> service.create(req, null));
    }

    @Test
    void create_shouldThrow_whenNameContainsForbiddenWord() {
        CreateReaderRequest req = new CreateReaderRequest();
        req.setUsername("newuser");
        req.setFullName("BadName Word");

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of(new ForbiddenName("forbidden")));

        assertThrows(IllegalArgumentException.class, () -> service.create(req, null));
    }

    @Test
    void create_shouldReturnSavedReaderDetails_whenValid() {
        CreateReaderRequest req = new CreateReaderRequest();
        req.setUsername("newuser");
        req.setFullName("Good Name");
        req.setInterestList(null);

        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
        when(readerRepo.getCountFromCurrentYear()).thenReturn(7);

        Reader mockReader = mock(Reader.class);
        ReaderDetails expectedDetails = mock(ReaderDetails.class);

        when(readerMapper.createReader(any())).thenReturn(mockReader);
        when(readerMapper.createReaderDetails(eq(8), eq(mockReader), eq(req), eq((String) null), isNull()))
                .thenReturn(expectedDetails);

        when(userRepo.save(mockReader)).thenReturn(mockReader);
        when(readerRepo.save(expectedDetails)).thenReturn(expectedDetails);

        var result = service.create(req, null);

        assertThat(result).isSameAs(expectedDetails);
        verify(userRepo).save(mockReader);
        verify(readerRepo).save(expectedDetails);
    }

    @Test
    void findTopByGenre_shouldThrow_whenStartAfterEnd() {
        LocalDate start = LocalDate.of(2023, 1, 2);
        LocalDate end = LocalDate.of(2022, 1, 1);

        assertThrows(IllegalArgumentException.class, () -> service.findTopByGenre("fiction", start, end));
    }

    @Test
    void findTopByGenre_shouldReturnContent_whenFound() {
        LocalDate start = LocalDate.of(2020, 1, 1);
        LocalDate end = LocalDate.of(2021, 1, 1);

        var dto = mock(ReaderBookCountDTO.class);
        when(readerRepo.findTopByGenre(any(), eq("fiction"), eq(start), eq(end)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        var res = service.findTopByGenre("fiction", start, end);

        assertThat(res).hasSize(1);
        assertThat(res.get(0)).isSameAs(dto);
    }

    @Test
    void update_shouldSaveAndReturnReaderDetails() {
        UpdateReaderRequest req = new UpdateReaderRequest();
        long desiredVersion = 1L;

        ReaderDetails existing = mock(ReaderDetails.class);
        when(readerRepo.findByUserId(anyLong())).thenReturn(Optional.of(existing));

        when(readerRepo.save(existing)).thenReturn(existing);

        var res = service.update(123L, req, desiredVersion, null);

        assertThat(res).isSameAs(existing);
        verify(readerRepo).save(existing);
    }

    @Test
    void removeReaderPhoto_shouldDeleteFileAndReturnUpdated() {
        String rn = "2020/1";
        long version = 2L;

        ReaderDetails rd = mock(ReaderDetails.class);
        var photo = mock(pt.psoft.g1.psoftg1.shared.model.Photo.class);
        when(photo.getPhotoFile()).thenReturn("/tmp/p.jpg");
        when(rd.getPhoto()).thenReturn(photo);
        when(readerRepo.findByReaderNumber(rn)).thenReturn(Optional.of(rd));

        when(readerRepo.save(rd)).thenReturn(rd);

        var res = service.removeReaderPhoto(rn, version);

        assertThat(res).isPresent();
        verify(photoRepository).deleteByPhotoFile("/tmp/p.jpg");
    }

    @Test
    void findTopReaders_shouldThrow_whenMinTopInvalid() {
        assertThrows(IllegalArgumentException.class, () -> service.findTopReaders(0));
    }

    @Test
    void findTopReaders_shouldReturnList_whenValid() {
        var rd = mock(ReaderDetails.class);
        when(readerRepo.findTopReaders(any())).thenReturn(new PageImpl<>(List.of(rd)));

        var res = service.findTopReaders(3);
        assertThat(res).hasSize(1);
    }

    @Test
    void searchReaders_shouldThrow_whenNoResults() {
        when(readerRepo.searchReaderDetails(any(), any())).thenReturn(List.of());
        assertThrows(NotFoundException.class, () -> service.searchReaders(null, null));
    }

    @Test
    void searchReaders_shouldReturnList_whenFound() {
        var rd = mock(ReaderDetails.class);
        when(readerRepo.searchReaderDetails(any(), any())).thenReturn(List.of(rd));

        var res = service.searchReaders(null, null);
        assertThat(res).hasSize(1);
    }
}
