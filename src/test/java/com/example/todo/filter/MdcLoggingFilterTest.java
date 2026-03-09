package com.example.todo.controller;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.UpdateDescriptionRequest;
import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import com.example.todo.repository.TodoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TodoRepository todoRepository;

    @BeforeEach
    void cleanDb() {
        todoRepository.deleteAll();
    }

    // ---- Helpers ----

    private long createTodoViaRepo(String description, LocalDateTime dueAt, TodoStatus status) {
        TodoItem item = TodoItem.builder()
                .description(description)
                .dueAt(dueAt)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
        return todoRepository.save(item).getId();
    }

    private String json(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    // ---- Tests ----

    @Nested
    @DisplayName("POST /api/todos")
    class CreateTodoTests {

        @Test
        @DisplayName("201 - should create a todo with valid request")
        void shouldCreate() throws Exception {
            CreateTodoRequest req = new CreateTodoRequest();
            req.setDescription("Learn Spring Boot");
            req.setDueAt(LocalDateTime.now().plusDays(5));

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.description").value("Learn Spring Boot"))
                    .andExpect(jsonPath("$.status").value("NOT_DONE"))
                    .andExpect(jsonPath("$.doneAt").doesNotExist());
        }

        @Test
        @DisplayName("400 - should reject blank description")
        void shouldRejectBlankDescription() throws Exception {
            CreateTodoRequest req = new CreateTodoRequest();
            req.setDescription("  ");
            req.setDueAt(LocalDateTime.now().plusDays(1));

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - should reject past due date")
        void shouldRejectPastDueDate() throws Exception {
            CreateTodoRequest req = new CreateTodoRequest();
            req.setDescription("Old task");
            req.setDueAt(LocalDateTime.now().minusDays(1));

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - should reject missing dueAt")
        void shouldRejectMissingDueAt() throws Exception {
            CreateTodoRequest req = new CreateTodoRequest();
            req.setDescription("Task without due date");

            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("400 - should return clear message for invalid date format")
        void shouldRejectInvalidDateFormat() throws Exception {
            mockMvc.perform(post("/api/todos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"description\": \"Buy groceries\", \"dueAt\": \"31-12-2026\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Invalid Request Body"))
                    .andExpect(jsonPath("$.detail").value(containsString("dueAt")))
                    .andExpect(jsonPath("$.detail").value(containsString("yyyy-MM-dd'T'HH:mm:ss")));
        }
    }

    @Nested
    @DisplayName("PATCH /api/todos/{id}/description")
    class UpdateDescriptionTests {

        @Test
        @DisplayName("200 - should update description")
        void shouldUpdateDescription() throws Exception {
            long id = createTodoViaRepo("Old", LocalDateTime.now().plusDays(1), TodoStatus.NOT_DONE);
            UpdateDescriptionRequest req = new UpdateDescriptionRequest();
            req.setDescription("New description");

            mockMvc.perform(patch("/api/todos/update-description/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.description").value("New description"));
        }

        @Test
        @DisplayName("422 - should reject updating a PAST_DUE item")
        void shouldRejectPastDue() throws Exception {
            long id = createTodoViaRepo("Expired", LocalDateTime.now().minusDays(1), TodoStatus.PAST_DUE);
            UpdateDescriptionRequest req = new UpdateDescriptionRequest();
            req.setDescription("Trying anyway");

            mockMvc.perform(patch("/api/todos/update-description/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isUnprocessableEntity());
        }

        @Test
        @DisplayName("404 - should return 404 for unknown id")
        void shouldReturn404() throws Exception {
            UpdateDescriptionRequest req = new UpdateDescriptionRequest();
            req.setDescription("Ghost");

            mockMvc.perform(patch("/api/todos/update-description/9999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PATCH /api/todos/done/{id}")
    class MarkAsDoneTests {

        @Test
        @DisplayName("200 - should mark item as done")
        void shouldMarkAsDone() throws Exception {
            long id = createTodoViaRepo("Task", LocalDateTime.now().plusDays(1), TodoStatus.NOT_DONE);

            mockMvc.perform(patch("/api/todos/done/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("DONE"))
                    .andExpect(jsonPath("$.doneAt").exists());
        }

        @Test
        @DisplayName("422 - should reject marking PAST_DUE item as done")
        void shouldRejectPastDue() throws Exception {
            long id = createTodoViaRepo("Expired", LocalDateTime.now().minusDays(1), TodoStatus.PAST_DUE);

            mockMvc.perform(patch("/api/todos/done/{id}", id))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("PATCH /api/todos/not-done/{id}")
    class MarkAsNotDoneTests {

        @Test
        @DisplayName("200 - should revert a DONE item to NOT_DONE and clear doneAt")
        void shouldRevertDoneItem() throws Exception {
            long id = createTodoViaRepo("Task", LocalDateTime.now().plusDays(1), TodoStatus.DONE);
            // Set doneAt via repo
            TodoItem item = todoRepository.findById(id).orElseThrow();
            item.setDoneAt(LocalDateTime.now().minusMinutes(10));
            todoRepository.save(item);

            mockMvc.perform(patch("/api/todos/not-done/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("NOT_DONE"))
                    .andExpect(jsonPath("$.doneAt").doesNotExist());
        }

        @Test
        @DisplayName("422 - should reject reverting a PAST_DUE item")
        void shouldRejectPastDue() throws Exception {
            long id = createTodoViaRepo("Expired", LocalDateTime.now().minusDays(1), TodoStatus.PAST_DUE);

            mockMvc.perform(patch("/api/todos/not-done/{id}", id))
                    .andExpect(status().isUnprocessableEntity());
        }
    }

    @Nested
    @DisplayName("GET /api/todos")
    class GetItemsTests {

        @Test
        @DisplayName("should return only NOT_DONE items by default")
        void shouldReturnNotDone() throws Exception {
            createTodoViaRepo("Not done task", LocalDateTime.now().plusDays(1), TodoStatus.NOT_DONE);
            createTodoViaRepo("Done task", LocalDateTime.now().plusDays(1), TodoStatus.DONE);
            createTodoViaRepo("Past due task", LocalDateTime.now().minusDays(1), TodoStatus.PAST_DUE);

            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))              // $.content not $
                    .andExpect(jsonPath("$.content[0].status").value("NOT_DONE"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return all items when all=true")
        void shouldReturnAllItems() throws Exception {
            createTodoViaRepo("Not done task", LocalDateTime.now().plusDays(1), TodoStatus.NOT_DONE);
            createTodoViaRepo("Done task", LocalDateTime.now().plusDays(1), TodoStatus.DONE);
            createTodoViaRepo("Past due task", LocalDateTime.now().minusDays(1), TodoStatus.PAST_DUE);

            mockMvc.perform(get("/api/todos").param("all", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))              // $.content not $
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("should return empty list when no NOT_DONE items exist")
        void shouldReturnEmptyList() throws Exception {
            mockMvc.perform(get("/api/todos"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))              // $.content not $
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/todos/{id}")
    class GetItemTests {

        @Test
        @DisplayName("200 - should return item details")
        void shouldReturnItem() throws Exception {
            long id = createTodoViaRepo("My task", LocalDateTime.now().plusDays(2), TodoStatus.NOT_DONE);

            mockMvc.perform(get("/api/todos/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.description").value("My task"))
                    .andExpect(jsonPath("$.status").value("NOT_DONE"));
        }

        @Test
        @DisplayName("404 - should return 404 for unknown id")
        void shouldReturn404() throws Exception {
            mockMvc.perform(get("/api/todos/9999"))
                    .andExpect(status().isNotFound());
        }
    }
}
