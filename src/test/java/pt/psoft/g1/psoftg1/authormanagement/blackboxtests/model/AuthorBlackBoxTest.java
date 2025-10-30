package pt.psoft.g1.psoftg1.authormanagement.blackboxtests.model;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import static org.junit.jupiter.api.Assertions.*;

class AuthorBlackBoxTest {

    @Test
    void construct_WithValidFields_SetsValues() {
        Author a = new Author("Valid Name", "Valid bio", null);

        assertEquals("Valid Name", a.getName());
        assertEquals("Valid bio", a.getBio());
        assertNull(a.getPhoto(), "Photo should be null when photoURI is null");
        assertEquals(0L, a.getVersion(), "New authors should start at version 0");
    }

    @Test
    void construct_WithIdAndPhoto_SetsPhoto() {
        Author a = new Author("2025-abc123", "Ana Maria", "Some bio", "uploads/pic.png");

        assertNotNull(a.getPhoto(), "Photo must be set when a valid path is provided");
        assertTrue(a.getPhoto().getPhotoFile().contains("uploads/pic.png"));
        assertEquals("Ana Maria", a.getName());
        assertEquals("Some bio", a.getBio());
    }

    @Test
    void construct_WithInvalidNameCharacters_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new Author("J.K. Rowling", "Bio", null));
    }

    @Test
    void construct_WithBlankName_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new Author("   ", "Bio", null));
    }

    @Test
    void construct_WithAllowedNameCharacters_Succeeds() {
        Author a = new Author("Ana O'Neill-Silva", "Bio", null);
        assertEquals("Ana O'Neill-Silva", a.getName());
    }

    @Test
    void construct_WithNullBio_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new Author("Valid Name", null, null));
    }

    @Test
    void construct_WithBlankBio_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new Author("Valid Name", "   ", null));
    }

    @Test
    void construct_WithTooLongBio_Throws() {
        String tooLong = "B".repeat(4097);
        assertThrows(IllegalArgumentException.class, () -> new Author("Valid Name", tooLong, null));
    }

    @Test
    void applyPatch_WithCorrectVersion_UpdatesNonNullFields() {
        Author a = new Author("Old Name", "Old Bio", "uploads/old.png");
        assertNotNull(a.getPhoto());

        long desiredVersion = a.getVersion();

        UpdateAuthorRequest patch = new UpdateAuthorRequest();
        patch.setName("New Name");
        patch.setBio("New Bio");

        a.applyPatch(desiredVersion, patch);

        assertEquals("New Name", a.getName());
        assertEquals("New Bio", a.getBio());
        assertNotNull(a.getPhoto(), "Photo should remain unchanged when photoURI is null");
    }

    @Test
    void applyPatch_WithPhotoUri_SetsPhoto() {
        Author a = new Author("Name", "Bio", null);
        assertNull(a.getPhoto());

        long desiredVersion = a.getVersion();

        UpdateAuthorRequest patch = new UpdateAuthorRequest();
        patch.setPhotoURI("uploads/new.jpg");

        a.applyPatch(desiredVersion, patch);

        assertNotNull(a.getPhoto());
        assertTrue(a.getPhoto().getPhotoFile().contains("uploads/new.jpg"));
    }

    @Test
    void applyPatch_WithWrongVersion_ThrowsStaleObject() {
        Author a = new Author("Name", "Bio", null);

        UpdateAuthorRequest patch = new UpdateAuthorRequest();
        patch.setName("Other");

        assertThrows(StaleObjectStateException.class, () -> a.applyPatch(a.getVersion() + 1, patch));
    }

    @Test
    void removePhoto_WithCorrectVersion_RemovesPhoto() {
        Author a = new Author("Name", "Bio", "uploads/pic.jpg");
        assertNotNull(a.getPhoto());

        a.removePhoto(a.getVersion());
        assertNull(a.getPhoto(), "Photo should be removed when version matches");
    }

    @Test
    void removePhoto_WithWrongVersion_ThrowsConflict() {
        Author a = new Author("Name", "Bio", "uploads/pic.jpg");
        assertNotNull(a.getPhoto());

        assertThrows(ConflictException.class, () -> a.removePhoto(a.getVersion() + 1));
    }
}
