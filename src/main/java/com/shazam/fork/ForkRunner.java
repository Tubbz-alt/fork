/*
 * Copyright 2014 Shazam Entertainment Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.shazam.fork;

import com.shazam.fork.listeners.PoolProgressTracker;
import com.shazam.fork.listeners.ProgressReporter;
import com.shazam.fork.model.Pool;
import com.shazam.fork.model.TestClass;
import com.shazam.fork.pooling.PoolLoader;
import com.shazam.fork.runner.PoolTestRunner;
import com.shazam.fork.runner.PoolTestRunnerFactory;
import com.shazam.fork.suite.TestClassLoader;
import com.shazam.fork.summary.*;
import com.shazam.fork.system.io.FileManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import static com.shazam.fork.Utils.namedExecutor;

public class ForkRunner {
    private static final Logger logger = LoggerFactory.getLogger(ForkRunner.class);

    private final RuntimeConfiguration runtimeConfiguration;
    private final PoolLoader poolLoader;
    private final TestClassLoader testClassLoader;
    private final SummaryPrinter summaryPrinter;
    private final FileManager fileManager;
    private final PoolTestRunnerFactory poolTestRunnerFactory;
    private final ProgressReporter progressReporter;

    public ForkRunner(RuntimeConfiguration runtimeConfiguration,
                      PoolLoader poolLoader,
                      TestClassLoader testClassLoader,
                      SummaryPrinter summaryPrinter,
                      FileManager fileManager,
                      PoolTestRunnerFactory poolTestRunnerFactory,
                      ProgressReporter progressReporter) {
        this.runtimeConfiguration = runtimeConfiguration;
        this.poolLoader = poolLoader;
        this.testClassLoader = testClassLoader;
        this.summaryPrinter = summaryPrinter;
        this.fileManager = fileManager;
        this.poolTestRunnerFactory = poolTestRunnerFactory;
        this.progressReporter = progressReporter;
    }

    public boolean run() {
        ExecutorService poolExecutor = null;
        try {
            Collection<Pool> pools = poolLoader.loadPools();
            if (pools.isEmpty()) {
                logger.error("No device pools found, so marking as failure");
                return false;
            }

            int numberOfPools = pools.size();
            CountDownLatch poolCountDownLatch = new CountDownLatch(numberOfPools);
            poolExecutor = namedExecutor(numberOfPools, "PoolExecutor-%d");

            List<TestClass> testClasses = testClassLoader.loadTestClasses();
            // Only need emergency shutdown hook once tests have started.
            ReportGeneratorHook reportGeneratorHook = new ReportGeneratorHook(runtimeConfiguration, fileManager,
                    pools, testClasses, summaryPrinter);
            Runtime.getRuntime().addShutdownHook(reportGeneratorHook);

            progressReporter.start();
            for (Pool pool : pools) {
                progressReporter.addPoolProgress(pool, new PoolProgressTracker(testClasses.size()));
                PoolTestRunner poolTestRunner = poolTestRunnerFactory.createPoolTestRunner(pool, testClasses,
                        poolCountDownLatch, progressReporter);
                poolExecutor.execute(poolTestRunner);
            }
            poolCountDownLatch.await();
            progressReporter.stop();

            Summary summary = reportGeneratorHook.generateReportOnlyOnce();
            boolean overallSuccess = summary != null && new OutcomeAggregator().aggregate(summary);
            logger.info("Overall success: " + overallSuccess);
            return overallSuccess;

        } catch (Exception e) {
            logger.error("Error while Fork runner was executing", e);
            return false;

        } finally {
            if (poolExecutor != null) {
                poolExecutor.shutdown();
            }
        }
    }
}
