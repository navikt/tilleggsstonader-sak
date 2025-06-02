alter table oppgave add column tilordnet_saksbehandler varchar;
create index idx_oppgave_gsak_oppgave_id on oppgave (gsak_oppgave_id);
