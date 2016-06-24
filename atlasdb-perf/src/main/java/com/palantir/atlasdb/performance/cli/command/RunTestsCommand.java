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
package com.palantir.atlasdb.performance.cli.command;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.palantir.atlasdb.keyvalue.api.KeyValueService;
import com.palantir.atlasdb.performance.api.PerformanceTest;
import com.palantir.atlasdb.performance.api.annotation.PerfTest;
import com.palantir.atlasdb.performance.cli.backend.PhysicalStore;
import com.palantir.atlasdb.performance.cli.backend.PhysicalStoreType;
import com.palantir.atlasdb.performance.tests.TestSinglePuts;

import io.airlift.airline.Arguments;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "run", description = "Run tests")
public class RunTestsCommand implements Callable<Integer> {

    Logger metricsLogger = Logger.getLogger("com.palantir.atlasdb.performance.metrics");

    @Arguments(title = "TESTS", description = "tests to run")
    private String tests = "";

    @Option(name = {"-b", "--backend"}, title = "PHYSICAL STORE", description = "underling physical store (eg POSTGRES)", required = true)
    private PhysicalStoreType type;

    private static final Set<Class<? extends PerformanceTest>> ALL_TESTS =
            ImmutableSet.<Class<? extends PerformanceTest>>builder().add(TestSinglePuts.class).build();

    @Override
    public Integer call() throws Exception {
        Set<Class<? extends PerformanceTest>> allTestsClasses = getAllTests();

        Set<Class<? extends PerformanceTest>> testsToRun = allTestsClasses.stream()
                .filter(test -> getTestArguments().contains(test.getAnnotation(PerfTest.class).name()))
                .collect(Collectors.toSet());
        if (testsToRun.size() == 0 || testsToRun.size() != getTestArguments().size()) {
            printPossibleTests(allTestsClasses);
            return 1;
        }

        try (PhysicalStore physicalStore = PhysicalStore.create(type)) {
            // TODO: something like KeyValueServices.createOn(PhysicalStore store);
            KeyValueService kvs = physicalStore.connect();
            testsToRun.stream().forEach(testClass -> runTest(testClass, kvs));
        }

        return 0;
    }

    private void printPossibleTests(Set<Class<? extends PerformanceTest>> allTestsClasses) {
        String requestedTestsString = Joiner.on(", ").join(getTestArguments());
        String possibleTestsString = Joiner.on(", ").join(
                allTestsClasses.stream()
                        .map(clazz -> clazz.getAnnotation(PerfTest.class).name())
                        .collect(Collectors.toSet()));
        if (requestedTestsString.isEmpty()) {
            System.out.println(String.format("Please select a test to run (%s).", possibleTestsString));
        } else {
            System.out.println(
                    String.format("Not all requested tests (%s) exist. Valid tests include %s.",
                            requestedTestsString,
                            possibleTestsString));
        }
    }

    private void runTest(Class<? extends PerformanceTest> testClass, KeyValueService kvs) {
        try {
            PerformanceTest test = testClass.newInstance();
            test.setup(kvs);
            Stopwatch timer = Stopwatch.createStarted();
            test.run();
            timer.stop();
            test.tearDown();
            String msg = String.format("Test '%s' duration (millis): %d", testClass.getAnnotation(PerfTest.class).name(), timer.elapsed(TimeUnit.MILLISECONDS));
            metricsLogger.info(msg);
            System.out.println(msg);
            kvs.close();
        } catch (InstantiationException e) {
            throw new RuntimeException(testClass.getCanonicalName() + " cannot be instantiated (needs a no args constructor?).", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(testClass.getCanonicalName() + " cannot be instantiated.", e);
        }
    }

    private List<String> getTestArguments() {
        return Lists.newArrayList(tests.split("\\s*(,|\\s)\\s*"));
    }


    protected Set<Class<? extends PerformanceTest>> getAllTests() {
        return ALL_TESTS;
    }

}
