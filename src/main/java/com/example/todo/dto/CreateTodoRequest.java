package com.example.todo.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateTodoRequest {

    @NotBlank(message = "Description must not be blank")
    private String description;

    @NotNull(message = "Due date-time must not be null")
    @Future(message = "Due date-time must be in the future")
    private LocalDateTime dueAt;
}
