package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        service = new FilmService(new InMemoryFilmStorage(), userStorage);
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
        FilmService emptyService = new FilmService(new InMemoryFilmStorage(), new InMemoryUserStorage());

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
    void shouldCreateFilmWithoutSuppliedLikes() {
        Film newFilm = newFilm();
        newFilm.setLikes(Set.of(user.getId()));

        Film createdFilm = service.create(newFilm);

        assertTrue(service.getById(createdFilm.getId()).getLikes().isEmpty());
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
