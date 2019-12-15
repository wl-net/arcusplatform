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
package com.arcussmarthome.platform.history.appender.subsys;

import static com.arcussmarthome.platform.history.appender.translator.ValueGetter.PLACE_NAME;

import java.util.List;
import java.util.UUID;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.PlaceMonitorSubsystemCapability;
import com.arcussmarthome.messages.capability.PlaceMonitorSubsystemCapability.DeviceOfflineEvent;
import com.arcussmarthome.messages.capability.PlaceMonitorSubsystemCapability.DeviceOnlineEvent;
import com.arcussmarthome.platform.history.HistoryAppenderDAO;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.history.appender.AnnotatedAppender;
import com.arcussmarthome.platform.history.appender.MessageContext;
import com.arcussmarthome.platform.history.appender.ObjectNameCache;
import com.arcussmarthome.platform.history.appender.annotation.AutoTranslate;
import com.arcussmarthome.platform.history.appender.annotation.Event;
import com.arcussmarthome.platform.history.appender.annotation.Group;
import com.arcussmarthome.platform.history.appender.annotation.Values;
import com.arcussmarthome.platform.history.appender.matcher.MatchResults;

@Singleton
@AutoTranslate
@Group(PlaceMonitorSubsystemCapability.NAMESPACE)
@Event(event = DeviceOnlineEvent.NAME,  tpl = "device.connection.online",  critical = false)
@Event(event = DeviceOfflineEvent.NAME, tpl = "device.connection.offline", critical = false)
@Values({ PLACE_NAME })
public class PlaceMonitorSubsystemAppender extends AnnotatedAppender
{
   @Inject
   protected PlaceMonitorSubsystemAppender(HistoryAppenderDAO appender, ObjectNameCache cache)
   {
      super(appender, cache);
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults)
   {
      Address deviceAddress = null;

      switch (message.getMessageType())
      {
         case DeviceOnlineEvent.NAME:
            deviceAddress = Address.fromString(DeviceOnlineEvent.getDeviceAddress(message.getValue()));
            break;
         case DeviceOfflineEvent.NAME:
            deviceAddress = Address.fromString(DeviceOfflineEvent.getDeviceAddress(message.getValue()));
            break;
         default:
            throw new IllegalArgumentException("Unexpected message type: " + message.getMessageType());
      }

      context.setDeviceId((UUID) deviceAddress.getId());
      context.setDeviceName(getDeviceNameFromAddress(deviceAddress));

      return super.translate(message, context, matchResults);
   }
}

