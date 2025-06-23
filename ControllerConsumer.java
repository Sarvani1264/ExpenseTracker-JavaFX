package com.example.expensetracker;

import java.io.IOException;

@FunctionalInterface
public interface ControllerConsumer<T> {
    void accept(T controller) throws IOException;
}
