/**
 * Copyright 2015 Palantir Technologies
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
package com.palantir.atlasdb.performance.utils;

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.stream.Stream;

import com.palantir.atlasdb.performance.api.ValueGenerator;

// This is a naive non-configurable implementation of a ValueGenerator.
// This will be refactored or deleted as we figure out what we want to
// do with value generation in general.
public class RandomByteBufferGenerator implements ValueGenerator {

    private static final int STREAM_LIMIT = 10000;
    private static final int BYTE_ARRAY_SIZE = 100;

    private final Random rand;

    public static RandomByteBufferGenerator withSeed(long seed) {
        return new RandomByteBufferGenerator(seed);
    }

    public RandomByteBufferGenerator(long seed) {
        this.rand = new Random(seed);
    }

    @Override
    public Stream<ByteBuffer> stream() {
        return Stream.generate(this::genBytes).limit(STREAM_LIMIT);
    }

    private ByteBuffer genBytes() {
        byte[] b = new byte[BYTE_ARRAY_SIZE];
        rand.nextBytes(b);
        return ByteBuffer.wrap(b);
    }
}
