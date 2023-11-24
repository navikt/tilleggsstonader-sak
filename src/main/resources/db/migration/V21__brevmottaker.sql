CREATE TABLE brevmottaker (
    id                                          UUID PRIMARY KEY,
    behandling_id                               UUID         NOT NULL REFERENCES behandling (id),

    journalpost_id                              VARCHAR,
    bestilling_id                               VARCHAR,

    person_mottaker_person_ident                VARCHAR,
    person_mottaker_navn                        VARCHAR,
    person_mottaker_mottaker_rolle              VARCHAR,

    organisasjon_mottaker_organisasjonsnummer   VARCHAR,
    organisasjon_mottaker_navn_hos_organisasjon VARCHAR,
    organisasjon_mottaker_mottaker_rolle        VARCHAR,

    CONSTRAINT unik_personmottaker UNIQUE (behandling_id, person_mottaker_person_ident),
    CONSTRAINT unik_organisasjonsmottaker UNIQUE (behandling_id, organisasjon_mottaker_organisasjonsnummer),
    CONSTRAINT personmottaker_eller_organisasjonsmottaker_finnes CHECK ((person_mottaker_person_ident IS NOT NULL AND
                                                                         organisasjon_mottaker_organisasjonsnummer IS NULL) OR
                                                                        (person_mottaker_person_ident IS NULL AND
                                                                         organisasjon_mottaker_organisasjonsnummer IS NOT NULL)),

    opprettet_av                                VARCHAR      NOT NULL DEFAULT 'VL',
    opprettet_tid                               TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP,
    endret_av                                   VARCHAR      NOT NULL,
    endret_tid                                  TIMESTAMP(3) NOT NULL DEFAULT LOCALTIMESTAMP
);