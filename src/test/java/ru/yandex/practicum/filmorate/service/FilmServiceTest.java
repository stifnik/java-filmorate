package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.practicum.filmorate.storage.TestData.genreStorage;
import static ru.yandex.practicum.filmorate.storage.TestData.mpaStorage;

class FilmServiceTest {
    private FilmService service;
    private User user;
    private User otherUser;
    private Film film;

    @BeforeEach
    void setUp() {
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        UserService userService = new UserService(userStorage);
        user = userService.create(newUser());
        otherUser = userService.create(newUser());
        service = new FilmService(new InMemoryFilmStorage(), userStorage, genreStorage(), mpaStorage());
        film = service.create(newFilm());
    }

    @Test
    void shouldCountEachUsersLikeOnlyOnce() {
        service.addLike(film.getId(), user.getId());
        service.addLike(film.getId(), user.getId());
        service.addLike(film.getId(), otherUser.getId());

        assertEquals(Set.of(user.getId(), otherUser.getId()), service.getById(film.getId()).getLikes());
    }

    @Test
    void shouldRemoveLikeAndKeepOtherUsersLikes() {
        service.addLike(film.getId(), user.getId());
        service.addLike(film.getId(), otherUser.getId());

        service.removeLike(film.getId(), user.getId());
        service.removeLike(film.getId(), user.getId());

        assertEquals(Set.of(otherUser.getId()), service.getById(film.getId()).getLikes());
    }

    @Test
    void shouldSortFilmsByLikesAndUpdateRankingAfterRemoval() {
        Film popularFilm = service.create(newFilm());
        service.addLike(film.getId(), user.getId());
        service.addLike(popularFilm.getId(), user.getId());
        service.addLike(popularFilm.getId(), otherUser.getId());

        assertEquals(List.of(popularFilm, film), service.getPopular(10));

        service.removeLike(popularFilm.getId(), user.getId());
        service.removeLike(popularFilm.getId(), otherUser.getId());

        assertEquals(List.of(film, popularFilm), service.getPopular(10));
    }

    @Test
    void shouldLimitPopularFilmsToRequestedCount() {
        Film popularFilm = service.create(newFilm());
        for (int i = 0; i < 10; i++) {
            service.create(newFilm());
        }
        service.addLike(popularFilm.getId(), user.getId());

        assertEquals(List.of(popularFilm, film), service.getPopular(2));
        assertEquals(10, service.getPopular(10).size());
        assertEquals(12, service.getPopular(20).size());
    }

    @Test
    void shouldReturnEmptyPopularityListWhenThereAreNoFilms() {
        FilmService emptyService = new FilmService(new InMemoryFilmStorage(), new InMemoryUserStorage(),
                genreStorage(), mpaStorage());

        assertTrue(emptyService.getPopular(10).isEmpty());
    }

    @Test
    void shouldRejectNonPositivePopularityCount() {
        assertThrows(ValidationException.class, () -> service.getPopular(0));
        assertThrows(ValidationException.class, () -> service.getPopular(-1));
    }

    @Test
    void shouldRejectUnknownFilmsAndUsersWithoutChangingLikes() {
        long missingId = 999L;
        service.addLike(film.getId(), user.getId());

        assertThrows(NotFoundException.class, () -> service.getById(missingId));
        assertThrows(NotFoundException.class, () -> service.addLike(film.getId(), missingId));
        assertThrows(NotFoundException.class, () -> service.addLike(missingId, user.getId()));
        assertThrows(NotFoundException.class, () -> service.removeLike(film.getId(), missingId));
        assertThrows(NotFoundException.class, () -> service.removeLike(missingId, user.getId()));
        assertEquals(Set.of(user.getId()), service.getById(film.getId()).getLikes());
    }

    @Test
    void shouldKeepLikesWhenUpdatingFilm() {
        service.addLike(film.getId(), user.getId());
        Film update = newFilm();
        update.setId(film.getId());
        update.setName("Новое название");
        update.setLikes(null);

        Film updatedFilm = service.update(update);

        assertEquals("Новое название", service.getById(film.getId()).getName());
        assertEquals(Set.of(user.getId()), updatedFilm.getLikes());
    }

    @Test
    void shouldStoreSeveralGenresAndMpaAndReplaceThemOnUpdate() {
        Film newFilm = newFilm();
        newFilm.setGenres(Set.of(new Genre(1, "Комедия"), new Genre(2, "Драма")));
        newFilm.setMpa(new Mpa(2, "PG"));
        Film createdFilm = service.create(newFilm);
        service.addLike(createdFilm.getId(), user.getId());

        assertEquals(newFilm.getGenres(), service.getById(createdFilm.getId()).getGenres());
        assertEquals(new Mpa(2, "PG"), service.getById(createdFilm.getId()).getMpa());

        Film update = newFilm();
        update.setId(createdFilm.getId());
        update.setGenres(Set.of(new Genre(4, "Триллер")));
        update.setMpa(new Mpa(4, "R"));
        service.update(update);

        assertEquals(Set.of(new Genre(4, "Триллер")), service.getById(createdFilm.getId()).getGenres());
        assertEquals(new Mpa(4, "R"), service.getById(createdFilm.getId()).getMpa());
        assertEquals(Set.of(user.getId()), service.getById(createdFilm.getId()).getLikes());
    }

