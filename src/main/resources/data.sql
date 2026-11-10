INSERT INTO genres (id, name)
SELECT 1, 'Комедия' WHERE (SELECT COUNT(*) FROM genres WHERE id = 1) = 0;
INSERT INTO genres (id, name)
SELECT 2, 'Драма' WHERE (SELECT COUNT(*) FROM genres WHERE id = 2) = 0;
INSERT INTO genres (id, name)
SELECT 3, 'Мультфильм' WHERE (SELECT COUNT(*) FROM genres WHERE id = 3) = 0;
INSERT INTO genres (id, name)
SELECT 4, 'Триллер' WHERE (SELECT COUNT(*) FROM genres WHERE id = 4) = 0;
INSERT INTO genres (id, name)
SELECT 5, 'Документальный' WHERE (SELECT COUNT(*) FROM genres WHERE id = 5) = 0;
INSERT INTO genres (id, name)
SELECT 6, 'Боевик' WHERE (SELECT COUNT(*) FROM genres WHERE id = 6) = 0;

INSERT INTO mpa (id, name)
SELECT 1, 'G' WHERE (SELECT COUNT(*) FROM mpa WHERE id = 1) = 0;
INSERT INTO mpa (id, name)
SELECT 2, 'PG' WHERE (SELECT COUNT(*) FROM mpa WHERE id = 2) = 0;
INSERT INTO mpa (id, name)
SELECT 3, 'PG-13' WHERE (SELECT COUNT(*) FROM mpa WHERE id = 3) = 0;
INSERT INTO mpa (id, name)
SELECT 4, 'R' WHERE (SELECT COUNT(*) FROM mpa WHERE id = 4) = 0;
INSERT INTO mpa (id, name)
SELECT 5, 'NC-17' WHERE (SELECT COUNT(*) FROM mpa WHERE id = 5) = 0;
