package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.Set;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.practicum.filmorate.storage.TestData.newFilm;
import static ru.yandex.practicum.filmorate.storage.TestData.newUser;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class FilmDbStorageTest {
    private static final String COUNT_GENRES_QUERY = "SELECT COUNT(*) FROM film_genres WHERE film_id = ?";
    private static final String COUNT_LIKES_QUERY = "SELECT COUNT(*) FROM film_likes WHERE film_id = ?";

    private final JdbcTemplate jdbc;
    private FilmDbStorage films;
    private UserDbStorage users;

    @BeforeEach
    void setUp() {
        films = new FilmDbStorage(jdbc, new FilmRowMapper());
        users = new UserDbStorage(jdbc, new UserRowMapper());
    }

    @Test
    void shouldCreateReadAndListFilmsWithCanonicalGenresAndMpa() {
        Film request = newFilm("first");
        request.setId(999L);
        request.setLikes(Set.of(999L));
        Film first = films.create(request);
        Film second = films.create(newFilm("second"));

        assertThat(first.getId()).isPositive().isNotEqualTo(999L);
        assertThat(first.getLikes()).isEmpty();
        assertThat(first.getMpa()).isEqualTo(new Mpa(1, "G"));
        assertThat(first.getGenres()).extracting(Genre::getName).containsExactly("Комедия", "Драма");
        assertThat(films.getById(first.getId())).contains(first);
        assertThat(films.getById(999L)).isEmpty();
        assertThat(films.getAll()).containsExactly(first, second);
    }

    @Test
    void shouldReplaceAndClearGenresWithoutLosingLikes() {
        Film film = films.create(newFilm("first"));
        User user = users.create(newUser("user"));
        films.addLike(film.getId(), user.getId());
        Film update = newFilm("updated");
        update.setId(film.getId());
        update.setGenres(Set.of(new Genre(4, "ignored")));
        update.setMpa(new Mpa(3, "ignored"));

        Film actual = films.update(update);

        assertThat(actual.getName()).isEqualTo("updated");
        assertThat(actual.getGenres()).extracting(Genre::getId).containsExactly(4);
        assertThat(actual.getMpa()).isEqualTo(new Mpa(3, "PG-13"));
        assertThat(actual.getLikes()).containsExactly(user.getId());
        update.setGenres(null);
        update.setMpa(null);
        actual = films.update(update);
        assertThat(actual.getGenres()).isEmpty();
        assertThat(actual.getMpa()).isNull();
        assertThat(actual.getLikes()).containsExactly(user.getId());
    }

    @Test
    void shouldRejectUpdateOfMissingFilmWithoutChangingExistingFilms() {
        Film stored = films.create(newFilm("original"));
        Film update = newFilm("updated");
        update.setId(999L);

        assertThatThrownBy(() -> films.update(update)).isInstanceOf(NotFoundException.class);
        assertThat(films.getById(stored.getId())).contains(stored);
        assertThat(films.getAll()).containsExactly(stored);
    }

    @Test
    void shouldCountLikesOnceAndSortPopularFilmsWithoutMultiplyingGenres() {
        Film first = films.create(newFilm("first"));
        Film second = films.create(newFilm("second"));
        Film third = films.create(newFilm("third"));
        User one = users.create(newUser("one"));
        User two = users.create(newUser("two"));
        films.addLike(second.getId(), one.getId());
        films.addLike(second.getId(), one.getId());
        films.addLike(second.getId(), two.getId());
        films.addLike(third.getId(), one.getId());

        assertThat(films.getById(second.getId()).orElseThrow().getLikes()).hasSize(2);
        assertThat(films.getPopular(2)).extracting(Film::getId).containsExactly(second.getId(), third.getId());
        assertThat(films.getPopular(10)).extracting(Film::getId)
                .containsExactly(second.getId(), third.getId(), first.getId());

        films.removeLike(second.getId(), one.getId());
        films.removeLike(second.getId(), one.getId());
        films.removeLike(second.getId(), two.getId());

        assertThat(films.getPopular(10)).extracting(Film::getId)
                .containsExactly(third.getId(), first.getId(), second.getId());
        assertThat(films.getPopular(0)).isEmpty();
    }

    @Test
    void shouldLoadRelationsForSeveralFilmsWithoutMixingGenresAndLikes() {
        Film firstRequest = newFilm("first");
        firstRequest.setGenres(Set.of(new Genre(1, null)));
        Film first = films.create(firstRequest);
        Film secondRequest = newFilm("second");
        secondRequest.setGenres(Set.of(new Genre(4, null), new Genre(2, null)));
        secondRequest.setMpa(new Mpa(4, null));
        Film second = films.create(secondRequest);
        Film emptyRequest = newFilm("empty");
        emptyRequest.setGenres(Set.of());
        emptyRequest.setMpa(null);
        Film empty = films.create(emptyRequest);
        User one = users.create(newUser("one"));
        User two = users.create(newUser("two"));
        films.addLike(first.getId(), one.getId());
        films.addLike(second.getId(), one.getId());
        films.addLike(second.getId(), two.getId());

        List<Film> actual = films.getAll();

        assertThat(actual).extracting(Film::getId).containsExactly(first.getId(), second.getId(), empty.getId());
        assertThat(actual.get(0).getGenres()).extracting(Genre::getName).containsExactly("Комедия");
        assertThat(actual.get(0).getLikes()).containsExactly(one.getId());
        assertThat(actual.get(1).getGenres()).extracting(Genre::getName).containsExactly("Драма", "Триллер");
        assertThat(actual.get(1).getLikes()).containsExactlyInAnyOrder(one.getId(), two.getId());
        assertThat(actual.get(1).getMpa()).isEqualTo(new Mpa(4, "R"));
        assertThat(actual.get(2).getGenres()).isEmpty();
        assertThat(actual.get(2).getLikes()).isEmpty();
        assertThat(actual.get(2).getMpa()).isNull();
    }

    @Test
    void shouldKeepPopularityOrderAndLoadOnlySelectedFilmsWithTheirRelations() {
        Film firstRequest = newFilm("first");
        firstRequest.setGenres(Set.of(new Genre(1, null)));
        Film first = films.create(firstRequest);
        Film popularRequest = newFilm("popular");
        popularRequest.setGenres(Set.of(new Genre(4, null), new Genre(3, null)));
        Film popular = films.create(popularRequest);
        Film otherRequest = newFilm("other");
        otherRequest.setGenres(Set.of(new Genre(6, null)));
        films.create(otherRequest);
        User one = users.create(newUser("one"));
        User two = users.create(newUser("two"));
        films.addLike(first.getId(), one.getId());
        films.addLike(popular.getId(), one.getId());
        films.addLike(popular.getId(), two.getId());

        List<Film> actual = films.getPopular(2);

        assertThat(actual).extracting(Film::getId).containsExactly(popular.getId(), first.getId());
        assertThat(actual.get(0).getGenres()).extracting(Genre::getId).containsExactly(3, 4);
        assertThat(actual.get(0).getLikes()).containsExactlyInAnyOrder(one.getId(), two.getId());
        assertThat(actual.get(1).getGenres()).extracting(Genre::getId).containsExactly(1);
        assertThat(actual.get(1).getLikes()).containsExactly(one.getId());
    }

    @Test
    void shouldDeleteFilmWithGenreAndLikeRelations() {
        Film film = films.create(newFilm("first"));
        User user = users.create(newUser("user"));
        films.addLike(film.getId(), user.getId());

        films.delete(film.getId());
        films.delete(film.getId());

        assertThat(films.getById(film.getId())).isEmpty();
        assertThat(films.getAll()).isEmpty();
        assertThat(films.getPopular(10)).isEmpty();
        assertThat(jdbc.queryForObject(COUNT_GENRES_QUERY,
                Integer.class, film.getId())).isZero();
        assertThat(jdbc.queryForObject(COUNT_LIKES_QUERY,
                Integer.class, film.getId())).isZero();
    }
}
