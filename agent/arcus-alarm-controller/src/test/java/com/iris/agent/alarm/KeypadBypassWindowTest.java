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

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.iris.messages.address.Address;

@RunWith(JUnit4.class)
public class KeypadBypassWindowTest {
   private final AtomicLong now = new AtomicLong(1_000_000L);
   private final Address keypad = Address.platformDriverAddress(UUID.randomUUID());
   private final Address otherKeypad = Address.platformDriverAddress(UUID.randomUUID());

   private KeypadBypassWindow window;

   @Before
   public void setUp() {
      window = new KeypadBypassWindow(4000, now::get);
   }

   @Test
   public void testClosedByDefault() {
      Assert.assertFalse(window.isOpen(keypad));
   }

   @Test
   public void testOpenWithinWindow() {
      window.open(keypad);
      now.addAndGet(3999);
      Assert.assertTrue(window.isOpen(keypad));
   }

   @Test
   public void testExpiresAfterWindow() {
      window.open(keypad);
      now.addAndGet(4000);
      Assert.assertFalse(window.isOpen(keypad));
   }

   @Test
   public void testPerKeypad() {
      window.open(keypad);
      Assert.assertTrue(window.isOpen(keypad));
      Assert.assertFalse(window.isOpen(otherKeypad));
   }

   @Test
   public void testClose() {
      window.open(keypad);
      window.close(keypad);
      Assert.assertFalse(window.isOpen(keypad));
   }

   @Test
   public void testReopenExtendsWindow() {
      window.open(keypad);
      now.addAndGet(3000);
      window.open(keypad);
      now.addAndGet(3000);
      Assert.assertTrue(window.isOpen(keypad));
   }
}
