package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

public class PasswordTest {

    @Test
    void ensurePasswordCanBeCreatedWithValidPassword() {
        assertDoesNotThrow(() -> new Password("ValidPassword1!"));
    }

    @Test
    void ensureExceptionIsThrownForInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> new Password("invalid"));
    }

    @Test
    void ensureExceptionIsThrownForEmptyPassword() {
        assertThrows(IllegalArgumentException.class, ()-> new Password(""));
    }

    @Test
    void ensureExceptionIsThrownForNullPassword() {
        assertThrows(IllegalArgumentException.class, ()-> new Password(null));
    }
    
}
