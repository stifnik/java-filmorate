package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.time.LocalDate;
import java.util.Set;
import java.util.List;
import java.util.Optional;

public final class TestData {
    private TestData() {
    }

    public static User newUser(String login) {
        User user = new User();
        user.setLogin(login);
        user.setEmail(login + "@example.com");
        user.setName(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }

    public static Film newFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        film.setMpa(new Mpa(1, null));
        film.setGenres(Set.of(new Genre(1, null), new Genre(2, null)));
        return film;
    }

    public static GenreStorage genreStorage() {
        return new GenreStorage() {
            @Override
            public List<Genre> getAll() {
                return List.of(new Genre(1, "Комедия"), new Genre(2, "Драма"), new Genre(3, "Мультфильм"),
                        new Genre(4, "Триллер"), new Genre(5, "Документальный"), new Genre(6, "Боевик"));
            }

            @Override
            public Optional<Genre> getById(int id) {
                return getAll().stream().filter(genre -> genre.getId() == id).findFirst();
            }

            @Override
            public List<Genre> getByIds(Set<Integer> ids) {
                return getAll().stream().filter(genre -> ids.contains(genre.getId())).toList();
            }
        };
    }

    public static MpaStorage mpaStorage() {
        return new MpaStorage() {
            @Override
            public List<Mpa> getAll() {
                return List.of(new Mpa(1, "G"), new Mpa(2, "PG"), new Mpa(3, "PG-13"),
                        new Mpa(4, "R"), new Mpa(5, "NC-17"));
            }

            @Override
            public Optional<Mpa> getById(int id) {
                return getAll().stream().filter(mpa -> mpa.getId() == id).findFirst();
            }
        };
    }
}
