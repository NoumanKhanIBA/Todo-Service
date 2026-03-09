package com.example.todo.repository;

import com.example.todo.model.TodoItem;
import com.example.todo.model.TodoStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TodoRepository extends JpaRepository<TodoItem, Long> {

    List<TodoItem> findByStatus(TodoStatus status);


    List<TodoItem> findByStatusAndDueAtBefore(TodoStatus status, LocalDateTime now);

}
