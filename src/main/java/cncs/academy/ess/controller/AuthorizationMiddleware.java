package cncs.academy.ess.controller;

import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.UserRepository;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import io.javalin.http.UnauthorizedResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cncs.academy.ess.service.RBACService;

public class AuthorizationMiddleware implements Handler {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationMiddleware.class);
    private final UserRepository userRepository;
    private final RBACService rbacService;

    public AuthorizationMiddleware(UserRepository userRepository, RBACService rbacService) {
        this.userRepository = userRepository;
        this.rbacService = rbacService;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        // if method is OPTIONS bypass auth middleware
        if (ctx.method() == HandlerType.OPTIONS) {
            // Optionally: validate if it is a legitimate CORS preflight
            return;
        }

        // Allow unauthenticated requests to /login and OAuth2 routes
        if (ctx.path().equals("/login") && ctx.method().name().equals("POST") ||
                ctx.path().startsWith("/auth/"))
            return;

        // Allow unauthenticated POST /user for public registration (no Authorization
        // header)
        if (ctx.path().equals("/user") && ctx.method().name().equals("POST")) {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                // No auth header = public registration, allow
                return;
            }
            // Has auth header = authenticated user trying to create user, continue to RBAC
            // check
        }

        // Allow static files and root
        // Allow static files and root
        if (ctx.path().equals("/") || ctx.path().equals("/index.html") || ctx.path().startsWith("/public/")
                || ctx.path().equals("/debug/users") || ctx.path().equals("/debug/role")) {
            return;
        }

        // Check if authorization header exists
        String authorizationHeader = ctx.header("Authorization");
        String path = ctx.path();
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.info("Authorization header is missing or invalid '{}' for path '{}'", authorizationHeader, path);
            throw new UnauthorizedResponse();
        }

        // Extract token from authorization header
        String token = authorizationHeader.substring(7); // Remove "Bearer "

        // Check if token is valid (perform authentication logic)
        int userId = validateTokenAndGetUserId(token);
        if (userId == -1) {
            logger.info("Authorization token is invalid {}", token);
            throw new UnauthorizedResponse();
        }

        // Add user ID and username to context for use in route handlers and RBAC
        ctx.attribute("userId", userId);

        // Get user to extract username for RBAC
        User user = userRepository.findById(userId);
        if (user != null) {
            logger.info("AuthMiddleware: Setting username={} for userId={}", user.getUsername(), userId);
            ctx.attribute("username", user.getUsername());

            // Sync role to Casbin enforcer (fix for 403 after restart)
            String role = user.getRole();
            if (role == null || role.isEmpty()) {
                role = "base";
            }
            rbacService.addRoleForUser(user.getUsername(), role);
        } else {
            logger.warn("AuthMiddleware: User found by token but null by ID? userId={}", userId);
        }
    }

    /**
     * NOTE: This method currently uses username lookup as a placeholder for real
     * token validation.
     * Replace with proper token parsing/verification (e.g., JWT, session lookup) as
     * needed.
     */
    private Integer validateTokenAndGetUserId(String token) {
        // Validate JWT Token
        String username = cncs.academy.ess.util.AuthUtils.validateToken(token);
        if (username == null) {
            return -1;
        }

        // Find user by username from token
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return -1;
        }
        return user.getId();
    }
}
