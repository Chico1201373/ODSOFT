package pt.psoft.g1.psoftg1.readermanagement.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ReaderServiceImplIntegrationTest {

    @Autowired
    ReaderServiceImpl service;

    @MockBean
    ReaderRepository readerRepo;

    @MockBean
    UserRepository userRepo;

    @MockBean
    ReaderMapper readerMapper;

    @MockBean
    GenreRepository genreRepo;

    @MockBean
    ForbiddenNameRepository forbiddenNameRepository;

    @MockBean
    PhotoRepository photoRepository;

    @Test
    void create_integration_shouldSave_whenValid() {
        CreateReaderRequest req = new CreateReaderRequest();
        req.setUsername("integrationUser");
        req.setFullName("Integration Test");

        when(userRepo.findByUsername("integrationUser")).thenReturn(Optional.empty());
        when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
        when(readerRepo.getCountFromCurrentYear()).thenReturn(1);

        Reader mockReader = mock(Reader.class);
        ReaderDetails expected = mock(ReaderDetails.class);

        when(readerMapper.createReader(any())).thenReturn(mockReader);
        when(readerMapper.createReaderDetails(eq(2), eq(mockReader), eq(req), eq((String) null), isNull()))
                .thenReturn(expected);

        when(userRepo.save(mockReader)).thenReturn(mockReader);
        when(readerRepo.save(expected)).thenReturn(expected);

        var res = service.create(req, null);

        assertThat(res).isSameAs(expected);
        verify(userRepo).save(mockReader);
        verify(readerRepo).save(expected);
    }

    @Test
    void removeReaderPhoto_integration_shouldDeleteFile() {
        String rn = "2022/1";

        ReaderDetails rd = mock(ReaderDetails.class);
        var photo = mock(pt.psoft.g1.psoftg1.shared.model.Photo.class);
        when(photo.getPhotoFile()).thenReturn("/tmp/x.jpg");
        when(rd.getPhoto()).thenReturn(photo);
        when(readerRepo.findByReaderNumber(rn)).thenReturn(Optional.of(rd));
        when(readerRepo.save(rd)).thenReturn(rd);

        var res = service.removeReaderPhoto(rn, 1L);

        assertThat(res).isPresent();
        verify(photoRepository).deleteByPhotoFile("/tmp/x.jpg");
    }
}
