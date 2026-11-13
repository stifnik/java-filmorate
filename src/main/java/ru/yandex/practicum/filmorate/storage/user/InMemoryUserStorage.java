package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

@Deprecated
@Component
public class InMemoryUserStorage implements UserStorage {
    private final Map<Long, User> users = new HashMap<>();
    private long nextId = 1;

    @Override
    public User create(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> getById(long id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Collection<User> getAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void saveFriendship(long userId, long friendId, FriendshipStatus status) {
        users.get(userId).getFriends().put(friendId, status);
    }

    @Override
    public void removeFriendship(long userId, long friendId) {
        users.get(userId).getFriends().remove(friendId);
    }

    @Override
    public List<User> getFriends(long id) {
        return users.get(id).getFriends().keySet().stream()
                .sorted()
                .map(users::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> getCommonFriends(long id, long otherId) {
        return users.get(id).getFriends().keySet().stream()
                .filter(users.get(otherId).getFriends()::containsKey)
                .sorted()
                .map(users::get)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(long id) {
        users.remove(id);
    }
}
