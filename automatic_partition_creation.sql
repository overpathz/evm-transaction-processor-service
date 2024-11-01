CREATE OR REPLACE PROCEDURE create_transaction_partition(start_block BIGINT, end_block BIGINT)
    LANGUAGE plpgsql AS $$
DECLARE
    partition_name TEXT;
BEGIN
    partition_name := format('transactions_p%010d_%010d', start_block, end_block - 1);

    EXECUTE format('
        CREATE TABLE IF NOT EXISTS %I
        PARTITION OF transactions
        FOR VALUES FROM (%L) TO (%L);
    ', partition_name, start_block, end_block);

    -- Create trigger on the new partition
    EXECUTE format('
        CREATE TRIGGER tsvectorupdate BEFORE INSERT OR UPDATE
            ON %I
            FOR EACH ROW EXECUTE FUNCTION transactions_search_vector_update();
    ', partition_name);

    -- Create indexes on the new partition
    EXECUTE format('CREATE INDEX idx_transactions_hash_%I ON %I (hash);', partition_name, partition_name);
    EXECUTE format('CREATE INDEX idx_transactions_from_address_%I ON %I (from_address);', partition_name, partition_name);
    EXECUTE format('CREATE INDEX idx_transactions_to_address_%I ON %I (to_address);', partition_name, partition_name);
    EXECUTE format('CREATE INDEX idx_transactions_search_vector_%I ON %I USING GIN (search_vector);', partition_name, partition_name);

END;
$$;
