CREATE TABLE oppfolging
(
    id                        UUID         NOT NULL PRIMARY KEY,
    behandling_id             UUID         NOT NULL,
    version                   INT          NOT NULL,
    aktiv                     BOOLEAN      NOT NULL,
    data                      JSONB        NOT NULL,
    opprettet_tidspunkt       TIMESTAMP(3) NOT NULL,
    kontrollert_tidspunkt     TIMESTAMP(3),
    kontrollert_saksbehandler TEXT,
    kontrollert_kommentar     TEXT
);

CREATE INDEX on oppfolging (aktiv) WHERE aktiv = true;
CREATE INDEX on oppfolging (behandling_id) WHERE aktiv = true;