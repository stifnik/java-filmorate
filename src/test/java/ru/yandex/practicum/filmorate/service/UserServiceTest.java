package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserServiceTest {
    private UserService service;
    private User user;
    private User friend;

    @BeforeEach
    void setUp() {
        service = new UserService(new InMemoryUserStorage());
        user = service.create(newUser());
        friend = service.create(newUser());
    }

    @Test
    void shouldAddFriendshipOnBothSidesWithoutDuplicates() {
        service.addFriend(user.getId(), friend.getId());
        service.addFriend(friend.getId(), user.getId());
        service.addFriend(user.getId(), friend.getId());

        assertEquals(List.of(friend), service.getFriends(user.getId()));
        assertEquals(List.of(user), service.getFriends(friend.getId()));
    }

    @Test
    void shouldRemoveFriendshipOnBothSidesAndKeepOtherFriends() {
        User otherFriend = service.create(newUser());
        service.addFriend(user.getId(), friend.getId());
        service.addFriend(user.getId(), otherFriend.getId());

        service.removeFriend(friend.getId(), user.getId());
        service.removeFriend(friend.getId(), user.getId());

        assertEquals(List.of(otherFriend), service.getFriends(user.getId()));
        assertTrue(service.getFriends(friend.getId()).isEmpty());
        assertEquals(List.of(user), service.getFriends(otherFriend.getId()));
    }

    @Test
    void shouldFindCommonFriendsWithoutChangingFriendships() {
        User common = service.create(newUser());
        User onlyUserFriend = service.create(newUser());
        service.addFriend(user.getId(), common.getId());
        service.addFriend(friend.getId(), common.getId());
        service.addFriend(user.getId(), onlyUserFriend.getId());

        assertEquals(List.of(common), service.getCommonFriends(user.getId(), friend.getId()));
        assertEquals(Set.of(common.getId(), onlyUserFriend.getId()), user.getFriends());
        assertEquals(Set.of(common.getId()), friend.getFriends());
        assertEquals(Set.of(user.getId(), friend.getId()), common.getFriends());
    }

    @Test
    void shouldReturnEmptyListsWhenThereAreNoFriendsInCommon() {
        assertTrue(service.getFriends(user.getId()).isEmpty());
        service.addFriend(user.getId(), friend.getId());

        assertTrue(service.getCommonFriends(user.getId(), friend.getId()).isEmpty());
    }

    @Test
    void shouldRejectUnknownUsersWithoutChangingFriendships() {
        long missingId = 999L;
        service.addFriend(user.getId(), friend.getId());

        assertThrows(NotFoundException.class, () -> service.getById(missingId));
        assertThrows(NotFoundException.class, () -> service.getFriends(missingId));
        assertThrows(NotFoundException.class, () -> service.addFriend(user.getId(), missingId));
        assertThrows(NotFoundException.class, () -> service.addFriend(missingId, user.getId()));
        assertThrows(NotFoundException.class, () -> service.removeFriend(user.getId(), missingId));
        assertThrows(NotFoundException.class, () -> service.removeFriend(missingId, user.getId()));
        assertThrows(NotFoundException.class, () -> service.getCommonFriends(user.getId(), missingId));
        assertThrows(NotFoundException.class, () -> service.getCommonFriends(missingId, user.getId()));
        assertEquals(List.of(friend), service.getFriends(user.getId()));
        assertEquals(List.of(user), service.getFriends(friend.getId()));
    }

    @Test
    void shouldRejectFriendshipWithSelf() {
        assertThrows(ValidationException.class, () -> service.addFriend(user.getId(), user.getId()));

        assertTrue(service.getFriends(user.getId()).isEmpty());
    }

    @Test
    void shouldRejectRemovingSelfFromFriendsWithoutChangingFriendships() {
        service.addFriend(user.getId(), friend.getId());

        assertThrows(ValidationException.class, () -> service.removeFriend(user.getId(), user.getId()));
        assertEquals(List.of(friend), service.getFriends(user.getId()));
        assertEquals(List.of(user), service.getFriends(friend.getId()));
    }

    @Test
    void shouldRejectFindingCommonFriendsWithSelfWithoutChangingFriendships() {
        service.addFriend(user.getId(), friend.getId());

        assertThrows(ValidationException.class, () -> service.getCommonFriends(user.getId(), user.getId()));
        assertEquals(List.of(friend), service.getFriends(user.getId()));
        assertEquals(List.of(user), service.getFriends(friend.getId()));
    }

    @Test
    void shouldDeleteUserAndRejectDeletingUnknownUser() {
        service.delete(user.getId());

        assertThrows(NotFoundException.class, () -> service.getById(user.getId()));
        assertThrows(NotFoundException.class, () -> service.delete(user.getId()));
        assertEquals(1, service.getAll().size());
        assertEquals(friend, service.getById(friend.getId()));
    }

    @Test
    void shouldKeepFriendshipWhenUpdatingUser() {
        service.addFriend(user.getId(), friend.getId());
        User update = newUser();
        update.setId(user.getId());
        update.setLogin("updated");
        update.setFriends(Set.of(999L));

        User updatedUser = service.update(update);

        assertEquals("updated", updatedUser.getLogin());
        assertEquals("updated", updatedUser.getName());
        assertEquals(Set.of(friend.getId()), updatedUser.getFriends());
        assertEquals(Set.of(user.getId()), service.getById(friend.getId()).getFriends());
        assertEquals(List.of(updatedUser), service.getFriends(friend.getId()));
    }

    @Test
    void shouldCreateUserWithoutSuppliedFriendships() {
        User newUser = newUser();
        newUser.setFriends(Set.of(user.getId()));

        User createdUser = service.create(newUser);

        assertTrue(service.getFriends(createdUser.getId()).isEmpty());
        assertTrue(service.getFriends(user.getId()).isEmpty());
    }

    private User newUser() {
        User result = new User();
        result.setEmail("user@example.com");
        result.setLogin("user");
        result.setBirthday(LocalDate.of(2000, 1, 1));
        return result;
    }
}
