create table registrert_kjort_uke
(
    id            UUID                            NOT NULL PRIMARY KEY,
    behandling_id UUID REFERENCES behandling (id) NOT NULL,
    reise_id      UUID                            NOT NULL,
    begrunnelse   VARCHAR                         NULL
);

create index idx_registrert_kjort_uke_behandling_id on registrert_kjort_uke (behandling_id);

create table registrert_kjort_dag
(
    id                      UUID    NOT NULL PRIMARY KEY,
    registrert_kjort_uke_id UUID    NOT NULL REFERENCES registrert_kjort_uke (id),
    dato                    DATE    NOT NULL,
    har_kjort               BOOLEAN NOT NULL,
    parkeringsutgift        INT     NULL
);

create index idx_registrert_kjort_dag_uke_id on registrert_kjort_dag (registrert_kjort_uke_id);
