DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 from pg_roles where rolname = 'datastream')
        THEN
            ALTER DEFAULT PRIVILEGES IN SCHEMA PUBLIC GRANT SELECT ON TABLES TO "datastream";
            GRANT SELECT ON ALL TABLES IN SCHEMA PUBLIC TO "datastream";

            ALTER USER "tilleggsstonader-sak" WITH REPLICATION;
            ALTER USER "datastream" WITH REPLICATION;
            CREATE PUBLICATION "ds_publication" FOR ALL TABLES;
            SELECT PG_CREATE_LOGICAL_REPLICATION_SLOT('ds_replication', 'pgoutput');
        END IF;
    END
$$;