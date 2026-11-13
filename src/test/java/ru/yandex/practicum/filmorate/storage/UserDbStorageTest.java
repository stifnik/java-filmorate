package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.practicum.filmorate.storage.TestData.newFilm;
import static ru.yandex.practicum.filmorate.storage.TestData.newUser;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {
    private final JdbcTemplate jdbc;
    private UserDbStorage users;
    private FilmDbStorage films;

    @BeforeEach
    void setUp() {
        users = new UserDbStorage(jdbc, new UserRowMapper());
        films = new FilmDbStorage(jdbc, new FilmRowMapper());
    }

    @Test
    void shouldCreateReadAndListUsersWithoutSavingSuppliedFriendships() {
        User request = newUser("first");
        request.setId(999L);
        request.setFriends(Map.of(999L, FriendshipStatus.CONFIRMED));
        User first = users.create(request);
        User second = users.create(newUser("second"));

        assertThat(first.getId()).isPositive().isNotEqualTo(999L);
        assertThat(first.getFriends()).isEmpty();
        assertThat(users.getById(first.getId())).contains(first);
        assertThat(users.getById(999L)).isEmpty();
        assertThat(users.getAll()).containsExactly(first, second);
    }

    @Test
    void shouldUpdateProfileWithoutChangingFriendships() {
        User first = users.create(newUser("first"));
        User second = users.create(newUser("second"));
        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.PENDING);
        User update = newUser("updated");
        update.setId(first.getId());

        User actual = users.update(update);

        assertThat(actual.getEmail()).isEqualTo("updated@example.com");
        assertThat(actual.getFriends()).containsExactlyEntriesOf(Map.of(second.getId(), FriendshipStatus.PENDING));
        assertThat(users.getById(first.getId())).contains(actual);
        update.setId(999L);
        assertThatThrownBy(() -> users.update(update)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldSaveUpdateAndRemoveDirectedFriendshipWithoutDuplicates() {
        User first = users.create(newUser("first"));
        User second = users.create(newUser("second"));
        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.PENDING);
        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.PENDING);

        assertThat(users.getFriends(first.getId())).extracting(User::getId).containsExactly(second.getId());
        assertThat(users.getFriends(second.getId())).isEmpty();

        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.CONFIRMED);
        assertThat(users.getById(first.getId()).orElseThrow().getFriends())
                .containsEntry(second.getId(), FriendshipStatus.CONFIRMED);

        users.removeFriendship(first.getId(), second.getId());
        users.removeFriendship(first.getId(), second.getId());
        assertThat(users.getFriends(first.getId())).isEmpty();
    }

    @Test
    void shouldFindOnlyCommonOutgoingFriendsInIdOrder() {
        User first = users.create(newUser("first"));
        User second = users.create(newUser("second"));
        User common = users.create(newUser("common"));
        User other = users.create(newUser("other"));
        for (User user : List.of(first, second)) {
            users.saveFriendship(user.getId(), common.getId(), FriendshipStatus.PENDING);
        }
        users.saveFriendship(first.getId(), other.getId(), FriendshipStatus.PENDING);

        assertThat(users.getCommonFriends(first.getId(), second.getId()))
                .extracting(User::getId).containsExactly(common.getId());
        assertThat(users.getCommonFriends(common.getId(), first.getId())).isEmpty();
        assertThat(users.getFriends(first.getId())).extracting(User::getId)
                .containsExactly(common.getId(), other.getId());
    }

    @Test
    void shouldLoadFriendshipsForUserListsWithoutMixingOwnersAndStatuses() {
        User first = users.create(newUser("first"));
        User second = users.create(newUser("second"));
        User common = users.create(newUser("common"));
        User other = users.create(newUser("other"));
        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.CONFIRMED);
        users.saveFriendship(second.getId(), first.getId(), FriendshipStatus.CONFIRMED);
        users.saveFriendship(first.getId(), common.getId(), FriendshipStatus.PENDING);
        users.saveFriendship(second.getId(), common.getId(), FriendshipStatus.PENDING);
        users.saveFriendship(common.getId(), other.getId(), FriendshipStatus.PENDING);
        first.setFriends(Map.of(second.getId(), FriendshipStatus.CONFIRMED,
                common.getId(), FriendshipStatus.PENDING));
        second.setFriends(Map.of(first.getId(), FriendshipStatus.CONFIRMED,
                common.getId(), FriendshipStatus.PENDING));
        common.setFriends(Map.of(other.getId(), FriendshipStatus.PENDING));

        assertThat(users.getAll()).containsExactly(first, second, common, other);
        assertThat(users.getFriends(first.getId())).containsExactly(second, common);
        assertThat(users.getCommonFriends(first.getId(), second.getId())).containsExactly(common);
        assertThat(users.getById(first.getId())).contains(first);
        assertThat(users.getById(other.getId())).contains(other);
        assertThat(users.getFriends(other.getId())).isEmpty();
    }

    @Test
    void shouldDeleteUserWithIncomingOutgoingFriendshipsAndLikes() {
        User first = users.create(newUser("first"));
        User second = users.create(newUser("second"));
        long filmId = films.create(newFilm("Film")).getId();
        users.saveFriendship(first.getId(), second.getId(), FriendshipStatus.CONFIRMED);
        users.saveFriendship(second.getId(), first.getId(), FriendshipStatus.CONFIRMED);
        films.addLike(filmId, first.getId());

        users.delete(first.getId());
        users.delete(first.getId());

        assertThat(users.getById(first.getId())).isEmpty();
        assertThat(users.getFriends(second.getId())).isEmpty();
        assertThat(films.getById(filmId).orElseThrow().getLikes()).isEmpty();
        assertThat(users.getAll()).extracting(User::getId).containsExactly(second.getId());
    }
}
