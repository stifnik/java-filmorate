package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.practicum.filmorate.storage.TestData.genreStorage;
import static ru.yandex.practicum.filmorate.storage.TestData.mpaStorage;

class CatalogControllerTest {
    @Test
    void shouldReturnAllGenresFindByIdAndRejectUnknownId() {
        GenreController controller = new GenreController(new GenreService(genreStorage()));

        assertThat(controller.getAll()).extracting(Genre::getId).containsExactly(1, 2, 3, 4, 5, 6);
        for (Genre genre : controller.getAll()) {
            assertThat(controller.getById(genre.getId())).isEqualTo(genre);
        }
        assertThatThrownBy(() -> controller.getById(999)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldReturnFiveAmericanRatingsFindByIdAndRejectUnknownId() {
        MpaController controller = new MpaController(new MpaService(mpaStorage()));

        assertThat(controller.getAll()).extracting(Mpa::getId).containsExactly(1, 2, 3, 4, 5);
        assertThat(controller.getAll()).extracting(Mpa::getName).containsExactly("G", "PG", "PG-13", "R", "NC-17");
        for (Mpa mpa : controller.getAll()) {
            assertThat(controller.getById(mpa.getId())).isEqualTo(mpa);
        }
        assertThatThrownBy(() -> controller.getById(999)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> controller.getById(0)).isInstanceOf(NotFoundException.class);
    }
}
