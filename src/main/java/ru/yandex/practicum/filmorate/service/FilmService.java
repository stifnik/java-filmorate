package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

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
        Film updatedFilm = filmStorage.update(film);
        log.info("Обновлён фильм с id = {}", updatedFilm.getId());
        return updatedFilm;
    }

    public Film getById(long id) {
        return filmStorage.getById(id);
    }

    public Collection<Film> getAll() {
        return filmStorage.getAll();
    }

    public void addLike(long id, long userId) {
        Film film = filmStorage.getById(id);
        userStorage.getById(userId);
        film.getLikes().add(userId);
        log.debug("Пользователь {} поставил лайк фильму {}. Всего лайков: {}", userId, id, film.getLikes().size());
    }

    public void removeLike(long id, long userId) {
        Film film = filmStorage.getById(id);
        userStorage.getById(userId);
        film.getLikes().remove(userId);
        log.debug("Пользователь {} удалил лайк у фильма {}. Всего лайков: {}", userId, id, film.getLikes().size());
    }

    public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Количество фильмов должно быть больше нуля");
        }
        log.debug("Запрошено {} наиболее популярных фильмов", count);
        return filmStorage.getAll().stream()
                .sorted(Comparator.comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparing(Film::getId))
                .limit(count)
                .collect(Collectors.toList());
    }

    private void validateReleaseDate(Film film) {
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(EARLIEST_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза должна быть не раньше 28 декабря 1895 года");
        }
    }
}
