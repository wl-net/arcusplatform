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
import static org.easymock.EasyMock.*;

import java.util.List;

import org.junit.Test;

import com.iris.driver.DeviceDriverContext;

public class TestZWaveAssociationUtil {

   @Test
   public void testExtractNodeIds() {
      byte[] bytes = new byte[]{0, 0, 0, 5, 7, 12};
      List<Integer> nodes = ZWaveAssociationUtil.extractNodeIds(bytes, 3);
      assertEquals(3, nodes.size());
      assertEquals(Integer.valueOf(5), nodes.get(0));
      assertEquals(Integer.valueOf(7), nodes.get(1));
      assertEquals(Integer.valueOf(12), nodes.get(2));
   }

   @Test
   public void testExtractNodeIdsSkipsZeros() {
      byte[] bytes = new byte[]{0, 0, 0, 5, 0, 12};
      List<Integer> nodes = ZWaveAssociationUtil.extractNodeIds(bytes, 3);
      assertEquals(2, nodes.size());
      assertEquals(Integer.valueOf(5), nodes.get(0));
      assertEquals(Integer.valueOf(12), nodes.get(1));
   }

   @Test
   public void testExtractNodeIdsEmpty() {
      byte[] bytes = new byte[]{1, 2, 3};
      List<Integer> nodes = ZWaveAssociationUtil.extractNodeIds(bytes, 3);
      assertTrue(nodes.isEmpty());
   }

   @Test
   public void testBuildAssociationsJsonEmpty() {
      DeviceDriverContext context = createNiceMock(DeviceDriverContext.class);
      expect(context.getVariable("zwda.maxGroups")).andReturn(0);
      replay(context);

      String json = ZWaveAssociationUtil.buildAssociationsJson(context);
      assertEquals("{}", json);
      verify(context);
   }

   @Test
   public void testBuildAssociationsJsonWithData() {
      DeviceDriverContext context = createNiceMock(DeviceDriverContext.class);
      expect(context.getVariable("zwda.maxGroups")).andReturn(3);
      expect(context.getVariable("zwda.assoc.1")).andReturn(null);
      expect(context.getVariable("zwda.assoc.2")).andReturn("5, 7");
      expect(context.getVariable("zwda.assoc.3")).andReturn("12");
      replay(context);

      String json = ZWaveAssociationUtil.buildAssociationsJson(context);
      assertEquals("{\"2\": [5, 7], \"3\": [12]}", json);
      verify(context);
   }
}
