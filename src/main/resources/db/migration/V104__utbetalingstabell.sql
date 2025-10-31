create table IVERKSETTING_LOGG
(
    ID              BIGSERIAL   NOT NULL PRIMARY KEY,
    IVERKSETTING_ID UUID        NOT NULL,
    UTBETALING_JSON JSON        NOT NULL,
    SENDT_TIDSPUNKT TIMESTAMP   NOT NULL DEFAULT current_timestamp(4)
);

create index idx_fagsak_utbetalinger_iverksetting_id on IVERKSETTING_LOGG (iverksetting_id)
