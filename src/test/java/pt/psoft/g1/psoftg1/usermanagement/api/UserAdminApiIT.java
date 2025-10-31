package pt.psoft.g1.psoftg1.usermanagement.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserAdminApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    @BeforeEach
    @Transactional
    void setUp() {
        em.createQuery("DELETE FROM Reader").executeUpdate();
        em.createQuery("DELETE FROM User").executeUpdate();
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldCreateUser_whenValidRequest() throws Exception {
        String createUserJson = """
                {
                    "username": "newuser@test.com",
                    "password": "StrongPass1!",
                    "name": "Test User",
                    "role": "READER"
                }
                """;
                

    mockMvc.perform(post("/api/admin/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(createUserJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("newuser@test.com"))
        .andExpect(jsonPath("$.fullName").value("Test User"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnBadRequest_whenCreateUserWithInvalidEmail() throws Exception {
        String createUserJson = """
                {
                    "username": "invalid-email",
                    "password": "StrongPass1!",
                    "name": "Test User",
                    "role": "READER"
                }
                """;

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createUserJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateUser_whenValidRequest() throws Exception {
        User user = User.newUser("test@example.com", "StrongPass1!", "Initial Name", "USER");
        user = userRepository.save(user);

        String updateUserJson = """
                {
                    "name": "Updated Name",
                    "enabled": true
                }
                """;

    mockMvc.perform(put("/api/admin/users/" + user.getId())
        .contentType(MediaType.APPLICATION_JSON)
        .content(updateUserJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldDeleteUser_whenExists() throws Exception {
        User user = User.newUser("delete@test.com", "StrongPass1!", "Delete User", "USER");
        user = userRepository.save(user);

    mockMvc.perform(delete("/api/admin/users/" + user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("delete@test.com"));

    mockMvc.perform(get("/api/admin/users/" + user.getId()))
        .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnNotFound_whenDeletingNonExistentUser() throws Exception {
        mockMvc.perform(delete("/api/admin/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldGetUser_whenExists() throws Exception {
        User user = User.newUser("get@test.com", "StrongPass1!", "Get User", "USER");
        user = userRepository.save(user);

    mockMvc.perform(get("/api/admin/users/" + user.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("get@test.com"))
        .andExpect(jsonPath("$.fullName").value("Get User"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturnNotFound_whenGettingNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/admin/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldSearchUsers_withValidCriteria() throws Exception {
        User user1 = User.newUser("user1@test.com", "StrongPass1!", "User One", "USER");
        User user2 = User.newUser("user2@test.com", "StrongPass1!", "User Two", "USER");
        userRepository.save(user1);
        userRepository.save(user2);

        String searchRequestJson = """
                {
                    "page": {
                        "number": 1,
                        "limit": 10
                    },
                    "query": {
                        "username": "user1@test.com"
                    }
                }
                """;

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].username").value("user1@test.com"));
    }

    @Test
    @Transactional
    @WithMockUser(username = "user", roles = {"USER"})
    void shouldReturnForbidden_whenNonAdminAccesses() throws Exception {
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Transactional
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldSearchUsers_withEmptyCriteria() throws Exception {
        User user1 = User.newUser("user1@test.com", "StrongPass1!", "User One", "USER");
        User user2 = User.newUser("user2@test.com", "StrongPass1!", "User Two", "USER");
        userRepository.save(user1);
        userRepository.save(user2);

        String searchRequestJson = """
                {
                    "page": {
                        "number": 1,
                        "limit": 10
                    },
                    "query": {}
                }
                """;

        mockMvc.perform(post("/api/admin/users/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(searchRequestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items").isArray());
    }
}
