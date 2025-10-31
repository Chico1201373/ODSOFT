package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserTest {

    private static final PasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    @Test
    void ensureUserCanBeCreated() {
        User user = new User("testUser", "StrongPass1!");
        assertEquals("testUser", user.getUsername());
        assertNotNull(user.getPassword(), "The password should be set");
    }

    @Test
    void passwordIsBCryptHashedAndMatches() {
        String raw = "StrongPass1!";
        User user = new User("hashUser", raw);

        assertNotEquals(raw, user.getPassword(), "The password must be hashed and not equal to the raw value");

        assertTrue(BCRYPT.matches(raw, user.getPassword()), "The hashed password must match the raw value");
    }

    @Test
    void ensureUserCannotBeCreatedWithInvalidPassword() {
        assertThrows(IllegalArgumentException.class, () -> new User("testUser", "invalid"));
    }

    @Test
    void setPasswordReHashes() {
        User user = new User("reHash", "StrongPass1!");
        String firstHash = user.getPassword();

        user.setPassword("NewStrong2!");
        String secondHash = user.getPassword();

        assertNotEquals(firstHash, secondHash, "The hash must change after setting a new password");
        assertTrue(BCRYPT.matches("NewStrong2!", secondHash));
        assertFalse(BCRYPT.matches("StrongPass1!", secondHash));
    }

    @Test
    void enabledDefaultsToTrueAndPropagates() {
        User user = new User("stateUser", "StrongPass1!");

        assertTrue(user.isEnabled());
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
    }

    @Test
    void disablingUserFlipsAllThreeFlags() {
        User user = new User("stateUser2", "StrongPass1!");
        user.setEnabled(false);

        assertFalse(user.isEnabled());
        assertFalse(user.isAccountNonExpired());
        assertFalse(user.isAccountNonLocked());
        assertFalse(user.isCredentialsNonExpired());
    }

    @Test
    void addAuthorityAddsRole() {
        User user = new User("roleUser", "StrongPass1!");
        assertTrue(user.getAuthorities().isEmpty());

        user.addAuthority(new Role("USER"));
        assertEquals(1, user.getAuthorities().size());
    }

    @Test
    void rolesAreDeDuplicated() {
        User user = new User("roleUser2", "StrongPass1!");

        Role r1 = new Role("ADMIN");
        Role r2 = new Role("ADMIN"); // même valeur

        for (Role r : List.of(r1, r2)) {
            user.addAuthority(r);
        }
        assertEquals(1, user.getAuthorities().size(), "The roles can't be duplicated for a user");
    }

    @Test
    void factoryNewUserWithName() {
        User user = User.newUser("factoryUser", "StrongPass1!", "Alice Doe");

        assertEquals("factoryUser", user.getUsername());
        assertNotNull(user.getPassword());
        assertTrue(BCRYPT.matches("StrongPass1!", user.getPassword()), "hash BCrypt expected");

        assertNotNull(user.getName(), "Name must be set by the factory");
    }

    @Test
    void factoryNewUserWithRole() {
        User user = User.newUser("factoryRoleUser", "StrongPass1!", "Bob", "ADMIN");

        assertNotNull(user.getName());
        assertTrue(BCRYPT.matches("StrongPass1!", user.getPassword()));

        // On vérifie qu’un rôle a été ajouté par la factory
        assertEquals(1, user.getAuthorities().size(), "One role expected");
    }

}
