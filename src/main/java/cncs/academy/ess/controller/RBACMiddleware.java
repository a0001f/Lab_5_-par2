package cncs.academy.ess.controller;

import cncs.academy.ess.service.RBACService;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RBACMiddleware implements Handler {
    private static final Logger log = LoggerFactory.getLogger(RBACMiddleware.class);
    private final RBACService rbacService;

    public RBACMiddleware(RBACService rbacService) {
        this.rbacService = rbacService;
    }

    @Override
    public void handle(Context ctx) throws Exception {
        // Obter username do JWT (já validado pelo AuthorizationMiddleware)
        String username = ctx.attribute("username");

        // Se não há username, é rota pública - skip RBAC
        if (username == null) {
            log.info("RBAC Skipped: username is null for {} {}", ctx.method(), ctx.path());
            return;
        }

        String resource;
        try {
            resource = ctx.endpointHandlerPath();
        } catch (Exception e) {
            // Fallback for when endpointHandlerPath is not available (e.g. 404s or before
            // matching)
            resource = ctx.path();
        }
        String action = ctx.method().name();

        log.info("RBAC Enforcing: user={} resource={} action={}", username, resource, action);
        log.info("RBAC Debug: Roles for user {} are: {}", username, rbacService.getRolesForUser(username));

        // Verificar permissão
        if (!rbacService.enforce(username, resource, action)) {
            log.info("RBAC denied: {} {} {}", username, action, resource);
            log.info("DEBUG: Roles for user {}: {}", username, rbacService.getRolesForUser(username));
            throw new ForbiddenResponse("Insufficient permissions for this resource");
        }

        log.debug("RBAC allowed: {} {} {}", username, action, resource);
    }
}
