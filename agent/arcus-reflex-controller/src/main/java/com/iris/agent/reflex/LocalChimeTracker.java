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
package com.iris.agent.reflex;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;

/**
 * Pairs the chimes the hub plays on its own with the chime requests the
 * platform sends for the same contact sensor, so each door opening chimes
 * once.
 *
 * The platform keeps sending hubchime:chime for every chime-enabled sensor,
 * because only some drivers set cont:contact on the hub. When the hub has
 * already chimed for a sensor, the matching platform request is dropped. Each
 * local chime pairs with exactly one platform request, so open, close, open
 * plays twice locally and drops two platform requests.
 *
 * Time never decides whether a request is a duplicate. It only clears entries
 * the platform will never answer, for example a door that opened and closed
 * while the hub was offline. Entries only age while the hub is connected to
 * the platform: an entry made offline, or still waiting when the hub goes
 * offline, waits until the hub reconnects and then gets the full expiry, so
 * a platform catching up after reconnect does not chime a second time.
 */
public final class LocalChimeTracker {
   public static final long DEFAULT_EXPIRY_NANOS = TimeUnit.SECONDS.toNanos(60);

   // A sensor opened and closed many times while the hub is offline should not
   // grow without bound. The platform sees at most one opening on reconnect.
   static final int MAX_PENDING_PER_DEVICE = 32;

   private final long expiryNanos;
   private final Map<String,Deque<Pending>> pending = new HashMap<>();
   private boolean connected;

   public LocalChimeTracker(long expiryNanos, boolean connected) {
      this.expiryNanos = expiryNanos;
      this.connected = connected;
   }

   /**
    * Records that the hub chimed for the given sensor.
    */
   public synchronized void localChimePlayed(String device, long nowNanos) {
      Deque<Pending> entries = pending.computeIfAbsent(device, (d) -> new ArrayDeque<>());
      if (entries.size() >= MAX_PENDING_PER_DEVICE) {
         entries.removeFirst();
      }

      entries.addLast(connected ? Pending.expiringAt(nowNanos + expiryNanos) : Pending.waitingForPlatform());
   }

   /**
    * Called for each hubchime:chime request from the platform.
    *
    * @return true if the request matches a chime the hub already played and
    *         should be dropped, false if the hub should chime
    */
   public synchronized boolean consumeLocalChime(@Nullable String device, long nowNanos) {
      purgeExpired(nowNanos);
      if (device == null) {
         return false;
      }

      Deque<Pending> entries = pending.get(device);
      if (entries == null || entries.isEmpty()) {
         return false;
      }

      entries.removeFirst();
      if (entries.isEmpty()) {
         pending.remove(device);
      }
      return true;
   }

   /**
    * Called when the hub connects to or disconnects from the platform.
    */
   public synchronized void connectionChanged(boolean nowConnected, long nowNanos) {
      if (connected == nowConnected) {
         return;
      }

      // While disconnected every entry waits, so on reconnect they all start
      // aging from now.
      connected = nowConnected;
      for (Deque<Pending> entries : pending.values()) {
         for (Pending entry : entries) {
            if (nowConnected) {
               entry.expireAt(nowNanos + expiryNanos);
            } else {
               entry.waitForPlatform();
            }
         }
      }
   }

   synchronized int pendingCount(String device) {
      Deque<Pending> entries = pending.get(device);
      return (entries == null) ? 0 : entries.size();
   }

   private void purgeExpired(long nowNanos) {
      Iterator<Deque<Pending>> it = pending.values().iterator();
      while (it.hasNext()) {
         Deque<Pending> entries = it.next();
         entries.removeIf((entry) -> entry.isExpired(nowNanos));
         if (entries.isEmpty()) {
            it.remove();
         }
      }
   }

   private static final class Pending {
      private boolean waiting;
      private long expiresAt;

      static Pending expiringAt(long expiresAt) {
         Pending entry = new Pending();
         entry.expireAt(expiresAt);
         return entry;
      }

      static Pending waitingForPlatform() {
         Pending entry = new Pending();
         entry.waitForPlatform();
         return entry;
      }

      void expireAt(long expiresAt) {
         this.waiting = false;
         this.expiresAt = expiresAt;
      }

      void waitForPlatform() {
         this.waiting = true;
      }

      boolean isExpired(long nowNanos) {
         return !waiting && nowNanos - expiresAt >= 0;
      }
   }
}
