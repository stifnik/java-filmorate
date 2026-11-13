package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository("userDbStorage")
public class UserDbStorage extends BaseDbStorage<User> implements UserStorage {
    private static final String FIND_ALL_QUERY = "SELECT * FROM users ORDER BY id";
    private static final String FIND_BY_ID_QUERY = "SELECT * FROM users WHERE id = ?";
    private static final String INSERT_QUERY = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_QUERY = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
    private static final String DELETE_QUERY = "DELETE FROM users WHERE id = ?";
    private static final String FIND_FRIENDSHIPS_QUERY = "SELECT user_id, friend_id, status FROM friendships "
            + "WHERE user_id IN (%s)";
    private static final String INSERT_FRIENDSHIP_QUERY = "INSERT INTO friendships (user_id, friend_id, status) "
            + "VALUES (?, ?, ?)";
    private static final String UPDATE_FRIENDSHIP_QUERY = "UPDATE friendships SET status = ? "
            + "WHERE user_id = ? AND friend_id = ?";
    private static final String DELETE_FRIENDSHIP_QUERY = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
    private static final String FIND_FRIENDS_QUERY = "SELECT u.* FROM users u "
            + "JOIN friendships fr ON fr.friend_id = u.id WHERE fr.user_id = ? ORDER BY u.id";
    private static final String FIND_COMMON_FRIENDS_QUERY = "SELECT u.* FROM users u "
            + "JOIN friendships f1 ON f1.friend_id = u.id "
            + "JOIN friendships f2 ON f2.friend_id = u.id "
            + "WHERE f1.user_id = ? AND f2.user_id = ? ORDER BY u.id";

    public UserDbStorage(JdbcTemplate jdbc, UserRowMapper mapper) {
        super(jdbc, mapper);
    }

    @Override
    public User create(User user) {
        long id = insert(INSERT_QUERY,
                user.getEmail(), user.getLogin(), user.getName(), user.getBirthday());
        user.setId(id);
        return getById(id).orElseThrow();
    }

    @Override
    public User update(User user) {
        int updated = jdbc.update(UPDATE_QUERY,
                user.getEmail(), user.getLogin(), user.getName(), user.getBirthday(), user.getId());
        if (updated == 0) {
            throw new NotFoundException("Пользователь с id = " + user.getId() + " не найден");
        }
        return getById(user.getId()).orElseThrow();
    }

    @Override
    public Optional<User> getById(long id) {
        return findOne(FIND_BY_ID_QUERY, id).map(user -> {
            loadFriends(List.of(user));
            return user;
        });
    }

    @Override
    public List<User> getAll() {
        List<User> users = findMany(FIND_ALL_QUERY);
        loadFriends(users);
        return users;
    }

    @Override
    public void saveFriendship(long userId, long friendId, FriendshipStatus status) {
        int updated = jdbc.update(UPDATE_FRIENDSHIP_QUERY,
                status.name(), userId, friendId);
        if (updated == 0) {
            jdbc.update(INSERT_FRIENDSHIP_QUERY,
                    userId, friendId, status.name());
        }
    }

    @Override
    public void removeFriendship(long userId, long friendId) {
        jdbc.update(DELETE_FRIENDSHIP_QUERY, userId, friendId);
    }

    @Override
    public List<User> getFriends(long id) {
        List<User> users = findMany(FIND_FRIENDS_QUERY, id);
        loadFriends(users);
        return users;
    }

    @Override
    public List<User> getCommonFriends(long id, long otherId) {
        List<User> users = findMany(FIND_COMMON_FRIENDS_QUERY, id, otherId);
        loadFriends(users);
        return users;
    }

    @Override
    public void delete(long id) {
        jdbc.update(DELETE_QUERY, id);
    }

    private void loadFriends(List<User> users) {
        if (users.isEmpty()) {
            return;
        }
        Map<Long, User> usersById = new HashMap<>();
        for (User user : users) {
            user.setFriends(new HashMap<>());
            usersById.put(user.getId(), user);
        }
        Object[] ids = usersById.keySet().toArray();
        String placeholders = String.join(", ", Collections.nCopies(ids.length, "?"));
        SqlRowSet rows = jdbc.queryForRowSet(String.format(FIND_FRIENDSHIPS_QUERY, placeholders), ids);
        while (rows.next()) {
            User user = usersById.get(rows.getLong("user_id"));
            user.getFriends().put(rows.getLong("friend_id"), FriendshipStatus.valueOf(rows.getString("status")));
        }
    }
}
