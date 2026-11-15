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
package com.iris.driver.groovy.zwave;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.protocol.ProtocolMessage;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.message.ZWaveCommandMessage;
import com.iris.protocol.zwave.message.ZWaveMessage;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Records a Z-Wave device's answer to devadv:SendZWaveGet in devadv:zwaveGet, and logs it.
 *
 * Registered as a protocol listener, so it sees every message before the driver's own
 * handlers, whether or not the driver handles the answer, and doesn't stop them from running.
 * The first answer after the request is recorded. A request is given 48 hours, so a device
 * that wakes every 12 hours, or less often than asked, still answers in time; after that it's
 * marked EXPIRED when the device is next heard from.
 */
public class ZWaveGetReplyListener implements ContextualEventHandler<Object> {
   static final long EXPIRES_MS = TimeUnit.HOURS.toMillis(48);

   private final LongSupplier clock;

   public ZWaveGetReplyListener() {
      this(System::currentTimeMillis);
   }

   ZWaveGetReplyListener(LongSupplier clock) {
      this.clock = clock;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      if (!(event instanceof ProtocolMessage)) {
         return false;
      }
      // A protocol listener gets messages of every protocol
      ProtocolMessage message = (ProtocolMessage) event;
      if (!ZWaveProtocol.NAMESPACE.equals(message.getMessageType())) {
         return false;
      }
      Map<String, String> pending = context.getAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET);
      if (pending == null || !ZWaveGetHandler.STATUS_PENDING.equals(pending.get(ZWaveGetHandler.KEY_STATUS))) {
         return false;
      }

      if (isExpired(pending)) {
         Map<String, String> expired = new LinkedHashMap<>(pending);
         expired.put(ZWaveGetHandler.KEY_STATUS, ZWaveGetHandler.STATUS_EXPIRED);
         context.setAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET, expired);
         context.getLogger().info("No answer to Z-Wave {} {} within {} hours",
               pending.get(ZWaveGetHandler.KEY_COMMAND_CLASS), pending.get(ZWaveGetHandler.KEY_COMMAND),
               TimeUnit.MILLISECONDS.toHours(EXPIRES_MS));
         return false;
      }

      ZWaveMessage zwave = message.getValue(ZWaveProtocol.INSTANCE);
      if (!(zwave instanceof ZWaveCommandMessage)) {
         return false;
      }
      ZWaveCommand received = ((ZWaveCommandMessage) zwave).getCommand();
      ZWaveCommandClass commandClass = ZWaveAllCommandClasses.getClass(pending.get(ZWaveGetHandler.KEY_COMMAND_CLASS));
      ZWaveCommand get = commandClass == null ? null : commandClass.get(pending.get(ZWaveGetHandler.KEY_COMMAND));
      if (received == null || get == null || received.commandClass != commandClass.number) {
         return false;
      }
      ZWaveCommand reply = null;
      for (ZWaveCommand expected : ZWaveGetHandler.expectedReplies(commandClass, get)) {
         if (expected.commandNumber == received.commandNumber) {
            reply = expected;
            break;
         }
      }
      if (reply == null) {
         return false;
      }

      byte[] payload = received.recvBytes == null ? new byte[0] : received.recvBytes;
      Map<String, String> replied = new LinkedHashMap<>(pending);
      replied.put(ZWaveGetHandler.KEY_STATUS, ZWaveGetHandler.STATUS_REPLIED);
      replied.put(ZWaveGetHandler.KEY_REPLY, commandClass.name + " " + reply.commandName);
      String values = values(reply.receiveNames, payload);
      if (!values.isEmpty()) {
         replied.put(ZWaveGetHandler.KEY_VALUES, values);
      }
      replied.put(ZWaveGetHandler.KEY_BYTES, hex(payload));
      replied.put(ZWaveGetHandler.KEY_REPLIED, Instant.ofEpochMilli(clock.getAsLong()).toString());
      context.setAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET, replied);
      context.getLogger().info("Answer to Z-Wave {} {}: {} {}",
            commandClass.name, get.commandName, replied.get(ZWaveGetHandler.KEY_REPLY),
            values.isEmpty() ? hex(payload) : values);
      return false;
   }

   private boolean isExpired(Map<String, String> pending) {
      String requested = pending.get(ZWaveGetHandler.KEY_REQUESTED);
      try {
         return requested == null || clock.getAsLong() - Instant.parse(requested).toEpochMilli() > EXPIRES_MS;
      } catch (DateTimeException e) {
         return true;
      }
   }

   /** The payload by the names the command model gives its bytes, in order, as unsigned numbers */
   static String values(List<String> names, byte[] payload) {
      List<String> values = new ArrayList<>();
      for (int i = 0; i < names.size() && i < payload.length; i++) {
         values.add(names.get(i) + "=" + (payload[i] & 0xFF));
      }
      return String.join(", ", values);
   }

   static String hex(byte[] bytes) {
      StringBuilder sb = new StringBuilder();
      for (byte b : bytes) {
         if (sb.length() > 0) {
            sb.append(' ');
         }
         sb.append(String.format("%02X", b & 0xFF));
      }
      return sb.toString();
   }
}
