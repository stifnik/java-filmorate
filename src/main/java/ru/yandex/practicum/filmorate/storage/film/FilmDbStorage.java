package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository("filmDbStorage")
public class FilmDbStorage extends BaseDbStorage<Film> implements FilmStorage {
    private static final String SELECT_FILMS = "SELECT f.*, m.name AS mpa_name "
            + "FROM films f LEFT JOIN mpa m ON m.id = f.mpa_id";
    private static final String FIND_ALL_QUERY = SELECT_FILMS + " ORDER BY f.id";
    private static final String FIND_BY_ID_QUERY = SELECT_FILMS + " WHERE f.id = ?";
    private static final String FIND_POPULAR_QUERY = SELECT_FILMS + " LEFT JOIN film_likes fl ON fl.film_id = f.id "
            + "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name "
            + "ORDER BY COUNT(fl.user_id) DESC, f.id LIMIT ?";
    private static final String INSERT_QUERY = "INSERT INTO films (name, description, release_date, duration, mpa_id) "
            + "VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE films SET name = ?, description = ?, release_date = ?, "
            + "duration = ?, mpa_id = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM films WHERE id = ?";
    private static final String FIND_GENRES_FOR_FILMS_QUERY = "SELECT fg.film_id, g.id, g.name FROM film_genres fg "
            + "JOIN genres g ON g.id = fg.genre_id WHERE fg.film_id IN (%s) ORDER BY fg.film_id, g.id";
    private static final String INSERT_GENRE_QUERY = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
    private static final String DELETE_GENRES_QUERY = "DELETE FROM film_genres WHERE film_id = ?";
    private static final String FIND_LIKES_FOR_FILMS_QUERY = "SELECT film_id, user_id FROM film_likes "
            + "WHERE film_id IN (%s)";
    private static final String INSERT_LIKE_QUERY = "INSERT INTO film_likes (film_id, user_id) SELECT ?, ? "
            + "WHERE (SELECT COUNT(*) FROM film_likes WHERE film_id = ? AND user_id = ?) = 0";
    private static final String DELETE_LIKE_QUERY = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";

    public FilmDbStorage(JdbcTemplate jdbc, FilmRowMapper mapper) {
        super(jdbc, mapper);
    }

    @Override
    public Film create(Film film) {
        Integer mpaId = film.getMpa() == null ? null : film.getMpa().getId();
        long id = insert(INSERT_QUERY,
                film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), mpaId);
        film.setId(id);
        saveGenres(id, film.getGenres());
        return getById(id).orElseThrow();
    }

    @Override
    public Film update(Film film) {
        Integer mpaId = film.getMpa() == null ? null : film.getMpa().getId();
        int updated = jdbc.update(UPDATE_QUERY,
                film.getName(), film.getDescription(), film.getReleaseDate(), film.getDuration(), mpaId, film.getId());
        if (updated == 0) {
            throw new NotFoundException("Фильм с id = " + film.getId() + " не найден");
        }
        jdbc.update(DELETE_GENRES_QUERY, film.getId());
        saveGenres(film.getId(), film.getGenres());
        return getById(film.getId()).orElseThrow();
    }

    @Override
    public Optional<Film> getById(long id) {
        return findOne(FIND_BY_ID_QUERY, id).map(film -> {
            loadRelations(List.of(film));
            return film;
        });
    }

    @Override
    public List<Film> getAll() {
        List<Film> films = findMany(FIND_ALL_QUERY);
        loadRelations(films);
        return films;
    }

    @Override
    public List<Film> getPopular(int count) {
        List<Film> films = findMany(FIND_POPULAR_QUERY, count);
        loadRelations(films);
        return films;
    }

    @Override
    public void addLike(long id, long userId) {
        jdbc.update(INSERT_LIKE_QUERY, id, userId, id, userId);
    }

    @Override
    public void removeLike(long id, long userId) {
        jdbc.update(DELETE_LIKE_QUERY, id, userId);
    }

    @Override
    public void delete(long id) {
        jdbc.update(DELETE_QUERY, id);
    }

    private void loadRelations(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Long, Film> filmsById = new HashMap<>();
        for (Film film : films) {
            film.setGenres(new LinkedHashSet<>());
            film.setLikes(new HashSet<>());
            filmsById.put(film.getId(), film);
        }
        Object[] ids = filmsById.keySet().toArray();
        String placeholders = String.join(", ", Collections.nCopies(ids.length, "?"));
        SqlRowSet genres = jdbc.queryForRowSet(String.format(FIND_GENRES_FOR_FILMS_QUERY, placeholders), ids);
        while (genres.next()) {
            Film film = filmsById.get(genres.getLong("film_id"));
            film.getGenres().add(new Genre(genres.getInt("id"), genres.getString("name")));
        }
        SqlRowSet likes = jdbc.queryForRowSet(String.format(FIND_LIKES_FOR_FILMS_QUERY, placeholders), ids);
        while (likes.next()) {
            filmsById.get(likes.getLong("film_id")).getLikes().add(likes.getLong("user_id"));
        }
    }

    private void saveGenres(long filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Object[]> params = new ArrayList<>();
        for (Genre genre : genres) {
            params.add(new Object[]{filmId, genre.getId()});
        }
        jdbc.batchUpdate(INSERT_GENRE_QUERY, params);
    }
}
