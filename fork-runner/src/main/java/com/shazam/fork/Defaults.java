/*
 * Copyright 2019 Apple Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.shazam.fork;

import java.util.concurrent.TimeUnit;

class Defaults {

    private Defaults() {
    }

    static final long TEST_OUTPUT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(1);
    static final String FORK_OUTPUT = "fork-output";
    static final int STRATEGY_LIMIT = 1;
    static final String TITLE = "Fork Report";
    static final String SUBTITLE = "";
    static final int RETRY_QUOTA_PER_TEST_CASE = 1;
}
