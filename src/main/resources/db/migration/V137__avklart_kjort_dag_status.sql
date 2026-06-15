ALTER TABLE avklart_kjort_dag
    ADD COLUMN avklart_kjort_dag_status VARCHAR NOT NULL DEFAULT 'NY';

ALTER TABLE avklart_kjort_dag
    ALTER COLUMN avklart_kjort_dag_status DROP DEFAULT;
