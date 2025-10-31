package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpringDataUserRepositoryTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private CriteriaQuery<User> criteriaQuery;

    @Mock
    private Root<User> root;

    @Mock
    private TypedQuery<User> typedQuery;

    private UserRepoCustomImpl userRepoCustom;

    private User createTestUser(Long id, String username, String fullName) {
        User user = new User(username, "StrongPass1!");
        user.setName(fullName);
        user.setEnabled(true);
        return user;
    }

    @BeforeEach
    void setUp() {
        userRepoCustom = new UserRepoCustomImpl(entityManager);
    }

    @Test
    void searchUsers_WithUsernameOnly() {
        // Given
        Page page = new Page(1, 10);
        SearchUsersQuery query = new SearchUsersQuery();
        query.setUsername("testUser");

        User user1 = createTestUser(1L, "testUser", "Test User");
        List<User> expectedUsers = Arrays.asList(user1);

        // Setup mocks
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(User.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = userRepoCustom.searchUsers(page, query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testUser", result.get(0).getUsername());
        verify(entityManager).createQuery(criteriaQuery);
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void searchUsers_WithFullNameOnly() {
        // Given
        Page page = new Page(1, 10);
        SearchUsersQuery query = new SearchUsersQuery();
        query.setFullName("Test");

        User user1 = createTestUser(1L, "user1", "Test User");
        User user2 = createTestUser(2L, "user2", "Testing Person");
        List<User> expectedUsers = Arrays.asList(user1, user2);

        // Setup mocks
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(User.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = userRepoCustom.searchUsers(page, query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(u -> u.getName().getName().contains("Test")));
        verify(entityManager).createQuery(criteriaQuery);
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void searchUsers_WithNoSearchCriteria() {
        // Given
        Page page = new Page(1, 10);
        SearchUsersQuery query = new SearchUsersQuery();

        User user1 = createTestUser(1L, "user1", "First User");
        User user2 = createTestUser(2L, "user2", "Second User");
        List<User> expectedUsers = Arrays.asList(user1, user2);

        // Setup mocks
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(User.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = userRepoCustom.searchUsers(page, query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(entityManager).createQuery(criteriaQuery);
        verify(typedQuery).setFirstResult(0);
        verify(typedQuery).setMaxResults(10);
    }

    @Test
    void searchUsers_WithPagination() {
        // Given
        Page page = new Page(2, 5); // Second page, 5 items per page
        SearchUsersQuery query = new SearchUsersQuery();

        User user6 = createTestUser(6L, "user6", "User Six");
        User user7 = createTestUser(7L, "user7", "User Seven");
        List<User> expectedUsers = Arrays.asList(user6, user7);

        // Setup mocks
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(User.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(User.class)).thenReturn(root);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.getResultList()).thenReturn(expectedUsers);

        // When
        List<User> result = userRepoCustom.searchUsers(page, query);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(entityManager).createQuery(criteriaQuery);
        verify(typedQuery).setFirstResult(5); // Skip first 5 items
        verify(typedQuery).setMaxResults(5);
    }

    
}