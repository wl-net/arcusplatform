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
package com.arcussmarthome.platform.tag;

import static com.arcussmarthome.messages.service.SessionService.TaggedEvent.ATTR_SERVICELEVEL;
import static com.arcussmarthome.messages.service.SessionService.TaggedEvent.ATTR_SOURCE;
import static com.arcussmarthome.messages.service.SessionService.TaggedEvent.ATTR_VERSION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.split;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.codahale.metrics.Counter;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.listener.annotation.OnMessage;
import com.arcussmarthome.messages.service.SessionService.TaggedEvent;
import com.arcussmarthome.metrics.IrisMetricSet;
import com.arcussmarthome.metrics.IrisMetrics;
import com.arcussmarthome.metrics.tag.TaggingMetric;

@Singleton
public class TaggedEventHandler
{
   private static final Logger logger = getLogger(TaggedEventHandler.class);

   private static final IrisMetricSet METRICS = IrisMetrics.metrics("tag");
   private static final Counter unknownTagName = METRICS.counter("tagName.unknown");

   private final TagCounterManager tagCounterManager;

   private final ImmutableSet<String> contextIncludes;

   @Inject
   public TaggedEventHandler(TagCounterManager tagCounterManager, TagServiceConfig config)
   {
      this.tagCounterManager = tagCounterManager;

      contextIncludes = ImmutableSet.copyOf(split(config.getContextIncludes(), ','));
   }

   @OnMessage(types = TaggedEvent.NAME)
   public void onTaggedEvent(PlatformMessage msg)
   {
      MessageBody msgBody = msg.getValue();

      String tagName = TaggedEvent.getName(msgBody);

      if (isNotBlank(tagName))
      {
         TaggingMetric<Counter> tagCounter = tagCounterManager.getTagCounter(tagName);

         if (tagCounter != null)
         {
            Map<String, Object> metricTags = TaggedEvent.getContext(msgBody);
            if (metricTags == null)
            {
               metricTags = new HashMap<>();
            }
            else
            {
               // Remove entries with null or blank values from context, since Kairos doesn't like them
               metricTags.entrySet().removeIf(
                  e -> e.getValue() == null || (e.getValue() instanceof String && isBlank((String) e.getValue())));
            }

            // Put in top-level attributes last, so clients can't unintentionally override them via the context

            String source = TaggedEvent.getSource(msgBody);
            if (!isBlank(source))
            {
               metricTags.put(ATTR_SOURCE, source);
            }

            String version = TaggedEvent.getVersion(msgBody);
            if (!isBlank(version))
            {
               metricTags.put(ATTR_VERSION, version);
            }

            String serviceLevel = TaggedEvent.getServiceLevel(msgBody);
            if (!isBlank(serviceLevel))
            {
               metricTags.put(ATTR_SERVICELEVEL, serviceLevel);
            }

            metricTags.keySet().retainAll(contextIncludes);

            tagCounter.tag(metricTags).inc();
         }
         else
         {
            logger.trace("Unknown tag name: " + tagName);

            unknownTagName.inc();
         }
      }
   }
}

