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
package com.arcussmarthome.platform.history.appender.devvc;

import static com.arcussmarthome.messages.capability.ThermostatCapability.ATTR_HVACMODE;
import static com.arcussmarthome.messages.capability.ThermostatCapability.HVACMODE_AUTO;
import static com.arcussmarthome.messages.capability.ThermostatCapability.HVACMODE_COOL;
import static com.arcussmarthome.messages.capability.ThermostatCapability.HVACMODE_HEAT;
import static com.arcussmarthome.messages.capability.ThermostatCapability.HVACMODE_OFF;
import static com.arcussmarthome.platform.history.appender.devvc.FanShutoffUtils.buildFanShutoffEntries;

import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.DeviceCapability;
import com.arcussmarthome.messages.capability.ThermostatCapability;
import com.arcussmarthome.platform.alarm.incident.AlarmIncidentDAO;
import com.arcussmarthome.platform.history.HistoryAppenderDAO;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.history.appender.AnnotatedDeviceValueChangeAppender;
import com.arcussmarthome.platform.history.appender.MessageContext;
import com.arcussmarthome.platform.history.appender.ObjectNameCache;
import com.arcussmarthome.platform.history.appender.annotation.AnyValue;
import com.arcussmarthome.platform.history.appender.annotation.AutoTranslate;
import com.arcussmarthome.platform.history.appender.annotation.EnumValue;
import com.arcussmarthome.platform.history.appender.annotation.Group;
import com.arcussmarthome.platform.history.appender.annotation.Values;
import com.arcussmarthome.platform.history.appender.matcher.MatchResults;
import com.arcussmarthome.platform.history.appender.translator.ValueGetter;

@Singleton
@Group(DeviceCapability.NAMESPACE)
@AutoTranslate()
@EnumValue(attr=ATTR_HVACMODE, 
				val={HVACMODE_AUTO, HVACMODE_OFF, HVACMODE_HEAT, HVACMODE_COOL}, 
				tpl="device.thermo.hvacmode.?", 
				critical=true)
@AnyValue(attr=ThermostatCapability.ATTR_HEATSETPOINT, tpl="device.thermo.heatsetpoint", critical=true)
@AnyValue(attr=ThermostatCapability.ATTR_COOLSETPOINT, tpl="device.thermo.coolsetpoint", critical=true)
@Values({ValueGetter.ATTR_VALUE_AS_FAHRENHEIT})
public class DeviceThermostatAppender extends AnnotatedDeviceValueChangeAppender
{
   private final AlarmIncidentDAO alarmIncidentDao;

   @Inject
   protected DeviceThermostatAppender(HistoryAppenderDAO appender, AlarmIncidentDAO alarmIncidentDao,
      ObjectNameCache cache)
   {
      super(appender, cache);

      this.alarmIncidentDao = alarmIncidentDao;
   }

   @Override
   protected List<HistoryLogEntry> translate(PlatformMessage message, MessageContext context, MatchResults matchResults)
   {
      // Call super first, so that context deviceId and deviceName get populated
      List<HistoryLogEntry> superEntries = super.translate(message, context, matchResults);

      String hvacMode = ThermostatCapability.getHvacmode(message.getValue());

      if (hvacMode != null && hvacMode.equals(HVACMODE_OFF))
      {
         Optional<List<HistoryLogEntry>> entriesOpt = buildFanShutoffEntries(message, context, alarmIncidentDao);

         if (entriesOpt.isPresent())
         {
            return entriesOpt.get();
         }
      }

      return superEntries;
   }
}

