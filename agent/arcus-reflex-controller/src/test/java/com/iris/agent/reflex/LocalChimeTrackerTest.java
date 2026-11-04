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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class LocalChimeTrackerTest {
   private static final long EXPIRY = TimeUnit.SECONDS.toNanos(60);
   private static final String DOOR = "ZIGB:0000000000000001";
   private static final String OTHER_DOOR = "ZIGB:0000000000000002";

   // Start well away from zero so nanoTime-style arithmetic is exercised.
   private long now = Long.MAX_VALUE - TimeUnit.MINUTES.toNanos(5);
   private LocalChimeTracker tracker;

   @Before
   public void setUp() {
      tracker = new LocalChimeTracker(EXPIRY, true);
   }

   private void advance(long amount, TimeUnit unit) {
      now += unit.toNanos(amount);
   }

   @Test
   public void platformChimeWithNoLocalChimePlays() {
      assertFalse(tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void platformChimeAfterLocalChimeIsDropped() {
      tracker.localChimePlayed(DOOR, now);
      advance(1, TimeUnit.SECONDS);

      assertTrue(tracker.consumeLocalChime(DOOR, now));
      assertFalse("one local chime only cancels one platform chime", tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void openCloseOpenPairsEachChime() {
      tracker.localChimePlayed(DOOR, now);
      advance(2, TimeUnit.SECONDS);
      tracker.localChimePlayed(DOOR, now);
      assertEquals(2, tracker.pendingCount(DOOR));

      advance(1, TimeUnit.SECONDS);
      assertTrue(tracker.consumeLocalChime(DOOR, now));
      assertTrue(tracker.consumeLocalChime(DOOR, now));
      assertFalse(tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void platformChimeForAnotherSensorPlays() {
      tracker.localChimePlayed(DOOR, now);

      assertFalse(tracker.consumeLocalChime(OTHER_DOOR, now));
      assertEquals(1, tracker.pendingCount(DOOR));
   }

   @Test
   public void platformChimeWithoutDevicePlaysAndKeepsPending() {
      tracker.localChimePlayed(DOOR, now);

      assertFalse("an older platform names no sensor", tracker.consumeLocalChime(null, now));
      assertEquals(1, tracker.pendingCount(DOOR));
   }

   @Test
   public void onlineLocalChimeExpires() {
      tracker.localChimePlayed(DOOR, now);

      advance(59, TimeUnit.SECONDS);
      assertEquals(1, tracker.pendingCount(DOOR));
      advance(1, TimeUnit.SECONDS);
      assertFalse(tracker.consumeLocalChime(DOOR, now));
      assertEquals(0, tracker.pendingCount(DOOR));
   }

   @Test
   public void offlineLocalChimeWaitsForReconnect() {
      tracker.connectionChanged(false, now);
      tracker.localChimePlayed(DOOR, now);

      advance(6, TimeUnit.HOURS);
      tracker.connectionChanged(true, now);
      advance(30, TimeUnit.SECONDS);

      assertTrue("the platform catching up after reconnect must not chime again", tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void offlineLocalChimeExpiresAfterReconnect() {
      tracker.connectionChanged(false, now);
      tracker.localChimePlayed(DOOR, now);

      advance(6, TimeUnit.HOURS);
      tracker.connectionChanged(true, now);
      advance(60, TimeUnit.SECONDS);

      assertFalse(tracker.consumeLocalChime(DOOR, now));
      assertEquals(0, tracker.pendingCount(DOOR));
   }

   @Test
   public void onlineLocalChimeStopsAgingWhileOffline() {
      tracker.localChimePlayed(DOOR, now);
      advance(10, TimeUnit.SECONDS);
      tracker.connectionChanged(false, now);

      advance(10, TimeUnit.MINUTES);
      tracker.connectionChanged(true, now);
      advance(59, TimeUnit.SECONDS);

      assertTrue(tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void startsDisconnectedUntilTold() {
      LocalChimeTracker offline = new LocalChimeTracker(EXPIRY, false);
      offline.localChimePlayed(DOOR, now);

      advance(1, TimeUnit.HOURS);
      assertTrue(offline.consumeLocalChime(DOOR, now));
   }

   @Test
   public void repeatedConnectionEventsDoNotResetExpiry() {
      tracker.localChimePlayed(DOOR, now);
      advance(50, TimeUnit.SECONDS);
      tracker.connectionChanged(true, now);
      advance(10, TimeUnit.SECONDS);

      assertFalse(tracker.consumeLocalChime(DOOR, now));
   }

   @Test
   public void pendingChimesPerSensorAreCapped() {
      tracker.connectionChanged(false, now);
      for (int i = 0; i < LocalChimeTracker.MAX_PENDING_PER_DEVICE + 10; i++) {
         tracker.localChimePlayed(DOOR, now);
         advance(1, TimeUnit.SECONDS);
      }

      assertEquals(LocalChimeTracker.MAX_PENDING_PER_DEVICE, tracker.pendingCount(DOOR));
   }
}
