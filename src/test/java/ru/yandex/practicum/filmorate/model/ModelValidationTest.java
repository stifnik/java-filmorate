package ru.yandex.practicum.filmorate.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModelValidationTest {
    private ValidatorFactory validatorFactory;
    private Validator validator;
    private Film film;
    private User user;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(1);

        user = new User();
        user.setEmail("user@example.com");
        user.setLogin("user");
        user.setBirthday(LocalDate.now());
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void shouldAcceptValidModels() {
        assertTrue(validator.validate(film).isEmpty());
        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    void shouldRejectEmptyModels() {
        assertFalse(validator.validate(new Film()).isEmpty());
        assertFalse(validator.validate(new User()).isEmpty());
    }

    @Test
    void shouldRejectMissingAndBlankFilmName() {
        for (String name : new String[]{null, "", " \t\n"}) {
            film.setName(name);
            assertFalse(validator.validate(film).isEmpty());
        }
    }

    @Test
    void shouldValidateDescriptionLengthBoundary() {
        film.setDescription("а".repeat(200));
        assertTrue(validator.validate(film).isEmpty());
        film.setDescription("а".repeat(201));
        assertFalse(validator.validate(film).isEmpty());
    }

    @Test
    void shouldAllowMissingAndEmptyDescription() {
        film.setDescription(null);
        assertTrue(validator.validate(film).isEmpty());
        film.setDescription("");
        assertTrue(validator.validate(film).isEmpty());
    }

    @Test
    void shouldRejectMissingReleaseDate() {
        film.setReleaseDate(null);
        assertFalse(validator.validate(film).isEmpty());
    }

    @Test
    void shouldRejectMissingZeroAndNegativeDuration() {
        for (Integer duration : new Integer[]{null, 0, -1}) {
            film.setDuration(duration);
            assertFalse(validator.validate(film).isEmpty());
        }
    }

    @Test
    void shouldRejectMissingAndBlankEmail() {
        for (String email : new String[]{null, "", " \t\n"}) {
            user.setEmail(email);
            assertFalse(validator.validate(user).isEmpty());
        }
    }

    @Test
    void shouldRejectMalformedEmailEvenWithAtSign() {
        for (String email : new String[]{"mail.ru", "это-неправильный?эмейл@", "user@", "@example.com",
                "user@@example.com", "user name@example.com"}) {
            user.setEmail(email);
            assertFalse(validator.validate(user).isEmpty(), "Недопустимый email: " + email);
        }
    }

    @Test
    void shouldRejectMissingBlankAndWhitespaceInLogin() {
        for (String login : new String[]{null, "", " ", "user name", " user", "user ", "user\tname", "user\nname"}) {
            user.setLogin(login);
            assertFalse(validator.validate(user).isEmpty());
        }
    }

    @Test
    void shouldAllowMissingAndEmptyDisplayName() {
        user.setName(null);
        assertTrue(validator.validate(user).isEmpty());
        user.setName("");
        assertTrue(validator.validate(user).isEmpty());
    }

    @Test
    void shouldAcceptBirthdayTodayAndRejectTomorrow() {
        assertTrue(validator.validate(user).isEmpty());
        user.setBirthday(LocalDate.now().plusDays(1));
        assertFalse(validator.validate(user).isEmpty());
    }

    @Test
    void shouldRejectMissingBirthday() {
        user.setBirthday(null);
        assertFalse(validator.validate(user).isEmpty());
    }
}
