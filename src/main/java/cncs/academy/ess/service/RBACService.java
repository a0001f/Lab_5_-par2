package cncs.academy.ess.service;

import org.casbin.jcasbin.main.Enforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RBACService {
    private static final Logger log = LoggerFactory.getLogger(RBACService.class);
    private final Enforcer enforcer;

    public RBACService() {
        // Load model and policy from classpath by copying to temp files
        try {
            String modelPath = copyResourceToTempFile("model.conf");
            String policyPath = copyResourceToTempFile("policy.csv");

            this.enforcer = new Enforcer(modelPath, policyPath);
            log.info("RBAC Enforcer initialized with model: {} and policy: {}", modelPath, policyPath);
        } catch (Exception e) {
            log.error("Failed to load RBAC configuration", e);
            throw new RuntimeException("Failed to load RBAC configuration", e);
        }
    }

    private String copyResourceToTempFile(String resourceName) throws java.io.IOException {
        try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new java.io.FileNotFoundException(resourceName + " not found in classpath");
            }
            java.io.File tempFile = java.io.File.createTempFile(resourceName, null);
            tempFile.deleteOnExit();
            java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return tempFile.getAbsolutePath();
        }
    }

    /**
     * Verifica se utilizador tem permissão para aceder recurso
     * 
     * @param username Username do utilizador
     * @param resource Caminho do recurso (ex: /todolist/123)
     * @param action   Método HTTP (GET, POST, DELETE, etc)
     * @return true se permitido, false caso contrário
     */
    public boolean enforce(String username, String resource, String action) {
        boolean allowed = enforcer.enforce(username, resource, action);
        log.debug("RBAC check: {} {} {} = {}", username, action, resource, allowed);
        return allowed;
    }

    /**
     * Adiciona role a utilizador (runtime)
     */
    public void addRoleForUser(String username, String role) {
        enforcer.addRoleForUser(username, role);
        log.info("Added role '{}' to user '{}'", role, username);
    }

    /**
     * Remove role de utilizador
     */
    public void deleteRoleForUser(String username, String role) {
        enforcer.deleteRoleForUser(username, role);
        log.info("Removed role '{}' from user '{}'", role, username);
    }

    /**
     * Obtém roles de um utilizador
     */
    public java.util.List<String> getRolesForUser(String username) {
        return enforcer.getRolesForUser(username);
    }

    /**
     * Verifica se utilizador tem uma role específica
     */
    public boolean hasRole(String username, String role) {
        return enforcer.hasRoleForUser(username, role);
    }
}
