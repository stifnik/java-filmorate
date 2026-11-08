package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {
    private UserController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new UserController(new UserService(new InMemoryUserStorage()));
        user = newUser();
    }

    @Test
    void shouldCreateUsersWithUniqueIdsAndReturnAll() {
        assertTrue(controller.getAll().isEmpty());
        user.setId(100L);
        User first = controller.create(user);
        User second = controller.create(newUser());

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
        assertEquals("Имя пользователя", first.getName());
        assertEquals(2, controller.getAll().size());
        assertTrue(controller.getAll().contains(first));
        assertTrue(controller.getAll().contains(second));
    }

    @Test
    void shouldUpdateExistingUser() {
        Long id = controller.create(user).getId();
        User update = newUser();
        update.setId(id);
        update.setEmail("updated@example.com");
        update.setLogin("updated");
        update.setName("Новое имя");
        update.setBirthday(LocalDate.of(1990, 1, 1));

        assertEquals(update, controller.update(update));
        assertEquals(1, controller.getAll().size());
        assertEquals(update, controller.getAll().iterator().next());
    }

    @Test
    void shouldUseLoginForMissingEmptyOrBlankNameOnCreate() {
        for (String name : new String[]{null, "", " \t\n"}) {
            User newUser = newUser();
            newUser.setName(name);
            assertEquals(newUser.getLogin(), controller.create(newUser).getName());
        }
    }

    @Test
    void shouldUseUpdatedLoginForMissingEmptyOrBlankNameOnUpdate() {
        Long id = controller.create(user).getId();
        for (String name : new String[]{null, "", " \t\n"}) {
            User update = newUser();
            update.setId(id);
            update.setLogin("updated");
            update.setName(name);
            assertEquals("updated", controller.update(update).getName());
        }
        assertEquals(1, controller.getAll().size());
        assertEquals("updated", controller.getAll().iterator().next().getName());
    }

    @Test
    void shouldRejectUpdateWithoutIdAndKeepStoredUser() {
        controller.create(user);
        User update = newUser();
        update.setName("Новое имя");

        assertThrows(ValidationException.class, () -> controller.update(update));
        assertEquals(user, controller.getAll().iterator().next());
    }

    @Test
    void shouldRejectUpdateOfUnknownUser() {
        user.setId(100L);

        assertThrows(NotFoundException.class, () -> controller.update(user));
        assertTrue(controller.getAll().isEmpty());
    }

    private User newUser() {
        User result = new User();
        result.setEmail("user@example.com");
        result.setLogin("user");
        result.setName("Имя пользователя");
        result.setBirthday(LocalDate.of(2000, 1, 1));
        return result;
    }
}
