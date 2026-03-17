package cncs.academy.ess.controller;

import cncs.academy.ess.service.OAuth2Service;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2Controller {
    private static final Logger log = LoggerFactory.getLogger(OAuth2Controller.class);
    private final OAuth2Service oauth2Service;

    public OAuth2Controller(OAuth2Service oauth2Service) {
        this.oauth2Service = oauth2Service;
    }

    /**
     * GET /auth/google - Redireciona para Google OAuth2
     */
    public void initiateGoogleLogin(Context ctx) {
        log.info("Initiating Google OAuth2 login");
        String authUrl = oauth2Service.getAuthorizationUrl();
        ctx.redirect(authUrl);
    }

    /**
     * GET /auth/google/callback - Callback do Google após autenticação
     */
    public void handleGoogleCallback(Context ctx) {
        String code = ctx.queryParam("code");
        String state = ctx.queryParam("state");
        String error = ctx.queryParam("error");

        if (error != null) {
            log.warn("OAuth2 error: {}", error);
            ctx.status(400).json(Map.of("error", "OAuth2 authentication failed: " + error));
            return;
        }

        if (code == null) {
            log.warn("Missing authorization code in callback");
            ctx.status(400).json(Map.of("error", "Missing authorization code"));
            return;
        }

        try {
            log.info("Processing OAuth2 callback");
            String jwt = oauth2Service.handleCallback(code);

            // Redirecionar para frontend com token
            ctx.redirect("/index.html?token=" + jwt);

        } catch (Exception e) {
            log.error("OAuth2 callback error", e);
            ctx.redirect("/index.html?error=" + java.net.URLEncoder.encode("Authentication failed: " + e.getMessage(),
                    java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
