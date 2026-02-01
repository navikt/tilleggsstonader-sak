-- Hvis en bruker eller Klageinstansen ber om innsyn i en sak, så kan vi få bruk for å hente ut hvilken informasjon saksbehandler har fått fra kartløsningen.
CREATE TABLE KJOREAVSTAND_LOGG
(
    id            UUID PRIMARY KEY,
    tidspunkt     TIMESTAMP(3) NOT NULL,
    saksbehandler VARCHAR      NOT NULL,
    sporring      JSONB        NOT NULL,
    resultat      JSONB
);
