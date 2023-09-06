create table behandlingsjournalpost
(
    behandling_id    UUID         NOT NULL REFERENCES behandling (id),
    journalpost_id   VARCHAR      NOT NULL,
    journalpost_type VARCHAR      NOT NULL,
    opprettet_av     VARCHAR      NOT NULL,
    opprettet_tid    TIMESTAMP(3) NOT NULL,
    endret_av        VARCHAR      NOT NULL,
    endret_tid       TIMESTAMP    NOT NULL,
    UNIQUE (behandling_id, journalpost_id)
);

CREATE INDEX behandlingsjournalpost_behandling_id_idx
    ON behandlingsjournalpost (behandling_id);