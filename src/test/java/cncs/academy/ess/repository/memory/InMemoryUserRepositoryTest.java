package cncs.academy.ess.repository.memory;

import cncs.academy.ess.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {
    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    void saveAndFindById_ShouldReturnSavedUser() {
        // Arrange
        User user = new User("jane", "password", "salt");

        // Act
        int id = repository.save(user);
        User savedUser = repository.findById(id);

        // Assert
        assertEquals(user.getUsername(), savedUser.getUsername());
        assertEquals(user.getPasswordHash(), savedUser.getPasswordHash());
    }

    @Test
    void findByUsername_ShouldReturnUser_WhenExists() {
        // Arrange
        User user = new User("john", "secret", "salt");
        repository.save(user);

        // Act
        User found = repository.findByUsername("john");

        // Assert
        assertNotNull(found);
        assertEquals("john", found.getUsername());
    }

    @Test
    void findByUsername_ShouldReturnNull_WhenNotExists() {
        // Act
        User found = repository.findByUsername("nonexistent");

        // Assert
        assertNull(found);
    }

    @Test
    void findAll_ShouldReturnAllUsers() {
        // Arrange
        repository.save(new User("user1", "pass", "salt"));
        repository.save(new User("user2", "pass", "salt"));

        // Act
        var users = repository.findAll();

        // Assert
        assertEquals(2, users.size());
    }

    @Test
    void deleteById_ShouldRemoveUser() {
        // Arrange
        User user = new User("todelete", "pass", "salt");
        int id = repository.save(user);

        // Act
        repository.deleteById(id);
        User deletedUser = repository.findById(id);

        // Assert
        assertNull(deletedUser);
    }
}
