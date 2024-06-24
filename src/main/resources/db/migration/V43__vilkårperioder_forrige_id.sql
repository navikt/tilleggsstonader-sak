ALTER TABLE vilkar_periode
    ADD COLUMN forrige_vilkarperiode_id UUID REFERENCES vilkar_periode (id);