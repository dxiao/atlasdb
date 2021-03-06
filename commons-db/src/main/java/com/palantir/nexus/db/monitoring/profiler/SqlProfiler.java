/*
 * Copyright 2015 Palantir Technologies, Inc. All rights reserved.
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
package com.palantir.nexus.db.monitoring.profiler;

import java.util.Collection;

import com.palantir.nexus.db.sql.BasicSQL;
import com.palantir.util.sql.SqlCallStats;

/**
 * Code extracted from {@link BasicSQL}. This provides one of several competing and overlapping
 * mechanisms to register monitoring callbacks for various SQL actions.
 *
 * @author jweel
 */
public interface SqlProfiler {
    void start();

    void update(String sqlKey, String rawSql, long durationNs);

    Collection<SqlCallStats> stop();

    void addSqlProfilerListener(SqlProfilerListener sqlProfilerListener);

    void removeSqlProfilerListener(SqlProfilerListener sqlProfilerListener);

    public interface SqlProfilerListener {
        void traceEvent(String key, long durationNs);
    }
}
