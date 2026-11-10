package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserStorage userStorage;

    @Autowired
    public UserService(@Qualifier("userDbStorage") UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User create(User user) {
        setDisplayName(user);
        user.setFriends(new HashMap<>());
        User createdUser = userStorage.create(user);
        log.info("Добавлен пользователь с id = {}", createdUser.getId());
        return createdUser;
    }

    public User update(User user) {
        if (user.getId() == null) {
            throw new ValidationException("Id пользователя должен быть указан");
        }
        User storedUser = getById(user.getId());
        setDisplayName(user);
        user.setFriends(storedUser.getFriends());
        User updatedUser = userStorage.update(user);
        log.info("Обновлён пользователь с id = {}", updatedUser.getId());
        return updatedUser;
    }

    public User getById(long id) {
        return userStorage.getById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public void delete(long id) {
        getById(id);
        userStorage.delete(id);
        log.info("Удалён пользователь с id = {}", id);
    }

    public void addFriend(long id, long friendId) {
        getById(id);
        User friend = getById(friendId);
        validateDifferentUsers(id, friendId);
        if (friend.getFriends().containsKey(id)) {
            userStorage.saveFriendship(id, friendId, FriendshipStatus.CONFIRMED);
            userStorage.saveFriendship(friendId, id, FriendshipStatus.CONFIRMED);
            log.debug("Дружба пользователей {} и {} подтверждена", id, friendId);
        } else {
            userStorage.saveFriendship(id, friendId, FriendshipStatus.PENDING);
            log.debug("Пользователь {} отправил заявку в друзья пользователю {}", id, friendId);
        }
    }

    public void removeFriend(long id, long friendId) {
        getById(id);
        User friend = getById(friendId);
        validateDifferentUsers(id, friendId);
        userStorage.removeFriendship(id, friendId);
        if (friend.getFriends().containsKey(id)) {
            userStorage.saveFriendship(friendId, id, FriendshipStatus.PENDING);
        }
        log.debug("Пользователь {} удалил пользователя {} из друзей", id, friendId);
    }

    public List<User> getFriends(long id) {
        getById(id);
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(long id, long otherId) {
        getById(id);
        getById(otherId);
        validateDifferentUsers(id, otherId);
        return userStorage.getCommonFriends(id, otherId);
    }

    private void validateDifferentUsers(long id, long otherId) {
        if (id == otherId) {
            throw new ValidationException("Пользователи должны быть разными");
        }
    }

    private void setDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
