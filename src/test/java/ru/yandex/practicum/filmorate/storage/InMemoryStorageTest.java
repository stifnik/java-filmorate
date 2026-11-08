package ru.yandex.practicum.filmorate.storage;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryStorageTest {
    @Test
    void shouldDeleteFilmWithoutReusingItsId() {
        FilmStorage storage = new InMemoryFilmStorage();
        Film film = storage.create(new Film());
        long id = film.getId();
        storage.delete(id);

        assertTrue(storage.getAll().isEmpty());
        assertThrows(NotFoundException.class, () -> storage.getById(id));
        assertThrows(NotFoundException.class, () -> storage.delete(id));
        assertEquals(id + 1, storage.create(new Film()).getId());
    }

    @Test
    void shouldDeleteUserWithoutReusingItsId() {
        UserStorage storage = new InMemoryUserStorage();
        User user = storage.create(new User());
        long id = user.getId();
        storage.delete(id);

        assertTrue(storage.getAll().isEmpty());
        assertThrows(NotFoundException.class, () -> storage.getById(id));
        assertThrows(NotFoundException.class, () -> storage.delete(id));
        assertEquals(id + 1, storage.create(new User()).getId());
    }
}
