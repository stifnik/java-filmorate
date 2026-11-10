package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStorageTest {
    @Test
    void shouldReturnOptionalFilmWhenFoundAndEmptyWhenMissing() {
        FilmStorage storage = new InMemoryFilmStorage();
        assertTrue(storage.getById(1L).isEmpty());

        Film film = storage.create(new Film());

        assertEquals(Optional.of(film), storage.getById(film.getId()));
    }

    @Test
    void shouldReturnOptionalUserWhenFoundAndEmptyWhenMissing() {
        UserStorage storage = new InMemoryUserStorage();
        assertTrue(storage.getById(1L).isEmpty());

        User user = storage.create(new User());

        assertEquals(Optional.of(user), storage.getById(user.getId()));
    }

    @Test
    void shouldDeleteFilmWithoutReusingItsId() {
        FilmStorage storage = new InMemoryFilmStorage();
        Film film = storage.create(new Film());
        long id = film.getId();
        storage.delete(id);

        assertTrue(storage.getAll().isEmpty());
        assertTrue(storage.getById(id).isEmpty());
        storage.delete(id);
        assertEquals(id + 1, storage.create(new Film()).getId());
    }

    @Test
    void shouldDeleteUserWithoutReusingItsId() {
        UserStorage storage = new InMemoryUserStorage();
        User user = storage.create(new User());
        long id = user.getId();
        storage.delete(id);

        assertTrue(storage.getAll().isEmpty());
        assertTrue(storage.getById(id).isEmpty());
        storage.delete(id);
        assertEquals(id + 1, storage.create(new User()).getId());
    }

    @Test
    void shouldSortAndLimitPopularFilmsInStorage() {
        FilmStorage storage = new InMemoryFilmStorage();
        Film first = storage.create(new Film());
        Film second = storage.create(new Film());
        Film third = storage.create(new Film());
        first.getLikes().add(1L);
        first.getLikes().add(2L);
        third.getLikes().add(1L);

        assertEquals(List.of(first, third), storage.getPopular(2));
        assertEquals(List.of(first, third, second), storage.getPopular(10));
        assertTrue(storage.getPopular(0).isEmpty());
    }
}
