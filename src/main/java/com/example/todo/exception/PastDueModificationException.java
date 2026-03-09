package com.example.todo.exception;

public class PastDueModificationException extends RuntimeException {
    public PastDueModificationException(Long id) {
        super("Todo item with id " + id + " is past due and cannot be modified");
    }
}
