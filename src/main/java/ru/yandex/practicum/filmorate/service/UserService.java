package ru.yandex.practicum.filmorate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User create(User user) {
        setDisplayName(user);
        user.setFriends(new HashSet<>());
        User createdUser = userStorage.create(user);
        log.info("Добавлен пользователь с id = {}", createdUser.getId());
        return createdUser;
    }

    public User update(User user) {
        setDisplayName(user);
        User updatedUser = userStorage.update(user);
        log.info("Обновлён пользователь с id = {}", updatedUser.getId());
        return updatedUser;
    }

    public User getById(long id) {
        return userStorage.getById(id);
    }

    public Collection<User> getAll() {
        return userStorage.getAll();
    }

    public void addFriend(long id, long friendId) {
        User user = userStorage.getById(id);
        User friend = userStorage.getById(friendId);
        if (id == friendId) {
            throw new ValidationException("Нельзя добавить себя в друзья");
        }
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
        log.debug("Пользователи {} и {} добавлены в друзья", id, friendId);
    }

    public void removeFriend(long id, long friendId) {
        User user = userStorage.getById(id);
        User friend = userStorage.getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
        log.debug("Дружба пользователей {} и {} удалена", id, friendId);
    }

    public List<User> getFriends(long id) {
        return findUsers(userStorage.getById(id).getFriends());
    }

    public List<User> getCommonFriends(long id, long otherId) {
        User user = userStorage.getById(id);
        User otherUser = userStorage.getById(otherId);
        Set<Long> commonFriends = new HashSet<>(user.getFriends());
        commonFriends.retainAll(otherUser.getFriends());
        log.debug("Общих друзей у пользователей {} и {}: {}", id, otherId, commonFriends.size());
        return findUsers(commonFriends);
    }

    private List<User> findUsers(Set<Long> ids) {
        return ids.stream()
                .sorted()
                .map(userStorage::getById)
                .collect(Collectors.toList());
    }

    private void setDisplayName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
