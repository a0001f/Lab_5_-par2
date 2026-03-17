package cncs.academy.ess.controller;

import cncs.academy.ess.controller.messages.TodoListAddRequest;
import cncs.academy.ess.controller.messages.TodoListAddResponse;
import cncs.academy.ess.model.TodoList;
import cncs.academy.ess.service.TodoListsService;
import io.javalin.http.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoListControllerTest {

    @Mock
    private TodoListsService todoListService;

    @Mock
    private Context ctx;

    @InjectMocks
    private TodoListController controller;

    @Test
    void createTodoList_ShouldReturn201_WhenSuccessful() {
        // Arrange
        TodoListAddRequest request = new TodoListAddRequest();
        request.listName = "New List";
        int userId = 1;

        when(ctx.bodyAsClass(TodoListAddRequest.class)).thenReturn(request);
        when(ctx.attribute("userId")).thenReturn(userId);

        TodoList createdList = new TodoList("New List", userId);
        createdList.setId(10);
        when(todoListService.createTodoListItem(anyString(), anyInt())).thenReturn(createdList);
        when(ctx.status(201)).thenReturn(ctx); // fluent API

        // Act
        controller.createTodoList(ctx);

        // Assert
        verify(ctx).status(201);
        verify(ctx).json(any(TodoListAddResponse.class));
    }

    @Test
    void getTodoList_ShouldReturn200_WhenFound() {
        // Arrange
        int listId = 10;
        int userId = 1;
        when(ctx.pathParam("listId")).thenReturn(String.valueOf(listId));
        when(ctx.attribute("userId")).thenReturn(userId);

        TodoList list = new TodoList("My List", userId);
        list.setId(listId);
        when(todoListService.getTodoList(listId, userId)).thenReturn(list);
        when(ctx.status(200)).thenReturn(ctx);

        // Act
        controller.getTodoList(ctx);

        // Assert
        verify(ctx).status(200);
    }

    @Test
    void getTodoList_ShouldReturn404_WhenNotFound() {
        // Arrange
        int listId = 99;
        int userId = 1;
        when(ctx.pathParam("listId")).thenReturn(String.valueOf(listId));
        when(ctx.attribute("userId")).thenReturn(userId);

        when(todoListService.getTodoList(listId, userId)).thenReturn(null);
        when(ctx.status(404)).thenReturn(ctx);

        // Act
        controller.getTodoList(ctx);

        // Assert
        verify(ctx).status(404);
    }
}
