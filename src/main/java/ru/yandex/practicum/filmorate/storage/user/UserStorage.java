package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface UserStorage {
    User create(User user);

    User update(User user);

    Optional<User> getById(long id);

    Collection<User> getAll();

    void saveFriendship(long userId, long friendId, FriendshipStatus status);

    void removeFriendship(long userId, long friendId);

    List<User> getFriends(long id);

    List<User> getCommonFriends(long id, long otherId);

    void delete(long id);
}
