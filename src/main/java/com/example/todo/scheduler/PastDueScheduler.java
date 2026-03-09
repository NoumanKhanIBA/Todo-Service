package com.example.todo.scheduler;

import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import com.example.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PastDueScheduler {

    private final TodoRepository todoRepository;

    @Scheduled(cron = "${app.scheduler.past-due-cron:0 * * * * *}")
    @Transactional
    public void markPastDueItems() {
        List<TodoItem> overdueItems = todoRepository
                .findByStatusAndDueAtBefore(TodoStatus.NOT_DONE, LocalDateTime.now());

        if (overdueItems.isEmpty()) {
            return;
        }

        overdueItems.forEach(item -> item.setStatus(TodoStatus.PAST_DUE));
        todoRepository.saveAll(overdueItems);

        log.info("Marked {} todo item(s) as PAST_DUE: ids={}",
                overdueItems.size(),
                overdueItems.stream().map(TodoItem::getId).toList());
    }
}
