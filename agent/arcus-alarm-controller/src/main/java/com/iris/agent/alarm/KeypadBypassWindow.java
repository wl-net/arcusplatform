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
package com.iris.agent.alarm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import com.iris.messages.address.Address;

/**
 * Tracks, per keypad, the window during which a repeated arm press arms with bypass.
 *
 * Keypads always report ArmPressed with bypass=false. When the platform handles the press and the
 * arm fails because devices are tripped or offline, it opens a short window for that keypad and
 * treats a second press within it as an arm with bypass (see HubAlarmSubsystem.armKeypad and
 * AlarmUtil.keypadArmBypassedTimeoutMs). The hub uses the same rule when arming offline.
 */
final class KeypadBypassWindow {
   static final long DEFAULT_WINDOW_MS = 4000;

   private final long windowMs;
   private final LongSupplier clock;
   private final Map<Address, Long> openUntil = new ConcurrentHashMap<>();

   KeypadBypassWindow() {
      this(DEFAULT_WINDOW_MS, System::currentTimeMillis);
   }

   KeypadBypassWindow(long windowMs, LongSupplier clock) {
      this.windowMs = windowMs;
      this.clock = clock;
   }

   void open(Address keypad) {
      openUntil.put(keypad, clock.getAsLong() + windowMs);
   }

   boolean isOpen(Address keypad) {
      Long until = openUntil.get(keypad);
      return until != null && until > clock.getAsLong();
   }

   void close(Address keypad) {
      openUntil.remove(keypad);
   }
}
