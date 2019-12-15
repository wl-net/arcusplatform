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
package com.arcussmarthome.security.authz.filter;

import java.util.UUID;

import org.junit.Test;

import com.google.inject.Inject;
import com.arcussmarthome.messages.MessagesModule;
import com.arcussmarthome.security.authz.AuthzFixtures;
import com.arcussmarthome.security.authz.AuthzModule;
import com.arcussmarthome.test.IrisTestCase;
import com.arcussmarthome.test.Modules;

@Modules({MessagesModule.class, AuthzModule.class})
public class TestMessageFilterRegistry extends IrisTestCase {

   @Inject
   private MessageFilterRegistry registry;

   @Test
   public void testListDevices() {
      MessageFilter filter = registry.getMessageFilter(AuthzFixtures.createListDevices());
      assertTrue(filter instanceof ListDevicesResponseMessageFilter);
   }

   @Test
   public void testUnknown() {
      MessageFilter filter = registry.getMessageFilter(AuthzFixtures.createSwitchOn(UUID.randomUUID()));
      assertTrue(filter instanceof DefaultMessageFilter);
   }

}

