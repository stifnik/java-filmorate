package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

@Service
public class FilmService {
    private static final Logger log = LoggerFactory.getLogger(FilmService.class);
    private static final LocalDate EARLIEST_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       GenreStorage genreStorage, MpaStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Film create(Film film) {
        validateReleaseDate(film);
        prepareReferences(film);
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
        prepareReferences(film);
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
        getById(id);
        validateUserExists(userId);
        filmStorage.addLike(id, userId);
        log.debug("Пользователь {} поставил лайк фильму {}", userId, id);
    }

    public void removeLike(long id, long userId) {
        getById(id);
        validateUserExists(userId);
        filmStorage.removeLike(id, userId);
        log.debug("Пользователь {} удалил лайк у фильма {}", userId, id);
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

    private void prepareReferences(Film film) {
        Set<Genre> genres = validateGenres(film.getGenres());
        Mpa mpa = validateMpa(film.getMpa());
        film.setGenres(genres);
        film.setMpa(mpa);
    }

    private Set<Genre> validateGenres(Set<Genre> genres) {
        TreeMap<Integer, Genre> result = new TreeMap<>();
        if (genres != null) {
            Set<Integer> ids = new HashSet<>();
            for (Genre genre : genres) {
                if (genre == null || genre.getId() == null) {
                    throw new ValidationException("Идентификатор жанра должен быть указан");
                }
                ids.add(genre.getId());
            }
            for (Genre storedGenre : genreStorage.getByIds(ids)) {
                result.put(storedGenre.getId(), storedGenre);
            }
            for (Integer id : ids) {
                if (!result.containsKey(id)) {
                    throw new NotFoundException("Жанр с id = " + id + " не найден");
                }
            }
        }
        return new LinkedHashSet<>(result.values());
    }

    private Mpa validateMpa(Mpa mpa) {
        if (mpa == null) {
            return null;
        }
        if (mpa.getId() == null) {
            throw new ValidationException("Идентификатор рейтинга должен быть указан");
        }
        return mpaStorage.getById(mpa.getId())
                .orElseThrow(() -> new NotFoundException("Рейтинг с id = " + mpa.getId() + " не найден"));
    }
}
