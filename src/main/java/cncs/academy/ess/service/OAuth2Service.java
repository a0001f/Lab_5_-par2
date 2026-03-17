package cncs.academy.ess.service;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import cncs.academy.ess.config.OAuth2Config;
import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import cncs.academy.ess.util.AuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OAuth2Service {
    private static final Logger log = LoggerFactory.getLogger(OAuth2Service.class);
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private final UserRepository userRepository;
    private final RBACService rbacService;

    public OAuth2Service(UserRepository userRepository, RBACService rbacService) {
        this.userRepository = userRepository;
        this.rbacService = rbacService;
    }

    /**
     * Gera URL de autorização do Google
     */
    public String getAuthorizationUrl() {
        String scope = URLEncoder.encode("openid email profile", StandardCharsets.UTF_8);
        String state = generateState();

        return OAuth2Config.AUTHORIZATION_URL +
                "?client_id=" + OAuth2Config.CLIENT_ID +
                "&redirect_uri=" + URLEncoder.encode(OAuth2Config.REDIRECT_URI, StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&scope=" + scope +
                "&state=" + state;
    }

    /**
     * Troca authorization code por access token e cria/atualiza utilizador
     */
    public String handleCallback(String code) throws IOException {
        log.info("Processing OAuth2 callback with code");

        // 1. Trocar code por access_token
        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory();

        GenericUrl tokenUrl = new GenericUrl(OAuth2Config.TOKEN_URL);
        UrlEncodedContent content = new UrlEncodedContent(Map.of(
                "code", code,
                "client_id", OAuth2Config.CLIENT_ID,
                "client_secret", OAuth2Config.CLIENT_SECRET,
                "redirect_uri", OAuth2Config.REDIRECT_URI,
                "grant_type", "authorization_code"));

        HttpRequest tokenRequest = requestFactory.buildPostRequest(tokenUrl, content);
        HttpResponse tokenResponse = tokenRequest.execute();

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenData = JSON_FACTORY.fromInputStream(
                tokenResponse.getContent(),
                Map.class);

        String accessToken = (String) tokenData.get("access_token");
        log.debug("Received access token from Google");

        // 2. Obter informação do utilizador
        GenericUrl userinfoUrl = new GenericUrl(OAuth2Config.USERINFO_URL);
        HttpRequest userinfoRequest = requestFactory.buildGetRequest(userinfoUrl);
        userinfoRequest.getHeaders().setAuthorization("Bearer " + accessToken);

        HttpResponse userinfoResponse = userinfoRequest.execute();

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = JSON_FACTORY.fromInputStream(
                userinfoResponse.getContent(),
                Map.class);

        String email = (String) userInfo.get("email");
        log.info("OAuth2 user authenticated: {}", email);

        // 3. Criar ou obter utilizador
        User user = userRepository.findByUsername(email);
        if (user == null) {
            // Criar novo utilizador OAuth2 (sem password local)
            user = new User(email, "", ""); // Hash e salt vazios para OAuth2
            user.setRole("base"); // Role padrão
            int id = userRepository.save(user);
            user.setId(id);
            log.info("Created new OAuth2 user: {}", email);
        } else {
            log.info("Existing user logged in via OAuth2: {}", email);
        }

        // 4. Register role in RBAC Enforcer (Runtime)
        // This is critical because Casbin only loads policy.csv on startup.
        // Dynamic users need their roles added to the in-memory enforcer.
        if (user.getRole() != null && !user.getRole().isEmpty()) {
            rbacService.addRoleForUser(user.getUsername(), user.getRole());
        } else {
            // Fallback default
            rbacService.addRoleForUser(user.getUsername(), "base");
        }

        // 5. Gerar JWT
        return AuthUtils.generateToken(user.getUsername());
    }

    private String generateState() {
        return AuthUtils.generateSalt(); // Reutiliza gerador de salt
    }
}
