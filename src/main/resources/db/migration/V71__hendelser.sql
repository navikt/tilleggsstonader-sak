CREATE TABLE hendelse
(
    id            TEXT         NOT NULL,
    type          TEXT         NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    PRIMARY KEY (id, type)
)