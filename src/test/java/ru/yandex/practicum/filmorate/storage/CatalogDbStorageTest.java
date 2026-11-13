package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class CatalogDbStorageTest {
    private final JdbcTemplate jdbc;
    private GenreDbStorage genres;
    private MpaDbStorage mpa;

    @BeforeEach
    void setUp() {
        genres = new GenreDbStorage(jdbc, new GenreRowMapper());
        mpa = new MpaDbStorage(jdbc, new MpaRowMapper());
    }

    @Test
    void shouldReturnAllGenresInIdOrderAndFindEveryGenre() {
        assertThat(genres.getAll()).extracting(Genre::getId).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(genres.getAll()).extracting(Genre::getName)
                .containsExactly("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
        for (Genre genre : genres.getAll()) {
            assertThat(genres.getById(genre.getId())).contains(genre);
        }
        assertThat(genres.getById(0)).isEmpty();
        assertThat(genres.getById(999)).isEmpty();
    }

    @Test
    void shouldFindRequestedGenresInIdOrderAndSkipMissingIds() {
        assertThat(genres.getByIds(Set.of(4, 1, 3)))
                .containsExactly(new Genre(1, "Комедия"), new Genre(3, "Мультфильм"), new Genre(4, "Триллер"));
        assertThat(genres.getByIds(Set.of(2, 999))).containsExactly(new Genre(2, "Драма"));
        assertThat(genres.getByIds(Set.of(999))).isEmpty();
        assertThat(genres.getByIds(Set.of())).isEmpty();
    }

    @Test
    void shouldReturnExactlyFiveAmericanRatingsAndFindEveryRating() {
        assertThat(mpa.getAll()).extracting(Mpa::getId).containsExactly(1, 2, 3, 4, 5);
        assertThat(mpa.getAll()).extracting(Mpa::getName).containsExactly("G", "PG", "PG-13", "R", "NC-17");
        for (Mpa rating : mpa.getAll()) {
            assertThat(mpa.getById(rating.getId())).contains(rating);
        }
        assertThat(mpa.getById(-1)).isEmpty();
        assertThat(mpa.getById(999)).isEmpty();
    }

    @Test
    void shouldApplyInitializationScriptsRepeatedlyWithoutDuplicatingCatalogs() throws IOException {
        for (int i = 0; i < 2; i++) {
            executeScript("schema.sql");
            executeScript("data.sql");
        }

        assertThat(genres.getAll()).hasSize(6);
        assertThat(mpa.getAll()).hasSize(5);
    }

    private void executeScript(String fileName) throws IOException {
        String sql = Files.readString(Path.of("src/main/resources", fileName));
        for (String query : sql.split(";")) {
            if (!query.isBlank()) {
                jdbc.execute(query);
            }
        }
    }
}
