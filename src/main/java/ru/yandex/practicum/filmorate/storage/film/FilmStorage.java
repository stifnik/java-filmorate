package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film film);

    Optional<Film> getById(long id);

    Collection<Film> getAll();

    List<Film> getPopular(int count);

    void addLike(long id, long userId);

    void removeLike(long id, long userId);

    void delete(long id);
}
