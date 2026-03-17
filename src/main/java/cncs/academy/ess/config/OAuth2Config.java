package cncs.academy.ess.config;

import io.github.cdimascio.dotenv.Dotenv;

public class OAuth2Config {
        private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Google OAuth2 credentials (from .env or environment variables)
        public static final String CLIENT_ID = dotenv.get("GOOGLE_CLIENT_ID");

        public static final String CLIENT_SECRET = dotenv.get("GOOGLE_CLIENT_SECRET");

        public static final String REDIRECT_URI = dotenv.get("OAUTH2_REDIRECT_URI",
                        "https://localhost:8443/auth/google/callback");

        // Google OAuth2 endpoints
        public static final String AUTHORIZATION_URL = "https://accounts.google.com/o/oauth2/v2/auth";
        public static final String TOKEN_URL = "https://www.googleapis.com/oauth2/v3/token";
        public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
}
