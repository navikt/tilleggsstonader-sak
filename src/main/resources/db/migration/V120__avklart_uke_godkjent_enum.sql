alter table avklart_kjort_dag
    alter column godkjent_gjennomfort_kjoring TYPE varchar(255)
    USING CASE WHEN godkjent_gjennomfort_kjoring is true THEN 'JA' ELSE 'IKKE_VURDERT' END::varchar;
