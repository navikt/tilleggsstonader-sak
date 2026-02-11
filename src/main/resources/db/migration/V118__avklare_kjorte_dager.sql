create table avklart_kjort_uke
(
    id             UUID                            NOT NULL PRIMARY KEY,
    behandling_id  UUID REFERENCES behandling (id) NOT NULL,
    fom            DATE                            NOT NULL,
    tom            DATE                            NOT NULL,
    ukenummer      INT                             NOT NULL,
    status         VARCHAR                         NOT NULL,
    type_avvik     VARCHAR                         NULL,
    behandlet_dato DATE                            NULL
);

create index idx_avklart_kjort_uke_behandling_id on avklart_kjort_uke (behandling_id);

create table avklart_kjort_dag
(
    id                           UUID      NOT NULL PRIMARY KEY,
    dato                         DATE      NOT NULL,
    avklart_kjort_uke_id         UUID      NOT NULL REFERENCES avklart_kjort_uke (id),
    godkjent_gjennomfort_kjoring BOOLEAN   NOT NULL,
    automatisk_vurdering         VARCHAR   NOT NULL,
    avvik                        VARCHAR[] NULL,
    begrunnelse                  VARCHAR   NULL,
    parkeringsutgift             INT       NULL
);

create index idx_avklart_kjort_dag_avklart_kjort_uke_id on avklart_kjort_dag (avklart_kjort_uke_id);