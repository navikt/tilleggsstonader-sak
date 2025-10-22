CREATE TABLE UTBETALING_ID
(
    ID         VARCHAR(25) NOT NULL PRIMARY KEY,
    FAGSAK_ID  UUID        NOT NULL REFERENCES fagsak (id),
    TYPE_ANDEL VARCHAR(50) NOT NULL
);

create unique index on UTBETALING_ID (fagsak_id, type_andel);

