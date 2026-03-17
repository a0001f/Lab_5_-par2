package cncs.academy.ess.service;

import cncs.academy.ess.model.TodoList;
import cncs.academy.ess.model.User;
import cncs.academy.ess.repository.TodoListsRepository;
import cncs.academy.ess.repository.UserRepository;

import java.util.Collection;

public class TodoListsService {
    TodoListsRepository todoListsRepository;
    UserRepository userRepository;

    public TodoListsService(TodoListsRepository todoListsRepository, UserRepository userRepository) {
        this.todoListsRepository = todoListsRepository;
        this.userRepository = userRepository;
    }

    public boolean shareTodoList(int listId, int ownerId, String targetUsername) {
        TodoList list = todoListsRepository.findById(listId);
        if (list == null || list.getOwnerId() != ownerId) {
            return false;
        }

        User targetUser = userRepository.findByUsername(targetUsername);
        if (targetUser == null) {
            return false;
        }

        todoListsRepository.basicShareList(listId, targetUser.getId());
        return true;
    }

    public TodoList createTodoListItem(String listName, int ownerId) {
        TodoList list = new TodoList(listName, ownerId);
        int listId = todoListsRepository.save(list);
        list.setId(listId);
        return list;
    }

    public TodoList getTodoList(int listId) {
        return todoListsRepository.findById(listId);
    }

    public TodoList getTodoList(int listId, int userId) {
        TodoList list = todoListsRepository.findById(listId);
        if (list == null) {
            return null;
        }
        if (list.getOwnerId() != userId) {
            // Ideally throw an exception here if we want 403,
            // but returning null allows for 404 (Not Found) which shields existence.
            // For this refactor I will return null effectively hiding the resource.
            return null;
        }
        return list;
    }

    public Collection<TodoList> getAllTodoLists(int userId) {
        return todoListsRepository.findAllByUserId(userId);
    }
}
