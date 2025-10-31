package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ReaderTest {
    @Test
    void ensureReaderCanBeCreatedWithFactoryMethod() {
        Reader reader = Reader.newReader("readerUser", "StrongPass1!", "Alice Smith");
        assert reader.getUsername().equals("readerUser");
        assert reader.getName().toString().equals("Alice Smith");
        assert reader.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals(Role.READER));
    }

    @Test
    void ensureReaderCannotBeCreatedWithFactoryWithInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> Reader.newReader("readerUser", "weak", "Alice Smith"));
    }

    @Test
    void ensureReaderCanBeCreated() {
        Reader reader = new Reader("readerUser", "StrongPass1!");
        assert reader.getUsername().equals("readerUser");
    }

    @Test
    void ensureReaderCannotBeCreatedWithInvalidPassword(){
        assertThrows(IllegalArgumentException.class,()-> new Reader("readerUser", "invalid"));
    }
}
