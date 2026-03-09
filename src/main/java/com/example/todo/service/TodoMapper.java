package com.example.todo.service;

import com.example.todo.dto.TodoItemResponse;
import com.example.todo.model.TodoItem;
import org.springframework.stereotype.Component;

@Component
public class TodoMapper {

    public TodoItemResponse toResponse(TodoItem item) {
        return TodoItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .status(item.getStatus())
                .createdAt(item.getCreatedAt())
                .dueAt(item.getDueAt())
                .doneAt(item.getDoneAt())
                .build();
    }
}
