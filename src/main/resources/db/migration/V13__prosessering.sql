CREATE TABLE task
(
    id            BIGSERIAL PRIMARY KEY,
    payload       VARCHAR NOT NULL,
    status        VARCHAR NOT NULL,
    versjon       BIGINT  NOT NULL,
    opprettet_tid TIMESTAMP(3),
    type          VARCHAR NOT NULL,
    metadata      VARCHAR,
    trigger_tid   TIMESTAMP(3),
    avvikstype    VARCHAR
);


CREATE UNIQUE INDEX ON task (payload, type);

CREATE INDEX ON task (status);

CREATE TABLE task_logg
(
    id            BIGSERIAL PRIMARY KEY,
    task_id       BIGINT       NOT NULL REFERENCES task (id),
    type          VARCHAR      NOT NULL,
    node          VARCHAR      NOT NULL,
    opprettet_tid TIMESTAMP(3) NOT NULL,
    melding       VARCHAR,
    endret_av     VARCHAR      NOT NULL
);

CREATE INDEX ON TASK_LOGG (task_id);

