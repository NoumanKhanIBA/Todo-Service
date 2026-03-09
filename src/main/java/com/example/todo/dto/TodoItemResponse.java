package com.example.todo.dto;

import com.example.todo.model.TodoStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TodoItemResponse {
    private Long id;
    private String description;
    private TodoStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime dueAt;
    private LocalDateTime doneAt;
}
