package cncs.academy.ess.service;

import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import cncs.academy.ess.util.AuthUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TodoUserService todoUserService;

    @Test
    void login_shouldReturnValidJWTTokenWhenCredentialsMatch() throws NoSuchAlgorithmException {
        // Arrange
        String username = "validUser";
        String password = "correctPassword";
        String salt = AuthUtils.generateSalt(); // Using real utility for convenience
        String hash = AuthUtils.hashPassword(password, salt);
        User user = new User(username, hash, salt);

        when(userRepository.findByUsername(username)).thenReturn(user);

        // Act
        String result = todoUserService.login(username, password);

        // Assert
        assertNotNull(result, "Result should not be null");
        assertTrue(result.startsWith("Bearer "), "Token should start with 'Bearer '");

        String token = result.substring(7); // Remove "Bearer " prefix
        String decodedUsername = AuthUtils.validateToken(token);

        assertEquals(username, decodedUsername, "Token should be valid and contain the correct username");

        verify(userRepository).findByUsername(username);
    }
}