    @Test
    void shouldResolveGenreAndMpaNamesFromTheirIds() {
        Film request = newFilm();
        request.setGenres(Set.of(new Genre(3, "ignored"), new Genre(1, null)));
        request.setMpa(new Mpa(2, "ignored"));

        Film created = service.create(request);

        assertEquals(List.of(1, 3), created.getGenres().stream().map(Genre::getId).toList());
        assertEquals(List.of("Комедия", "Мультфильм"), created.getGenres().stream().map(Genre::getName).toList());
        assertEquals(new Mpa(2, "PG"), created.getMpa());
    }

    @Test
    void shouldRejectUnknownGenreAndMpaBeforeCreatingFilm() {
        Film request = newFilm();
        request.setGenres(Set.of(new Genre(1, null), new Genre(999, null)));
        assertThrows(NotFoundException.class, () -> service.create(request));

        request.setGenres(Set.of());
        request.setMpa(new Mpa(999, null));
        assertThrows(NotFoundException.class, () -> service.create(request));

        assertEquals(List.of(film), List.copyOf(service.getAll()));
        assertEquals(film.getId() + 1, service.create(newFilm()).getId());
    }

    @Test
    void shouldRejectInvalidReferencesBeforeChangingStoredFilmOrLikes() {
        service.addLike(film.getId(), user.getId());
        Film update = newFilm();
        update.setId(film.getId());
        update.setName("Changed");
        update.setGenres(Set.of(new Genre(1, null), new Genre(999, null)));
        assertThrows(NotFoundException.class, () -> service.update(update));

        update.setGenres(Set.of(new Genre(1, null)));
        update.setMpa(new Mpa(999, null));
        assertThrows(NotFoundException.class, () -> service.update(update));

        assertEquals(film, service.getById(film.getId()));
        assertEquals("Фильм", service.getById(film.getId()).getName());
        assertTrue(service.getById(film.getId()).getGenres().isEmpty());
        assertEquals(Set.of(user.getId()), service.getById(film.getId()).getLikes());
    }

    @Test
    void shouldRejectMissingReferenceIdsAndNullGenre() {
        Film request = newFilm();
        request.setGenres(Set.of(new Genre(null, "Комедия")));
        assertThrows(ValidationException.class, () -> service.create(request));

        Set<Genre> genres = new HashSet<>();
        genres.add(null);
        request.setGenres(genres);
        assertThrows(ValidationException.class, () -> service.create(request));

        request.setGenres(Set.of());
        request.setMpa(new Mpa(null, "G"));
        assertThrows(ValidationException.class, () -> service.create(request));
        assertEquals(1, service.getAll().size());
    }

    @Test
    void shouldAllowClearingGenresAndMpaWithoutChangingLikes() {
        Film request = newFilm();
        request.setGenres(Set.of(new Genre(1, null)));
        request.setMpa(new Mpa(1, null));
        Film created = service.create(request);
        service.addLike(created.getId(), user.getId());
        Film update = newFilm();
        update.setId(created.getId());
        update.setGenres(null);

        Film updated = service.update(update);

        assertTrue(updated.getGenres().isEmpty());
        assertEquals(null, updated.getMpa());
        assertEquals(Set.of(user.getId()), updated.getLikes());
    }

    @Test
    void shouldCreateFilmWithoutSuppliedLikes() {
        Film newFilm = newFilm();
        newFilm.setLikes(Set.of(user.getId()));

        Film createdFilm = service.create(newFilm);

        assertTrue(service.getById(createdFilm.getId()).getLikes().isEmpty());
    }

    @Test
    void shouldDeleteFilmAndRejectDeletingUnknownFilm() {
        Film otherFilm = service.create(newFilm());

        service.delete(film.getId());

        assertThrows(NotFoundException.class, () -> service.getById(film.getId()));
        assertThrows(NotFoundException.class, () -> service.delete(film.getId()));
        assertEquals(List.of(otherFilm), service.getPopular(10));
    }

    private Film newFilm() {
        Film result = new Film();
        result.setName("Фильм");
        result.setDescription("Описание");
        result.setReleaseDate(LocalDate.of(2000, 1, 1));
        result.setDuration(100);
        return result;
    }

    private User newUser() {
        User result = new User();
        result.setEmail("user@example.com");
        result.setLogin("user");
        result.setBirthday(LocalDate.of(2000, 1, 1));
        return result;
    }
}
