package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.Assert.assertThrows;

import org.junit.jupiter.api.Test;

public class LibrarianTest {

    @Test
    void ensureLibrarianCanBeCreatedWithFactoryMethod() {
        Librarian librarian = Librarian.newLibrarian("librarianUser", "StrongPass1!", "Alice Smith");
        assert librarian.getUsername().equals("librarianUser");
        assert librarian.getName().toString().equals("Alice Smith");
        assert librarian.getAuthorities().stream().anyMatch(role -> role.getAuthority().equals(Role.LIBRARIAN));
    }

    @Test
    void ensureLibrarianCannotBeCreatedWithFactoryWithInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> Librarian.newLibrarian("librarianUser", "weak", "Alice Smith"));
    }

    @Test
    void ensureLibrarianCanBeCreated() {
        Librarian librarian = new Librarian("librarianUser", "StrongPass1!");
        assert librarian.getUsername().equals("librarianUser");
    }

    @Test
    void ensureLibrarianCannotBeCreatedWithInvalidPassword(){
        assertThrows(IllegalArgumentException.class,()-> new Librarian("llibrarianUser", "invalid"));
    }

}
