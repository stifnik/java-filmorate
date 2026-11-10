package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

@Service
public class FilmService {
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film create(Film film) {
        validateReleaseDate(film);
        film.setLikes(new HashSet<>());
        Film createdFilm = filmStorage.create(film);
        log.info("Добавлен фильм с id = {}", createdFilm.getId());
        return createdFilm;
    }

    public Film update(Film film) {
        validateReleaseDate(film);
        if (film.getId() == null) {
            throw new ValidationException("Id фильма должен быть указан");
        }
        Film storedFilm = getById(film.getId());
        film.setLikes(storedFilm.getLikes());
        Film updatedFilm = filmStorage.update(film);
        log.info("Обновлён фильм с id = {}", updatedFilm.getId());
        return updatedFilm;
    }

    public Film getById(long id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Collection<Film> getAll() {
        return filmStorage.getAll();
    }

    public void delete(long id) {
        getById(id);
        filmStorage.delete(id);
        log.info("Удалён фильм с id = {}", id);
    }

    public void addLike(long id, long userId) {
        Film film = getById(id);
        validateUserExists(userId);
        film.getLikes().add(userId);
        log.debug("Пользователь {} поставил лайк фильму {}. Всего лайков: {}", userId, id, film.getLikes().size());
    }

    public void removeLike(long id, long userId) {
        Film film = getById(id);
        validateUserExists(userId);
        film.getLikes().remove(userId);
        log.debug("Пользователь {} удалил лайк у фильма {}. Всего лайков: {}", userId, id, film.getLikes().size());
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Количество фильмов должно быть больше нуля");
        }
        log.debug("Запрошено {} наиболее популярных фильмов", count);
        return filmStorage.getPopular(count);
    }

    private void validateUserExists(long userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года");
        }
    }
}
