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

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import com.iris.driver.DeviceDriverContext;
import com.iris.driver.handler.ContextualEventHandler;
import com.iris.messages.MessageBody;
import com.iris.messages.PlatformMessage;
import com.iris.messages.address.Address;
import com.iris.messages.address.DeviceProtocolAddress;
import com.iris.messages.capability.DeviceAdvancedCapability;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetRequest;
import com.iris.messages.capability.DeviceAdvancedCapability.SendZWaveGetResponse;
import com.iris.messages.errors.Errors;
import com.iris.protocol.zwave.ZWaveProtocol;
import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Handles devadv:SendZWaveGet, which sends one Z-Wave Get that the device's driver doesn't,
 * such as Wake Up Interval Get, without deploying a driver. It goes to the hub like any driver
 * command, so the hub holds it for a sleeping device until it wakes. The request is recorded
 * as pending in devadv:zwaveGet, and {@link ZWaveGetReplyListener} fills in the answer.
 *
 * Only Gets are sent. A Get only reads, so it can't unlock a door or change a setting behind
 * the driver's back. Security Gets are left to the hub's own security layer, Firmware Update
 * Meta Data has a Get that starts an update, and a User Code Get answers with a code, which
 * would be stored where every client of the place can read it.
 */
public class ZWaveGetHandler implements ContextualEventHandler<Object> {
   static final String KEY_COMMAND_CLASS = "commandClass";
   static final String KEY_COMMAND = "command";
   static final String KEY_ARGS = "args";
   static final String KEY_STATUS = "status";
   static final String KEY_REQUESTED = "requested";
   static final String KEY_REPLY = "reply";
   static final String KEY_VALUES = "values";
   static final String KEY_BYTES = "bytes";
   static final String KEY_REPLIED = "replied";

   static final String STATUS_PENDING = "PENDING";
   static final String STATUS_REPLIED = "REPLIED";
   static final String STATUS_EXPIRED = "EXPIRED";

   private static final byte SECURITY = (byte) 0x98;
   private static final byte SECURITY_2 = (byte) 0x9F;
   private static final byte FIRMWARE_UPDATE_MD = (byte) 0x7A;
   private static final byte USER_CODE = (byte) 0x63;
   private static final Set<Byte> REFUSED_CLASSES =
         Collections.unmodifiableSet(new HashSet<>(Arrays.asList(SECURITY, SECURITY_2, FIRMWARE_UPDATE_MD, USER_CODE)));

   private final LongSupplier clock;

   public ZWaveGetHandler() {
      this(System::currentTimeMillis);
   }

   ZWaveGetHandler(LongSupplier clock) {
      this.clock = clock;
   }

