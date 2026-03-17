package cncs.academy.ess.service;

import cncs.academy.ess.model.TodoList;
import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.TodoListsRepository;
import cncs.academy.ess.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoListsServiceTest {

    @Mock
    private TodoListsRepository todoListsRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TodoListsService todoListsService;

    @Test
    void createTodoListItem_ShouldReturnCreatedList() {
        // Arrange
        String listName = "My List";
        int ownerId = 1;
        when(todoListsRepository.save(any(TodoList.class))).thenReturn(100);

        // Act
        TodoList result = todoListsService.createTodoListItem(listName, ownerId);

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getListId());
        assertEquals(listName, result.getName());
        assertEquals(ownerId, result.getOwnerId());
        verify(todoListsRepository).save(any(TodoList.class));
    }

    @Test
    void shareTodoList_ShouldReturnTrue_WhenValid() {
        // Arrange
        int listId = 1;
        int ownerId = 1;
        String targetUsername = "jane";

        TodoList list = new TodoList("List", ownerId);
        list.setId(listId);
        User targetUser = new User("jane", "pass", "salt");
        targetUser.setId(2);

        when(todoListsRepository.findById(listId)).thenReturn(list);
        when(userRepository.findByUsername(targetUsername)).thenReturn(targetUser);

        // Act
        boolean result = todoListsService.shareTodoList(listId, ownerId, targetUsername);

        // Assert
        assertTrue(result);
        verify(todoListsRepository).basicShareList(listId, targetUser.getId());
    }

    @Test
    void shareTodoList_ShouldReturnFalse_WhenNotOwner() {
        // Arrange
        int listId = 1;
        int ownerId = 1;
        int otherUserId = 2; // Requesting user
        String targetUsername = "jane";

        TodoList list = new TodoList("List", ownerId);
        list.setId(listId);

        when(todoListsRepository.findById(listId)).thenReturn(list);

        // Act
        boolean result = todoListsService.shareTodoList(listId, otherUserId, targetUsername);

        // Assert
        assertFalse(result);
        verify(todoListsRepository, never()).basicShareList(anyInt(), anyInt());
    }
}
