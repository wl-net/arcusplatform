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
package com.arcussmarthome.client.server.session;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.bridge.server.session.SessionListener;
import com.arcussmarthome.core.dao.HubDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.HubAdvancedCapability;
import com.arcussmarthome.messages.model.Hub;
import com.arcussmarthome.messages.service.SessionService;

@Singleton
public class StopCameraPreviewsUploadSessionListener implements SessionListener {

   private final PlatformMessageBus bus;
   private final HubDAO hubDao;

   @Inject
   public StopCameraPreviewsUploadSessionListener(PlatformMessageBus bus, HubDAO hubDao) {
      this.bus = bus;
      this.hubDao = hubDao;
   }

   @Override
   public void onSessionCreated(Session session) {
      // no op
   }

   @Override
   public void onSessionDestroyed(Session session) {
      sendStopCameraPreviewsToHub(session.getActivePlace());
   }

   private void sendStopCameraPreviewsToHub(String placeId) {
      if(StringUtils.isBlank(placeId)) {
         return;
      }
      Hub hub = hubDao.findHubForPlace(UUID.fromString(placeId));
      if(hub == null) {
         return;
      }
      MessageBody body = HubAdvancedCapability.StopUploadingCameraPreviewsEvent.instance();
      PlatformMessage msg = PlatformMessage.create(body,
            Address.platformService(SessionService.NAMESPACE),
            Address.fromString(hub.getAddress()),
            null);
      bus.send(msg);
   }

}

