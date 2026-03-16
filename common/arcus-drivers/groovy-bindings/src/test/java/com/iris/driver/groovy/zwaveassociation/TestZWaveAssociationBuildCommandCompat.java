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
package com.iris.driver.groovy.zwaveassociation;

import static org.junit.Assert.*;

import org.junit.Test;

import com.iris.protocol.zwave.model.ZWaveAllCommandClasses;
import com.iris.protocol.zwave.model.ZWaveCommand;
import com.iris.protocol.zwave.model.ZWaveCommandClass;

/**
 * Verifies that ZWaveAssociationUtil.buildCommand produces byte-identical
 * output to the expected Z-Wave Association command format.
 */
public class TestZWaveAssociationBuildCommandCompat {

   private static final ZWaveCommandClass ASSOCIATION_CC = ZWaveAllCommandClasses.getClass("association");

   @Test
   public void testAssociationSetBytesAreExact() {
      byte group = 2;
      byte node = 5;
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "set", group, node);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, group, node] — 4 bytes
      assertEquals(4, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("set").commandNumber, bytes[1]);
      assertEquals(group, bytes[2]);
      assertEquals(node, bytes[3]);
   }

   @Test
   public void testAssociationGetBytesAreExact() {
      byte group = 3;
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "get", group);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, group] — 3 bytes
      assertEquals(3, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("get").commandNumber, bytes[1]);
      assertEquals(group, bytes[2]);
   }

   @Test
   public void testAssociationRemoveBytesAreExact() {
      byte group = 1;
      byte node = 7;
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "remove", group, node);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, group, node] — 4 bytes
      assertEquals(4, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("remove").commandNumber, bytes[1]);
      assertEquals(group, bytes[2]);
      assertEquals(node, bytes[3]);
   }

   @Test
   public void testAssociationRemoveGroupOnlyBytesAreExact() {
      byte group = 4;
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "remove", group);
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD, group] — 3 bytes (clear all nodes in group)
      assertEquals(3, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("remove").commandNumber, bytes[1]);
      assertEquals(group, bytes[2]);
   }

   @Test
   public void testGroupingsGetBytesAreExact() {
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "groupings_get");
      byte[] bytes = cmd.toBytes();

      // Must be exactly [CC, CMD] — 2 bytes, no payload
      assertEquals(2, bytes.length);
      assertEquals(ASSOCIATION_CC.number, bytes[0]);
      assertEquals(ASSOCIATION_CC.commandsByName.get("groupings_get").commandNumber, bytes[1]);
   }

   @Test
   public void testSetHasExactly2SendVars() {
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "set", (byte) 1, (byte) 2);
      assertEquals(2, cmd.sendVariables.size());
   }

   @Test
   public void testGetHasExactly1SendVar() {
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "get", (byte) 1);
      assertEquals(1, cmd.sendVariables.size());
   }

   @Test
   public void testGroupingsGetHasZeroSendVars() {
      ZWaveCommand cmd = ZWaveAssociationUtil.buildCommand(ASSOCIATION_CC, "groupings_get");
      assertEquals(0, cmd.sendVariables.size());
   }
}
