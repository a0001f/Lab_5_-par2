package cncs.academy.ess;

import cncs.academy.ess.controller.AuthorizationMiddleware;
import cncs.academy.ess.controller.TodoController;
import cncs.academy.ess.controller.TodoListController;
import cncs.academy.ess.controller.UserController;
import cncs.academy.ess.repository.sql.SQLTodoListsRepository;
import cncs.academy.ess.repository.sql.SQLTodoRepository;
import cncs.academy.ess.repository.sql.SQLUserRepository;
import cncs.academy.ess.service.DuplicateUserException;
import cncs.academy.ess.service.TodoListsService;
import cncs.academy.ess.service.TodoService;
import cncs.academy.ess.service.TodoUserService;
import io.javalin.Javalin;
import org.apache.commons.dbcp2.BasicDataSource;

import java.security.NoSuchAlgorithmException;

public class App {
    public static void main(String[] args) throws NoSuchAlgorithmException, DuplicateUserException {
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("src/main/resources/public", io.javalin.http.staticfiles.Location.EXTERNAL);
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.anyHost();
                });
            });
            // Configure SSL/HTTPS
            config.router.ignoreTrailingSlashes = true;
            config.registerPlugin(new io.javalin.community.ssl.SslPlugin(ssl -> {
                ssl.pemFromClasspath("cert.pem", "key.pem");
                ssl.insecure = true; // Enable HTTP
                ssl.insecurePort = 7100; // Match Postman
                ssl.securePort = 8443;
            }));
        });

        // Global Exception Handler
        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500);
            ctx.json(java.util.Collections.singletonMap("message", "Server Error: " + e.getMessage()));
        });

        app.get("/", ctx -> ctx.redirect("/index.html"));

        // Initialize Database Setup
        /*
         * BasicDataSource dataSource = new BasicDataSource();
         * dataSource.setUrl("jdbc:postgresql://127.0.0.1:5433/postgres");
         * dataSource.setUsername("postgres");
         * dataSource.setPassword("mysecretpassword");
         * dataSource.setDriverClassName("org.postgresql.Driver");
         */

        // Initialize Database Schema
        // cncs.academy.ess.util.DatabaseInitializer.initialize(dataSource);

        // Initialize routes for user management
        cncs.academy.ess.repository.memory.InMemoryUserRepository userRepository = new cncs.academy.ess.repository.memory.InMemoryUserRepository();
        TodoUserService userService = new TodoUserService(userRepository);
        UserController userController = new UserController(userService);

        cncs.academy.ess.repository.memory.InMemoryTodoListsRepository listsRepository = new cncs.academy.ess.repository.memory.InMemoryTodoListsRepository();
        TodoListsService toDoListService = new TodoListsService(listsRepository, userRepository);
        TodoListController todoListController = new TodoListController(toDoListService);

        cncs.academy.ess.repository.memory.InMemoryTodoRepository todoRepository = new cncs.academy.ess.repository.memory.InMemoryTodoRepository();
        TodoService todoService = new TodoService(todoRepository, listsRepository);
        TodoController todoController = new TodoController(todoService, toDoListService);

        // RBAC Service and Middleware
        cncs.academy.ess.service.RBACService rbacService = new cncs.academy.ess.service.RBACService();
        cncs.academy.ess.controller.RBACMiddleware rbacMiddleware = new cncs.academy.ess.controller.RBACMiddleware(
                rbacService);

        AuthorizationMiddleware authMiddleware = new AuthorizationMiddleware(userRepository, rbacService);

        // OAuth2 Service and Controller
        cncs.academy.ess.service.OAuth2Service oauth2Service = new cncs.academy.ess.service.OAuth2Service(
                userRepository, rbacService);
        cncs.academy.ess.controller.OAuth2Controller oauth2Controller = new cncs.academy.ess.controller.OAuth2Controller(
                oauth2Service);

        // CORS
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "*");
        });
        // Authorization middleware (validates JWT)
        app.before(authMiddleware::handle);
        // RBAC middleware (validates permissions)
        app.before(rbacMiddleware::handle);

        // OAuth2 routes (public - handled by AuthorizationMiddleware)
        app.get("/auth/google", oauth2Controller::initiateGoogleLogin);
        app.get("/auth/google/callback", oauth2Controller::handleGoogleCallback);

        // User management
        app.post("/user", userController::createUser);
        app.get("/user/{userId}", userController::getUser);
        app.delete("/user/{userId}", userController::deleteUser);
        app.post("/login", userController::loginUser);

        // "To do" lists management
        /*
         * POST /todolist
         * {
         * "listName": "Shopping list"
         * }
         */
        app.post("/todolist", todoListController::createTodoList);
        app.get("/todolist", todoListController::getAllTodoLists);
        app.get("/todolist/{listId}", todoListController::getTodoList);
        app.post("/todolist/{listId}/share", todoListController::shareTodoList);

        // "To do" list items management
        /*
         * POST /todo/item
         * {
         * "description": "Buy milk",
         * "listId": 1
         * }
         */
        app.post("/todo/item", todoController::createTodoItem);
        /* GET /todo/1/tasks */
        app.get("/todo/{listId}/tasks", todoController::getAllTodoItems);
        /* GET /todo/1/tasks/1 */
        app.get("/todo/{listId}/tasks/{taskId}", todoController::getTodoItem);
        /* DELETE /todo/1/tasks/1 */
        app.delete("/todo/{listId}/tasks/{taskId}", todoController::deleteTodoItem);

        // fillDummyData(userService, toDoListService, todoService);

        // DEBUG ENDPOINT FOR CHECKPOINT 1 (Show stored hashes)
        app.get("/debug/users", ctx -> {
            ctx.json(userRepository.findAll());
        });

        // DEBUG ENDPOINT FOR CHECKPOINT 2 (Assign Role)
        app.post("/debug/role", ctx -> {
            String username = ctx.queryParam("username");
            String role = ctx.queryParam("role");
            cncs.academy.ess.model.User user = userRepository.findByUsername(username);
            if (user != null) {
                user.setRole(role);
                ctx.result("Role updated to " + role);
            } else {
                ctx.status(404).result("User not found");
            }
        });

        app.start(8443);
    }

    private static void fillDummyData(
            TodoUserService userService,
            TodoListsService toDoListService,
            TodoService todoService) throws NoSuchAlgorithmException, DuplicateUserException {
        userService.addUser("user1", "password1");
        userService.addUser("user2", "password2");
        toDoListService.createTodoListItem("Shopping list", 1);
        toDoListService.createTodoListItem("Other", 1);
        todoService.createTodoItem("Bread", 1);
        todoService.createTodoItem("Milk", 1);
        todoService.createTodoItem("Eggs", 1);
        todoService.createTodoItem("Cheese", 1);
        todoService.createTodoItem("Butter", 1);
    }
}