   @Override
   public boolean handleEvent(DeviceDriverContext context, Object event) throws Exception {
      MessageBody request;
      if (event instanceof PlatformMessage) {
         request = ((PlatformMessage) event).getValue();
      } else if (event instanceof MessageBody) {
         request = (MessageBody) event;
      } else {
         return false;
      }

      if (!isZWave(context.getProtocolAddress())) {
         context.respondToPlatform(Errors.invalidRequest(SendZWaveGetRequest.NAME + " only works on Z-Wave devices"));
         return true;
      }

      String className = SendZWaveGetRequest.getCommandClass(request);
      String commandName = SendZWaveGetRequest.getCommand(request);
      Map<String, Integer> args = SendZWaveGetRequest.getArgs(request, Collections.<String, Integer>emptyMap());
      if (className == null || commandName == null) {
         context.respondToPlatform(Errors.missingParam(className == null ? SendZWaveGetRequest.ATTR_COMMANDCLASS : SendZWaveGetRequest.ATTR_COMMAND));
         return true;
      }

      ZWaveCommandClass commandClass = ZWaveAllCommandClasses.getClass(className);
      if (commandClass == null) {
         context.respondToPlatform(Errors.invalidParam(SendZWaveGetRequest.ATTR_COMMANDCLASS));
         return true;
      }
      if (REFUSED_CLASSES.contains(commandClass.number)) {
         context.respondToPlatform(Errors.invalidRequest("Gets in the " + commandClass.name + " command class aren't sent"));
         return true;
      }
      ZWaveCommand template = commandClass.get(commandName);
      if (template == null) {
         context.respondToPlatform(Errors.invalidParam(SendZWaveGetRequest.ATTR_COMMAND));
         return true;
      }
      if (!isGet(template)) {
         context.respondToPlatform(Errors.invalidRequest("Only Gets are sent, and " + commandClass.name + " " + template.commandName + " isn't one"));
         return true;
      }

      for (Map.Entry<String, Integer> arg : args.entrySet()) {
         // Each is sent as one byte, which would quietly wrap anything outside it
         if (arg.getValue() == null || arg.getValue() < 0 || arg.getValue() > 0xFF) {
            context.respondToPlatform(Errors.invalidRequest("Argument " + arg.getKey() + " must be 0 to 255"));
            return true;
         }
      }

      ZWaveCommand command;
      try {
         command = ZWaveContext.doCreateZWaveCommand(commandClass, template, args);
      } catch (IllegalArgumentException e) {
         context.respondToPlatform(Errors.invalidRequest(e.getMessage()));
         return true;
      }

      ZWaveContext.doSendZWaveCommand(context, command);

      Map<String, String> pending = new LinkedHashMap<>();
      pending.put(KEY_COMMAND_CLASS, className);
      pending.put(KEY_COMMAND, commandName);
      if (!args.isEmpty()) {
         pending.put(KEY_ARGS, args.entrySet().stream()
               .map(arg -> arg.getKey() + "=" + arg.getValue())
               .collect(Collectors.joining(", ")));
      }
      pending.put(KEY_STATUS, STATUS_PENDING);
      pending.put(KEY_REQUESTED, Instant.ofEpochMilli(clock.getAsLong()).toString());
      context.setAttributeValue(DeviceAdvancedCapability.KEY_ZWAVEGET, pending);

      String expected = describe(commandClass, expectedReplies(commandClass, template));
      context.getLogger().info("Sent Z-Wave {} {} for {}, expecting {}",
            commandClass.name, template.commandName, SendZWaveGetRequest.NAME, expected);
      context.respondToPlatform(SendZWaveGetResponse.builder()
            .withStatus(SendZWaveGetResponse.STATUS_SENT)
            .withExpectedReply(expected)
            .build());
      return true;
   }

   static boolean isZWave(Address address) {
      return address instanceof DeviceProtocolAddress
            && ZWaveProtocol.NAMESPACE.equals(((DeviceProtocolAddress) address).getProtocolName());
   }

   static boolean isGet(ZWaveCommand command) {
      return command.commandName != null && command.commandName.toLowerCase(Locale.ROOT).endsWith("get");
   }

   /**
    * The commands that answer a Get: the one the command model names as its response, else the
    * report named after it (interval_capabilities_get is answered by interval_capabilities_report),
    * else any report in the class. The class's other commands, such as a Wake Up Notification,
    * aren't answers.
    */
   static List<ZWaveCommand> expectedReplies(ZWaveCommandClass commandClass, ZWaveCommand get) {
      if (get.response_id != 0) {
         ZWaveCommand response = commandClass.get(get.response_id);
         if (response != null) {
            return Collections.singletonList(response);
         }
      }
      String name = get.commandName;
      ZWaveCommand named = commandClass.get(name.substring(0, name.length() - "get".length()) + "report");
      if (named != null) {
         return Collections.singletonList(named);
      }
      List<ZWaveCommand> reports = new ArrayList<>();
      for (ZWaveCommand command : commandClass.commandsByName.values()) {
         if (command.commandName.toLowerCase(Locale.ROOT).endsWith("report")) {
            reports.add(command);
         }
      }
      return reports;
   }

   static String describe(ZWaveCommandClass commandClass, List<ZWaveCommand> commands) {
      if (commands.isEmpty()) {
         return commandClass.name + " (no report in the command model)";
      }
      return commands.stream()
            .map(command -> commandClass.name + " " + command.commandName)
            .collect(Collectors.joining(" or "));
   }
}
