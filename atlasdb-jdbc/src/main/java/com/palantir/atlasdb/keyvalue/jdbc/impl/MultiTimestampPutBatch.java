/*
 * Copyright 2016 Palantir Technologies, Inc. All rights reserved.
 *
 * Licensed under the BSD-3 License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.atlasdb.keyvalue.jdbc.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.jooq.InsertValuesStep4;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.Row3;
import org.jooq.impl.DSL;

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.palantir.atlasdb.keyvalue.api.Cell;
import com.palantir.atlasdb.keyvalue.api.Value;

public class MultiTimestampPutBatch implements PutBatch {
    private final Multimap<Cell, Value> data;

    public MultiTimestampPutBatch(Multimap<Cell, Value> data) {
        this.data = data;
    }

    @Override
    public InsertValuesStep4<Record, byte[], byte[], Long, byte[]> addValuesForInsert(InsertValuesStep4<Record, byte[], byte[], Long, byte[]> query) {
        for (Entry<Cell, Value> entry : data.entries()) {
            query = query.values(entry.getKey().getRowName(), entry.getKey().getColumnName(), entry.getValue().getTimestamp(), entry.getValue().getContents());
        }
        return query;
    }

    @Override
    public Collection<Row3<byte[], byte[], Long>> getRowsForSelect() {
        return Collections2.transform(data.entries(), new Function<Entry<Cell, Value>, Row3<byte[], byte[], Long>>() {
            @Override
            public Row3<byte[], byte[], Long> apply(Entry<Cell, Value> entry) {
                return DSL.row(entry.getKey().getRowName(), entry.getKey().getColumnName(), entry.getValue().getTimestamp());
            }
        });
    }

    @Override
    @Nullable
    public PutBatch getNextBatch(Result<? extends Record> existingRecords) {
        Map<CellTimestamp, byte[]> existing = Maps.newHashMapWithExpectedSize(existingRecords.size());
        for (Record record : existingRecords) {
            existing.put(
                    new CellTimestamp(record.getValue(JdbcConstants.A_ROW_NAME), record.getValue(JdbcConstants.A_COL_NAME), record.getValue(JdbcConstants.A_TIMESTAMP)),
                    record.getValue(JdbcConstants.A_VALUE));
        }
        Multimap<Cell, Value> nextBatch = ArrayListMultimap.create();
        for (Entry<Cell, Value> entry : data.entries()) {
            Cell cell = entry.getKey();
            Value newValue = entry.getValue();
            byte[] oldValue = existing.get(new CellTimestamp(cell.getRowName(), cell.getColumnName(), newValue.getTimestamp()));
            if (oldValue == null) {
                nextBatch.put(cell, newValue);
            } else if (!Arrays.equals(oldValue, newValue.getContents())) {
                return null;
            }
        }
        return new MultiTimestampPutBatch(nextBatch);
    }

}
