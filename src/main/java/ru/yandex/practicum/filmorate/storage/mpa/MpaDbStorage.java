package ru.yandex.practicum.filmorate.storage.mpa;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.BaseDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
public class MpaDbStorage extends BaseDbStorage<Mpa> implements MpaStorage {
    private static final String FIND_ALL_QUERY = "SELECT id, name FROM mpa ORDER BY id";
    private static final String FIND_BY_ID_QUERY = "SELECT id, name FROM mpa WHERE id = ?";

    public MpaDbStorage(JdbcTemplate jdbc, MpaRowMapper mapper) {
        super(jdbc, mapper);
    }

    @Override
    public List<Mpa> getAll() {
        return findMany(FIND_ALL_QUERY);
    }

    @Override
    public Optional<Mpa> getById(int id) {
        return findOne(FIND_BY_ID_QUERY, id);
    }
}
