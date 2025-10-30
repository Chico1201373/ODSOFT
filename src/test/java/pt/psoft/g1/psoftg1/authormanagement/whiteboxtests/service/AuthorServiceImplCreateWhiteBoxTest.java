package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorServiceImpl;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;
import pt.psoft.g1.psoftg1.authormanagement.services.FactoryAuthor;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceImplCreateWhiteBoxTest {

    @Mock
    private AuthorRepository authorRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private FactoryAuthor factoryAuthor;

    @Mock
    private PhotoRepository photoRepository;

    @InjectMocks
    private AuthorServiceImpl authorService;

    private CreateAuthorRequest request;
    private Author expectedAuthor;

    @BeforeEach
    void setUp() {
        request = new CreateAuthorRequest();
        request.setName("John Doe");
        request.setBio("A talented author");
        
        expectedAuthor = new Author("test-id-123", "John Doe", "A talented author", null);
    }


    @Test
    void create_whenPhotoAndPhotoURIAreNull_shouldKeepBothNull() {
        request.setPhoto(null);
        request.setPhotoURI(null);

        given(factoryAuthor.generateAuthor(request)).willReturn(expectedAuthor);
        given(authorRepository.save(any(Author.class))).willReturn(expectedAuthor);

        Author result = authorService.create(request);

        assertThat(request.getPhoto()).isNull();
        assertThat(request.getPhotoURI()).isNull();

        verify(factoryAuthor).generateAuthor(request);
        
        ArgumentCaptor<Author> authorCaptor = ArgumentCaptor.forClass(Author.class);
        verify(authorRepository).save(authorCaptor.capture());
        
        assertThat(result).isEqualTo(expectedAuthor);
    }


    @Test
    void create_whenPhotoIsNullButPhotoURIIsNotNull_shouldSetBothToNull() {
        request.setPhoto(null);
        request.setPhotoURI("some-uri.jpg");

        given(factoryAuthor.generateAuthor(request)).willReturn(expectedAuthor);
        given(authorRepository.save(any(Author.class))).willReturn(expectedAuthor);

        Author result = authorService.create(request);

        assertThat(request.getPhoto()).isNull();
        assertThat(request.getPhotoURI()).isNull();

        ArgumentCaptor<CreateAuthorRequest> requestCaptor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(factoryAuthor).generateAuthor(requestCaptor.capture());
        
        CreateAuthorRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPhoto()).isNull();
        assertThat(capturedRequest.getPhotoURI()).isNull();
        
        verify(authorRepository).save(any(Author.class));
        
        assertThat(result).isEqualTo(expectedAuthor);
    }


    @Test
    void create_whenPhotoIsNotNullButPhotoURIIsNull_shouldSetBothToNull() {
        MultipartFile mockPhoto = mock(MultipartFile.class);
        request.setPhoto(mockPhoto);
        request.setPhotoURI(null);

        given(factoryAuthor.generateAuthor(request)).willReturn(expectedAuthor);
        given(authorRepository.save(any(Author.class))).willReturn(expectedAuthor);

        Author result = authorService.create(request);

        assertThat(request.getPhoto()).isNull();
        assertThat(request.getPhotoURI()).isNull();

        ArgumentCaptor<CreateAuthorRequest> requestCaptor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(factoryAuthor).generateAuthor(requestCaptor.capture());
        
        CreateAuthorRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPhoto()).isNull();
        assertThat(capturedRequest.getPhotoURI()).isNull();
        
        verify(authorRepository).save(any(Author.class));
        
        assertThat(result).isEqualTo(expectedAuthor);
    }


    @Test
    void create_whenBothPhotoAndPhotoURIAreNotNull_shouldKeepBothSet() {
        MultipartFile mockPhoto = mock(MultipartFile.class);
        String photoURI = "author-photo.jpg";
        request.setPhoto(mockPhoto);
        request.setPhotoURI(photoURI);

        Author authorWithPhoto = new Author("test-id-123", "John Doe", "A talented author", photoURI);
        given(factoryAuthor.generateAuthor(request)).willReturn(authorWithPhoto);
        given(authorRepository.save(any(Author.class))).willReturn(authorWithPhoto);

        Author result = authorService.create(request);

        assertThat(request.getPhoto()).isEqualTo(mockPhoto);
        assertThat(request.getPhotoURI()).isEqualTo(photoURI);

        ArgumentCaptor<CreateAuthorRequest> requestCaptor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(factoryAuthor).generateAuthor(requestCaptor.capture());
        
        CreateAuthorRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getPhoto()).isEqualTo(mockPhoto);
        assertThat(capturedRequest.getPhotoURI()).isEqualTo(photoURI);
        
        verify(authorRepository).save(any(Author.class));
        
        assertThat(result).isEqualTo(authorWithPhoto);
    }


    @Test
    void create_shouldCallFactoryAndRepositoryInCorrectOrder() {
        // Arrange
        request.setPhoto(null);
        request.setPhotoURI(null);

        given(factoryAuthor.generateAuthor(any(CreateAuthorRequest.class))).willReturn(expectedAuthor);
        given(authorRepository.save(any(Author.class))).willReturn(expectedAuthor);

        Author result = authorService.create(request);

        var inOrder = inOrder(factoryAuthor, authorRepository);
        inOrder.verify(factoryAuthor).generateAuthor(any(CreateAuthorRequest.class));
        inOrder.verify(authorRepository).save(any(Author.class));

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("John Doe");
        assertThat(result.getBio()).isEqualTo("A talented author");
    }

    
    @Test
    void create_shouldPreserveNameAndBioRegardlessOfPhotoState() {
        String expectedName = "Jane Smith";
        String expectedBio = "Award-winning novelist";
        
        request.setName(expectedName);
        request.setBio(expectedBio);
        request.setPhoto(null);
        request.setPhotoURI("invalid-uri");  

        Author author = new Author("test-id-456", expectedName, expectedBio, null);
        given(factoryAuthor.generateAuthor(any(CreateAuthorRequest.class))).willReturn(author);
        given(authorRepository.save(any(Author.class))).willReturn(author);

        Author result = authorService.create(request);

        ArgumentCaptor<CreateAuthorRequest> requestCaptor = ArgumentCaptor.forClass(CreateAuthorRequest.class);
        verify(factoryAuthor).generateAuthor(requestCaptor.capture());
        
        CreateAuthorRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getName()).isEqualTo(expectedName);
        assertThat(capturedRequest.getBio()).isEqualTo(expectedBio);
        
        assertThat(result.getName()).isEqualTo(expectedName);
        assertThat(result.getBio()).isEqualTo(expectedBio);
    }


    @Test
    void create_whenPhotoURIIsEmptyString_shouldTreatAsNotNull() {
        
        request.setPhoto(null);
        request.setPhotoURI("");  
        given(factoryAuthor.generateAuthor(any(CreateAuthorRequest.class))).willReturn(expectedAuthor);
        given(authorRepository.save(any(Author.class))).willReturn(expectedAuthor);

        authorService.create(request);

        assertThat(request.getPhoto()).isNull();
        assertThat(request.getPhotoURI()).isNull();
    }
}
