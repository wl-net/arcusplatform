/*
 * Copyright 2019 Arcus Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.arcussmarthome.core.metricsreporter.builder;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bootstrap.guice.AbstractIrisModule;
import com.arcussmarthome.core.IrisApplication;
import com.arcussmarthome.core.metricsreporter.reporter.IrisMetricsTopicReporter;
import com.arcussmarthome.metrics.IrisMetrics;

/**
 *
 */
@Singleton
public class MetricsTopicReporterBuilderModule extends AbstractIrisModule {

    @Inject
    public MetricsTopicReporterBuilderModule(
          IrisApplication application // ensure IrisApplication has been loaded to set some of the info
    ) {
    }

    @Override
    protected void configure() {
       bind(MetricRegistry.class).toInstance(IrisMetrics.registry());
       bind(IrisMetricsTopicReporter.class).asEagerSingleton();
    }

}


