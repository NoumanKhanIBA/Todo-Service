package com.example.todo.service;

import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import com.example.todo.repository.TodoRepository;
import com.example.todo.scheduler.PastDueScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SchedulerTest {

    @Autowired
    private PastDueScheduler pastDueScheduler;

    @Autowired
    private TodoRepository todoRepository;

    @Test
    @DisplayName("should transition overdue NOT_DONE items to PAST_DUE")
    void shouldMarkOverdueItems() {
        todoRepository.deleteAll();

        TodoItem overdue = todoRepository.save(TodoItem.builder()
                .description("Overdue task")
                .status(TodoStatus.NOT_DONE)
                .createdAt(LocalDateTime.now().minusDays(3))
                .dueAt(LocalDateTime.now().minusHours(1))
                .build());

        TodoItem future = todoRepository.save(TodoItem.builder()
                .description("Future task")
                .status(TodoStatus.NOT_DONE)
                .createdAt(LocalDateTime.now().minusHours(1))
                .dueAt(LocalDateTime.now().plusDays(1))
                .build());

        TodoItem done = todoRepository.save(TodoItem.builder()
                .description("Done task")
                .status(TodoStatus.DONE)
                .createdAt(LocalDateTime.now().minusDays(1))
                .dueAt(LocalDateTime.now().minusHours(2))
                .doneAt(LocalDateTime.now().minusHours(3))
                .build());

        pastDueScheduler.markPastDueItems();

        List<TodoItem> all = todoRepository.findAll();
        assertThat(all).extracting(i -> i.getId() + ":" + i.getStatus())
                .containsExactlyInAnyOrder(
                        overdue.getId() + ":PAST_DUE",
                        future.getId() + ":NOT_DONE",
                        done.getId() + ":DONE"
                );
    }
}
