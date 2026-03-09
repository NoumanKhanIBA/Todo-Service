package com.example.todo.service;

import com.example.todo.dto.CreateTodoRequest;
import com.example.todo.dto.TodoItemResponse;
import com.example.todo.dto.UpdateDescriptionRequest;
import com.example.todo.exception.PastDueModificationException;
import com.example.todo.exception.TodoNotFoundException;
import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Spy
    private TodoMapper todoMapper;

    @InjectMocks
    private TodoService todoService;

    private TodoItem notDoneItem;
    private TodoItem doneItem;
    private TodoItem pastDueItem;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        notDoneItem = TodoItem.builder()
                .id(1L)
                .description("Buy groceries")
                .status(TodoStatus.NOT_DONE)
                .createdAt(now.minusHours(1))
                .dueAt(now.plusDays(1))
                .build();

        doneItem = TodoItem.builder()
                .id(2L)
                .description("Read a book")
                .status(TodoStatus.DONE)
                .createdAt(now.minusHours(2))
                .dueAt(now.plusDays(1))
                .doneAt(now.minusMinutes(30))
                .build();

        pastDueItem = TodoItem.builder()
                .id(3L)
                .description("Old task")
                .status(TodoStatus.PAST_DUE)
                .createdAt(now.minusDays(2))
                .dueAt(now.minusDays(1))
                .build();
    }

    @Nested
    @DisplayName("createTodo")
    class CreateTodo {

        @Test
        @DisplayName("should create a new NOT_DONE item and return its response")
        void shouldCreateItem() {
            CreateTodoRequest request = new CreateTodoRequest();
            request.setDescription("New task");
            request.setDueAt(LocalDateTime.now().plusDays(3));

            when(todoRepository.save(any(TodoItem.class))).thenAnswer(inv -> {
                TodoItem saved = inv.getArgument(0);
                saved.setId(10L);
                saved.setCreatedAt(LocalDateTime.now());
                return saved;
            });

            TodoItemResponse response = todoService.createTodo(request);

            assertThat(response.getId()).isEqualTo(10L);
            assertThat(response.getDescription()).isEqualTo("New task");
            assertThat(response.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
            assertThat(response.getDoneAt()).isNull();
            verify(todoRepository).save(any(TodoItem.class));
        }
    }

    @Nested
    @DisplayName("updateDescription")
    class UpdateDescription {

        @Test
        @DisplayName("should update description of a NOT_DONE item")
        void shouldUpdateDescription() {
            UpdateDescriptionRequest request = new UpdateDescriptionRequest();
            request.setDescription("Updated description");

            when(todoRepository.findById(1L)).thenReturn(Optional.of(notDoneItem));
            when(todoRepository.save(any())).thenReturn(notDoneItem);

            TodoItemResponse response = todoService.updateDescription(1L, request);

            assertThat(response.getDescription()).isEqualTo("Updated description");
        }

        @Test
        @DisplayName("should throw TodoNotFoundException when item does not exist")
        void shouldThrowWhenNotFound() {
            when(todoRepository.findById(99L)).thenReturn(Optional.empty());
            UpdateDescriptionRequest request = new UpdateDescriptionRequest();
            request.setDescription("X");

            assertThatThrownBy(() -> todoService.updateDescription(99L, request))
                    .isInstanceOf(TodoNotFoundException.class);
        }

        @Test
        @DisplayName("should throw PastDueModificationException for past due item")
        void shouldRejectPastDueItem() {
            when(todoRepository.findById(3L)).thenReturn(Optional.of(pastDueItem));
            UpdateDescriptionRequest request = new UpdateDescriptionRequest();
            request.setDescription("X");

            assertThatThrownBy(() -> todoService.updateDescription(3L, request))
                    .isInstanceOf(PastDueModificationException.class);
        }
    }

    @Nested
    @DisplayName("markAsDone")
    class MarkAsDone {

        @Test
        @DisplayName("should mark a NOT_DONE item as DONE and set doneAt")
        void shouldMarkAsDone() {
            when(todoRepository.findById(1L)).thenReturn(Optional.of(notDoneItem));
            when(todoRepository.save(any())).thenReturn(notDoneItem);

            TodoItemResponse response = todoService.markAsDone(1L);

            assertThat(response.getStatus()).isEqualTo(TodoStatus.DONE);
            assertThat(notDoneItem.getDoneAt()).isNotNull();
        }

        @Test
        @DisplayName("should reject marking a PAST_DUE item as done")
        void shouldRejectPastDueItem() {
            when(todoRepository.findById(3L)).thenReturn(Optional.of(pastDueItem));

            assertThatThrownBy(() -> todoService.markAsDone(3L))
                    .isInstanceOf(PastDueModificationException.class);
        }
    }

    @Nested
    @DisplayName("markAsNotDone")
    class MarkAsNotDone {

        @Test
        @DisplayName("should mark a DONE item as NOT_DONE and clear doneAt")
        void shouldMarkAsNotDone() {
            when(todoRepository.findById(2L)).thenReturn(Optional.of(doneItem));
            when(todoRepository.save(any())).thenReturn(doneItem);

            TodoItemResponse response = todoService.markAsNotDone(2L);

            assertThat(response.getStatus()).isEqualTo(TodoStatus.NOT_DONE);
            assertThat(doneItem.getDoneAt()).isNull();
        }

        @Test
        @DisplayName("should reject marking a PAST_DUE item as not done")
        void shouldRejectPastDueItem() {
            when(todoRepository.findById(3L)).thenReturn(Optional.of(pastDueItem));

            assertThatThrownBy(() -> todoService.markAsNotDone(3L))
                    .isInstanceOf(PastDueModificationException.class);
        }
    }

    @Nested
    @DisplayName("getItems")
    class GetItems {

        @Test
        @DisplayName("should return only NOT_DONE items by default")
        void shouldReturnNotDoneItems() {
            when(todoRepository.findByStatus(TodoStatus.NOT_DONE)).thenReturn(List.of(notDoneItem));

            List<TodoItemResponse> items = todoService.getItems(false);

            assertThat(items).hasSize(1);
            assertThat(items.get(0).getStatus()).isEqualTo(TodoStatus.NOT_DONE);
        }

        @Test
        @DisplayName("should return all items when includeAll is true")
        void shouldReturnAllItems() {
            when(todoRepository.findAll()).thenReturn(List.of(notDoneItem, doneItem, pastDueItem));

            List<TodoItemResponse> items = todoService.getItems(true);

            assertThat(items).hasSize(3);
        }
    }

    @Nested
    @DisplayName("getItem")
    class GetItem {

        @Test
        @DisplayName("should return the item details for a valid id")
        void shouldReturnItem() {
            when(todoRepository.findById(1L)).thenReturn(Optional.of(notDoneItem));

            TodoItemResponse response = todoService.getItem(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getDescription()).isEqualTo("Buy groceries");
        }

        @Test
        @DisplayName("should throw TodoNotFoundException for unknown id")
        void shouldThrowForUnknownId() {
            when(todoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> todoService.getItem(99L))
                    .isInstanceOf(TodoNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }
}
