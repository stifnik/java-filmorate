package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        setDisplayName(user);
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь с id = {}", user.getId());
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        if (user.getId() == null) {
            log.warn("Невозможно обновить пользователя: id не указан");
            throw new ValidationException("Id пользователя должен быть указан");
        }
        if (!users.containsKey(user.getId())) {
            log.warn("Невозможно обновить пользователя: id = {} не найден", user.getId());
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        setDisplayName(user);
        users.put(user.getId(), user);
        log.info("Обновлён пользователь с id = {}", user.getId());
        return user;
    }

    @GetMapping
    public Collection<User> getAll() {
        return new ArrayList<>(users.values());
    }

    private void setDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
