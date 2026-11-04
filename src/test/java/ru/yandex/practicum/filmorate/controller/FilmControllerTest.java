package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilmControllerTest {
    private FilmController controller;
    private Film film;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
        film = newFilm();
    }

    @Test
    void shouldCreateFilmsWithUniqueIdsAndReturnAll() {
        assertTrue(controller.getAll().isEmpty());
        film.setId(100L);
        Film first = controller.create(film);
        Film second = controller.create(newFilm());

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
        assertEquals(2, controller.getAll().size());
        assertTrue(controller.getAll().contains(first));
        assertTrue(controller.getAll().contains(second));
    }

    @Test
    void shouldUpdateExistingFilm() {
        Long id = controller.create(film).getId();
        Film update = newFilm();
        update.setId(id);
        update.setName("Новое название");
        update.setDescription("Новое описание");
        update.setReleaseDate(LocalDate.of(2000, 1, 1));
        update.setDuration(120);

        assertEquals(update, controller.update(update));
        assertEquals(1, controller.getAll().size());
        assertEquals(update, controller.getAll().iterator().next());
    }

    @Test
    void shouldRejectUpdateWithoutIdAndKeepStoredFilm() {
        controller.create(film);
        Film update = newFilm();
        update.setName("Новое название");

        assertThrows(ValidationException.class, () -> controller.update(update));
        assertEquals(film, controller.getAll().iterator().next());
    }

    @Test
    void shouldRejectUpdateOfUnknownFilm() {
        film.setId(100L);

        assertThrows(NotFoundException.class, () -> controller.update(film));
        assertTrue(controller.getAll().isEmpty());
    }

    @Test
    void shouldAcceptReleaseDatesAtOrAfterBoundary() {
        LocalDate[] dates = {LocalDate.of(1895, 12, 28), LocalDate.of(1895, 12, 29),
                LocalDate.now().plusYears(1)};
        for (LocalDate date : dates) {
            Film validFilm = newFilm();
            validFilm.setReleaseDate(date);
            assertEquals(date, controller.create(validFilm).getReleaseDate());
        }
    }

    @Test
    void shouldRejectEarlyReleaseDateWithoutStoringFilmOrUsingId() {
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        assertThrows(ValidationException.class, () -> controller.create(film));
        assertTrue(controller.getAll().isEmpty());
        film.setReleaseDate(LocalDate.of(1895, 12, 28));

        assertEquals(1L, controller.create(film).getId());
    }

    @Test
    void shouldRejectEarlyReleaseDateOnUpdateAndKeepStoredFilm() {
        Long id = controller.create(film).getId();
        Film update = newFilm();
        update.setId(id);
        update.setReleaseDate(LocalDate.of(1895, 12, 27));

        assertThrows(ValidationException.class, () -> controller.update(update));
        assertEquals(1, controller.getAll().size());
        assertEquals(film, controller.getAll().iterator().next());
    }

    private Film newFilm() {
        Film result = new Film();
        result.setName("Прибытие поезда");
        result.setDescription("Короткометражный фильм");
        result.setReleaseDate(LocalDate.of(1896, 1, 6));
        result.setDuration(1);
        return result;
    }
}
