package cncs.academy.ess.util;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class JwtStructureTest {

    @Test
    void testJwtStructure() {
        // Arrange
        String username = "testUser";
        String token = AuthUtils.generateToken(username);

        // Act
        assertNotNull(token);
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");

        // Validate Header
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        assertTrue(headerJson.contains("\"alg\":\"HS256\""), "Header should specify HS256 algorithm");
        assertTrue(headerJson.contains("\"typ\":\"JWT\""), "Header should specify JWT type");

        // Validate Payload
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        assertTrue(payloadJson.contains("\"username\":\"" + username + "\""),
                "Payload should contain correct username");
        assertTrue(payloadJson.contains("\"iss\":\"cncs-academy-ess\""), "Payload should contain correct issuer");
    }
}
