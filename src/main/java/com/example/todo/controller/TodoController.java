package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoItemResponse;
import com.example.todo.dto.UpdateDescriptionRequest;
import com.example.todo.service.TodoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    @PostMapping
    public ResponseEntity<TodoItemResponse> create(@Valid @RequestBody CreateTodoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(todoService.createTodo(request));
    }

    @PatchMapping("/update-description/{id}")
    public ResponseEntity<TodoItemResponse> updateDescription(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDescriptionRequest request) {
        return ResponseEntity.ok(todoService.updateDescription(id, request));
    }

    @PatchMapping("/done/{id}")
    public ResponseEntity<TodoItemResponse> markAsDone(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.markAsDone(id));
    }

    @PatchMapping("/not-done/{id}")
    public ResponseEntity<TodoItemResponse> markAsNotDone(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.markAsNotDone(id));
    }

    @GetMapping
    public ResponseEntity<List<TodoItemResponse>> getItems(
            @RequestParam(value = "all", defaultValue = "false") boolean all) {
        return ResponseEntity.ok(todoService.getItems(all));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TodoItemResponse> getItem(@PathVariable Long id) {
        return ResponseEntity.ok(todoService.getItem(id));
    }
}
