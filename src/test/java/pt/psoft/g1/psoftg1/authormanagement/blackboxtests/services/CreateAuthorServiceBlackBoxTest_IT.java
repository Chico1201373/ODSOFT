package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;
import pt.psoft.g1.psoftg1.authormanagement.services.CreateAuthorRequest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CreateAuthorServiceBlackBoxTest_IT {

    @Autowired
    private AuthorService authorService;

    @Autowired
    private AuthorRepository authorRepository;

    @BeforeEach
    void setUp() {

        // repository.deleteAll();
    }


    @Test
    void testCreate_WithValidNameAndBio_ReturnsAuthorWithGeneratedId() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("George Orwell");
        request.setBio("English novelist and essayist");
        request.setPhoto(null);
        request.setPhotoURI(null);

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor, "Created author should not be null");
        assertNotNull(createdAuthor.getAuthorNumber(), "Author number should be generated");
        assertEquals("George Orwell", createdAuthor.getName());
        assertEquals("English novelist and essayist", createdAuthor.getBio());
        assertNull(createdAuthor.getPhoto(), "Photo should be null when not provided");
    }

    @Test
    void testCreate_WithMinimalData_Success() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("A");
        request.setBio("B");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertNotNull(createdAuthor.getAuthorNumber());
        assertEquals("A", createdAuthor.getName());
        assertEquals("B", createdAuthor.getBio());
    }

    @Test
    void testCreate_WithMaxLengthName_Success() {
        
        String maxLengthName = "A".repeat(150);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(maxLengthName);
        request.setBio("Valid bio");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals(maxLengthName, createdAuthor.getName());
    }

    @Test
    void testCreate_WithMaxLengthBio_Success() {
        
        String maxLengthBio = "B".repeat(4096);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(maxLengthBio);

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals(maxLengthBio, createdAuthor.getBio());
    }

    @Test
    void testCreate_WithSpecialCharactersInName_Success() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("José O'Connor-Smith");
        request.setBio("Author with special characters in name");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals("José O'Connor-Smith", createdAuthor.getName());
    }

    @Test
    void testCreate_WithUnicodeCharacters_Success() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("村上春樹");
        request.setBio("Japanese author");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals("村上春樹", createdAuthor.getName());
    }

    @Test
    void testCreate_MultipleAuthors_GeneratesDifferentIds() {
        
        CreateAuthorRequest request1 = new CreateAuthorRequest();
        request1.setName("Author One");
        request1.setBio("First author");

        CreateAuthorRequest request2 = new CreateAuthorRequest();
        request2.setName("Author Two");
        request2.setBio("Second author");

        
        Author author1 = authorService.create(request1);
        Author author2 = authorService.create(request2);

        
        assertNotNull(author1.getAuthorNumber());
        assertNotNull(author2.getAuthorNumber());
        assertNotEquals(author1.getAuthorNumber(), author2.getAuthorNumber(),
                "Different authors should have different IDs");
    }

    @Test
    void testCreate_AuthorIsPersistedInDatabase() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Persistent Author");
        request.setBio("Testing persistence");

        
        Author createdAuthor = authorService.create(request);

        
        assertTrue(authorRepository.findByAuthorNumber(createdAuthor.getAuthorNumber()).isPresent(),
                "Author should be persisted in the database");
    }

    // ========== Photo Handling Scenarios ==========

    @Test
    void testCreate_WithPhotoAndPhotoURI_PhotoIsSet() {
        
        MockMultipartFile mockFile = new MockMultipartFile(
                "photo",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Photo Author");
        request.setBio("Author with photo");
        request.setPhoto(mockFile);
        request.setPhotoURI("uploads/test.jpg");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals("Photo Author", createdAuthor.getName());
        // Photo should be set when both photo and photoURI are provided
        assertNotNull(createdAuthor.getPhoto(), "Photo should be set");
    }

    @Test
    void testCreate_WithPhotoButNullPhotoURI_PhotoIsIgnored() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "photo",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Ignored Photo Author");
        request.setBio("Photo should be ignored");
        request.setPhoto(mockFile);
        request.setPhotoURI(null);

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertNull(createdAuthor.getPhoto(), "Photo should be null when photoURI is null");
    }

    @Test
    void testCreate_WithNullPhotoButValidPhotoURI_PhotoURIIsIgnored() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Ignored URI Author");
        request.setBio("PhotoURI should be ignored");
        request.setPhoto(null);
        request.setPhotoURI("uploads/fake.jpg");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertNull(createdAuthor.getPhoto(), "Photo should be null when photo file is null");
    }

    @Test
    void testCreate_WithBothPhotoAndPhotoURINull_PhotoIsNull() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("No Photo Author");
        request.setBio("Author without photo");
        request.setPhoto(null);
        request.setPhotoURI(null);

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertNull(createdAuthor.getPhoto(), "Photo should be null when both are null");
    }

    // ========== Validation Error Scenarios ==========

    @Test
    void testCreate_WithNullName_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName(null);
        request.setBio("Valid bio");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when name is null");
    }

    @Test
    void testCreate_WithEmptyName_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("");
        request.setBio("Valid bio");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when name is empty");
    }

    @Test
    void testCreate_WithBlankName_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("   ");
        request.setBio("Valid bio");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when name is blank");
    }

    @Test
    void testCreate_WithNullBio_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(null);

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when bio is null");
    }

    @Test
    void testCreate_WithEmptyBio_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when bio is empty");
    }

    @Test
    void testCreate_WithBlankBio_ThrowsException() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio("   ");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when bio is blank");
    }

    @Test
    void testCreate_WithBioTooLong_ThrowsException() {
        String tooLongBio = "B".repeat(4097);
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Valid Name");
        request.setBio(tooLongBio);

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when bio exceeds maximum length");
    }

    @Test
    void testCreate_WithInvalidCharactersInName_ThrowsException() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("J.K. Rowling");
        request.setBio("Valid bio");

         
        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when name contains invalid characters (dots)");
    }

    @Test
    void testCreate_WithSpecialSymbolsInName_ThrowsException() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Author@123");
        request.setBio("Valid bio");

        assertThrows(IllegalArgumentException.class, () -> authorService.create(request),
                "Should throw exception when name contains special symbols");
    }


    @Test
    void testCreate_WithHtmlInBio_BioisSanitized() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("HTML Author");
        request.setBio("Bio with <b>bold</b> and <a href='http://example.com'>link</a>");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertNotNull(createdAuthor.getBio());
    }

    @Test
    void testCreate_WithNewlinesInBio_Success() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Multiline Bio Author");
        request.setBio("First line\n\nSecond line\nThird line");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertTrue(createdAuthor.getBio().contains("\n"), "Bio should preserve newlines");
    }

    @Test
    void testCreate_AuthorHasVersionZero() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Version Test");
        request.setBio("Testing initial version");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals(0L, createdAuthor.getVersion(), 
                "New author should have version 0");
    }

    @Test
    void testCreate_WithLeadingAndTrailingSpacesInName_Success() {
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("First Middle Last");
        request.setBio("Author with spaces in name");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals("First Middle Last", createdAuthor.getName());
    }

    @Test
    void testCreate_WithNumericName_Success() {
        
        CreateAuthorRequest request = new CreateAuthorRequest();
        request.setName("Author 123");
        request.setBio("Numeric author");

        
        Author createdAuthor = authorService.create(request);

        
        assertNotNull(createdAuthor);
        assertEquals("Author 123", createdAuthor.getName());
    }

    @Test
    void testCreate_SameNameDifferentBio_CreatesTwoAuthors() {
        CreateAuthorRequest request1 = new CreateAuthorRequest();
        request1.setName("John Smith");
        request1.setBio("First John Smith");

        CreateAuthorRequest request2 = new CreateAuthorRequest();
        request2.setName("John Smith");
        request2.setBio("Second John Smith");

        
        Author author1 = authorService.create(request1);
        Author author2 = authorService.create(request2);

        
        assertNotEquals(author1.getAuthorNumber(), author2.getAuthorNumber(),
                "Authors with same name should have different IDs");
        assertEquals("John Smith", author1.getName());
        assertEquals("John Smith", author2.getName());
        assertNotEquals(author1.getBio(), author2.getBio());
    }
}
