package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoItemResponse;
import com.example.todo.dto.UpdateDescriptionRequest;
import com.example.todo.exception.PastDueModificationException;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import com.example.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TodoService {

    private final TodoRepository todoRepository;
    private final TodoMapper todoMapper;

    /**
     * Creates a new todo item with status NOT_DONE.
     */
    public TodoItemResponse createTodo(CreateTodoRequest request) {
        TodoItem item = TodoItem.builder()
                .description(request.getDescription())
                .dueAt(request.getDueAt())
                .status(TodoStatus.NOT_DONE)
                .build();

        TodoItem saved = todoRepository.save(item);
        log.info("Created todo item with id={}", saved.getId());
        return todoMapper.toResponse(saved);
    }


    public TodoItemResponse updateDescription(Long id, UpdateDescriptionRequest request) {
        TodoItem item = findOrThrow(id);
        guardNotPastDue(item);

        item.setDescription(request.getDescription());
        log.info("Updated description for todo id={}", id);
        return todoMapper.toResponse(todoRepository.save(item));
    }


    public TodoItemResponse markAsDone(Long id) {
        TodoItem item = findOrThrow(id);
        guardNotPastDue(item);

        if (item.getStatus() == TodoStatus.DONE) {
            return todoMapper.toResponse(item);  // make sure item is passed, not null
        }

        item.setStatus(TodoStatus.DONE);
        item.setDoneAt(LocalDateTime.now());
        log.info("Marked todo id={} as DONE", id);
        return todoMapper.toResponse(todoRepository.save(item));
    }

    public TodoItemResponse markAsNotDone(Long id) {
        TodoItem item = findOrThrow(id);
        guardNotPastDue(item);

        item.setStatus(TodoStatus.NOT_DONE);
        item.setDoneAt(null);
        log.info("Marked todo id={} as NOT_DONE", id);
        return todoMapper.toResponse(todoRepository.save(item));
    }

    public Page<TodoItemResponse> getItems(boolean includeAll, Pageable pageable) {
        Page<TodoItem> items = includeAll
                ? todoRepository.findAll(pageable)
                : todoRepository.findByStatus(TodoStatus.NOT_DONE,pageable);
        return items.map(todoMapper::toResponse);
    }


    public TodoItemResponse getItem(Long id) {
        return todoMapper.toResponse(findOrThrow(id));
    }


    private TodoItem findOrThrow(Long id) {
        return todoRepository.findById(id)
                .orElseThrow(() -> new TodoNotFoundException(id));
    }

    private void guardNotPastDue(TodoItem item) {
        if (item.getStatus() == TodoStatus.PAST_DUE) {
            throw new PastDueModificationException(item.getId());
        }
    }


}
