package pt.psoft.g1.psoftg1.authormanagement.whiteboxtests.model;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthorWhiteBoxTest {

    @Test
    void constructor_withThreeParameters_shouldCreateAuthorWithoutId() {
        Author author = new Author("John Doe", "Famous writer", "photo.jpg");

        assertThat(author.getName()).isEqualTo("John Doe");
        assertThat(author.getBio()).isEqualTo("Famous writer");
        assertThat(author.getPhoto()).isNotNull();
        assertThat(author.getAuthorNumber()).isNull();
    }

    @Test
    void constructor_withFourParameters_shouldCreateAuthorWithId() {
        Author author = new Author("auth-123", "Jane Smith", "Award-winning novelist", "jane.jpg");

        assertThat(author.getAuthorNumber()).isEqualTo("auth-123");
        assertThat(author.getName()).isEqualTo("Jane Smith");
        assertThat(author.getBio()).isEqualTo("Award-winning novelist");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void constructor_withNullPhotoURI_shouldCreateAuthorWithoutPhoto() {
        Author author = new Author("auth-456", "Bob Writer", "Short bio", null);

        assertThat(author.getAuthorNumber()).isEqualTo("auth-456");
        assertThat(author.getName()).isEqualTo("Bob Writer");
        assertThat(author.getBio()).isEqualTo("Short bio");
        assertThat(author.getPhoto()).isNull();
    }

    @Test
    void constructor_withInvalidPhotoURI_shouldSetPhotoToNull() {
        Author author = new Author("auth-789", "Alice Author", "Great writer", "\0invalid");

        assertThat(author.getAuthorNumber()).isEqualTo("auth-789");
        assertThat(author.getName()).isEqualTo("Alice Author");
        assertThat(author.getBio()).isEqualTo("Great writer");
        assertThat(author.getPhoto()).isNull();
    }

    @Test
    void setName_withValidName_shouldUpdateName() {
        Author author = new Author("Initial Name", "Bio", null);

        author.setName("Updated Name");

        assertThat(author.getName()).isEqualTo("Updated Name");
    }

    @Test
    void setName_withNull_shouldThrowException() {
        Author author = new Author("Initial Name", "Bio", null);

        assertThatThrownBy(() -> author.setName(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be null");
    }

    @Test
    void setName_withBlankString_shouldThrowException() {
        Author author = new Author("Initial Name", "Bio", null);

        assertThatThrownBy(() -> author.setName("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Name cannot be blank");
    }

    @Test
    void setBio_withValidBio_shouldUpdateBio() {
        Author author = new Author("Name", "Initial bio", null);

        author.setBio("Updated bio text");

        assertThat(author.getBio()).isEqualTo("Updated bio text");
    }

    @Test
    void setBio_withNull_shouldThrowException() {
        Author author = new Author("Name", "Initial bio", null);

        assertThatThrownBy(() -> author.setBio(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bio cannot be null");
    }

    @Test
    void setBio_withBlankString_shouldThrowException() {
        Author author = new Author("Name", "Initial bio", null);

        assertThatThrownBy(() -> author.setBio("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bio cannot be blank");
    }

    @Test
    void applyPatch_withVersionMismatch_shouldThrowStaleObjectStateException() {
        Author author = new Author("auth-001", "Original Name", "Original bio", null);
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName("New Name");

        assertThatThrownBy(() -> author.applyPatch(99L, request))
                .isInstanceOf(StaleObjectStateException.class)
                .hasMessageContaining("Object was already modified by another user");
    }

    @Test
    void applyPatch_withAllFieldsUpdated_shouldUpdateAllFields() {
        Author author = new Author("auth-002", "Original Name", "Original bio", "old-photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName("Updated Name");
        request.setBio("Updated bio");
        request.setPhotoURI("new-photo.jpg");

        author.applyPatch(0L, request);

        assertThat(author.getName()).isEqualTo("Updated Name");
        assertThat(author.getBio()).isEqualTo("Updated bio");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void applyPatch_withOnlyNameUpdated_shouldUpdateOnlyName() {
        Author author = new Author("auth-003", "Original Name", "Original bio", "photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName("Only Name Updated");
        request.setBio(null);
        request.setPhotoURI(null);

        author.applyPatch(0L, request);

        assertThat(author.getName()).isEqualTo("Only Name Updated");
        assertThat(author.getBio()).isEqualTo("Original bio");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void applyPatch_withOnlyBioUpdated_shouldUpdateOnlyBio() {
        Author author = new Author("auth-004", "Original Name", "Original bio", "photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName(null);
        request.setBio("Only Bio Updated");
        request.setPhotoURI(null);

        author.applyPatch(0L, request);

        assertThat(author.getName()).isEqualTo("Original Name");
        assertThat(author.getBio()).isEqualTo("Only Bio Updated");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void applyPatch_withOnlyPhotoURIUpdated_shouldUpdateOnlyPhoto() {
        Author author = new Author("auth-005", "Original Name", "Original bio", "old-photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName(null);
        request.setBio(null);
        request.setPhotoURI("new-photo.jpg");

        author.applyPatch(0L, request);

        assertThat(author.getName()).isEqualTo("Original Name");
        assertThat(author.getBio()).isEqualTo("Original bio");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void applyPatch_withAllNullFields_shouldNotUpdateAnything() {
        Author author = new Author("auth-006", "Original Name", "Original bio", "photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setName(null);
        request.setBio(null);
        request.setPhotoURI(null);

        author.applyPatch(0L, request);

        assertThat(author.getName()).isEqualTo("Original Name");
        assertThat(author.getBio()).isEqualTo("Original bio");
        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void applyPatch_withPhotoURISetToRemovePhoto_shouldRemovePhoto() {
        Author author = new Author("auth-007", "Name", "Bio", "existing-photo.jpg");
        UpdateAuthorRequest request = new UpdateAuthorRequest();
        request.setPhotoURI(null);

        author.applyPatch(0L, request);

        assertThat(author.getPhoto()).isNotNull();
    }

    @Test
    void removePhoto_withCorrectVersion_shouldRemovePhoto() {
        Author author = new Author("auth-008", "Name", "Bio", "photo.jpg");
        assertThat(author.getPhoto()).isNotNull();

        author.removePhoto(0L);

        assertThat(author.getPhoto()).isNull();
    }

    @Test
    void removePhoto_withIncorrectVersion_shouldThrowConflictException() {
        Author author = new Author("auth-009", "Name", "Bio", "photo.jpg");

        assertThatThrownBy(() -> author.removePhoto(99L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Provided version does not match latest version");
    }

    @Test
    void removePhoto_whenPhotoAlreadyNull_shouldSucceed() {
        Author author = new Author("auth-010", "Name", "Bio", null);
        assertThat(author.getPhoto()).isNull();

        author.removePhoto(0L);

        assertThat(author.getPhoto()).isNull();
    }

    @Test
    void getVersion_shouldReturnLongValue() {
        Author author = new Author("auth-011", "Name", "Bio", null);

        Long version = author.getVersion();

        assertThat(version).isNotNull();
        assertThat(version).isInstanceOf(Long.class);
        assertThat(version).isEqualTo(0L);
    }

    @Test
    void getName_shouldReturnStringRepresentation() {
        Author author = new Author("Test Author Name", "Bio", null);

        String name = author.getName();

        assertThat(name).isEqualTo("Test Author Name");
        assertThat(name).isInstanceOf(String.class);
    }

    @Test
    void getBio_shouldReturnStringRepresentation() {
        Author author = new Author("Name", "Test Author Bio", null);

        String bio = author.getBio();

        assertThat(bio).isEqualTo("Test Author Bio");
        assertThat(bio).isInstanceOf(String.class);
    }

    @Test
    void author_completeLifecycle_shouldWorkCorrectly() {
        Author author = new Author("auth-lifecycle", "Initial Name", "Initial bio", "initial-photo.jpg");
        assertThat(author.getAuthorNumber()).isEqualTo("auth-lifecycle");
        assertThat(author.getName()).isEqualTo("Initial Name");
        assertThat(author.getBio()).isEqualTo("Initial bio");
        assertThat(author.getPhoto()).isNotNull();

        UpdateAuthorRequest updateRequest = new UpdateAuthorRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setBio("Updated bio");
        author.applyPatch(0L, updateRequest);
        
        assertThat(author.getName()).isEqualTo("Updated Name");
        assertThat(author.getBio()).isEqualTo("Updated bio");

        author.removePhoto(0L);
        assertThat(author.getPhoto()).isNull();
    }

    @Test
    void setAuthorNumber_shouldUpdateAuthorNumber() {
        Author author = new Author("Name", "Bio", null);
        assertThat(author.getAuthorNumber()).isNull();

        author.setAuthorNumber("new-auth-id");

        assertThat(author.getAuthorNumber()).isEqualTo("new-auth-id");
    }

    @Test
    void setName_withAlphanumericSpecialCharacters_shouldWork() {
        Author author = new Author("Initial", "Bio", null);

        author.setName("John123");
        assertThat(author.getName()).isEqualTo("John123");
    }

    @Test
    void setBio_withLongValidBio_shouldWork() {
        Author author = new Author("Name", "Short", null);
        String longBio = "A".repeat(4000);

        author.setBio(longBio);

        assertThat(author.getBio()).hasSize(4000);
    }

    @Test
    void setBio_exceedingMaxLength_shouldThrowException() {
        Author author = new Author("Name", "Short", null);
        String tooLongBio = "A".repeat(4097);

        assertThatThrownBy(() -> author.setBio(tooLongBio))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bio has a maximum of 4096 characters");
    }
}
