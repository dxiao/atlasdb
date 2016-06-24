package com.palantir.atlasdb.performance.tests;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableMap;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.keyvalue.api.TableReference;
import com.palantir.atlasdb.keyvalue.api.Value;
import com.palantir.atlasdb.performance.api.PerformanceTest;
import com.palantir.atlasdb.performance.api.ValueGenerator;
import com.palantir.atlasdb.performance.api.annotation.PerfTest;
import com.palantir.atlasdb.performance.utils.RandomByteBufferGenerator;
import com.palantir.atlasdb.protos.generated.TableMetadataPersistence;
import com.palantir.atlasdb.table.description.TableDefinition;
import com.palantir.atlasdb.table.description.ValueType;
import com.palantir.atlasdb.transaction.api.ConflictHandler;

// TODO (mwakerman): define naming convention for new tests to be written and not collide
@PerfTest(name = "tests-single-puts")
public class TestSinglePuts implements PerformanceTest {

    // Constants.
    static private final long VALUE_SEED = 279L;
    static private final String TABLE_NAME = "performance.table";
    static private final String ROW_COMPONENT = "key";
    static private final String COLUMN_NAME = "value";
    static private final byte [] COLUMN_NAME_IN_BYTES = COLUMN_NAME.getBytes();

    // Setup.
    private KeyValueService kvs;
    private TableReference tableRef;
    private ValueGenerator gen;

    @Override
    public int setup(KeyValueService kvs) {
        this.kvs = kvs;
        this.tableRef = createTable(kvs);
        // TODO: this could do with a builder pattern to take the number of byte arrays and the length.
        this.gen = RandomByteBufferGenerator.withSeed(VALUE_SEED);
        return 0;
    }

    @Override
    public void run() {
        gen.stream().forEach(bytes -> kvs.put(tableRef, createSinglePutValue(bytes), 1));
    }

    @Override
    public int tearDown() {
        kvs.dropTable(tableRef);
        return 0;
    }

    private Map<Cell, byte[]> createSinglePutValue(ByteBuffer bytes) {
        Cell cell = Cell.create(ValueType.STRING.convertFromString(UUID.randomUUID().toString()), COLUMN_NAME_IN_BYTES);
        return ImmutableMap.of(cell, bytes.array());
    }

    /**
     * Creates the table used by this performance test in the provided key-value service
     * and returns a reference to the table.
     *
     * @return a table reference for the newly created table.
     */
    private TableReference createTable(KeyValueService kvs) {
        TableReference tableRef = TableReference.createFromFullyQualifiedName(TABLE_NAME);
        TableDefinition tableDef = new TableDefinition() {{
            rowName();
            rowComponent(ROW_COMPONENT, ValueType.STRING);
            columns();
            column(COLUMN_NAME, COLUMN_NAME, ValueType.BLOB);
            conflictHandler(ConflictHandler.IGNORE_ALL);
            sweepStrategy(TableMetadataPersistence.SweepStrategy.NOTHING);
        }};
        kvs.createTable(tableRef, tableDef.toTableMetadata().persistToBytes());
        return tableRef;
    }

}
