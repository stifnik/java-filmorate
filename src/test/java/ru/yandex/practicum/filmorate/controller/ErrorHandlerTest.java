package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorHandlerTest {
    @Test
    void shouldHideInternalExceptionDetails() {
        ErrorHandler handler = new ErrorHandler();
        IllegalStateException exception = new IllegalStateException("Внутренние сведения об ошибке");

        ErrorResponse response = handler.handleUnexpected(exception);

        assertEquals("Произошла непредвиденная ошибка.", response.getError());
    }
}
