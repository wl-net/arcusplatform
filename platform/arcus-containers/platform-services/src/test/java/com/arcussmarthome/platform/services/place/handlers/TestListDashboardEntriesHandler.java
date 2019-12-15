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
/**
 * 
 */
package com.arcussmarthome.platform.services.place.handlers;

import java.util.Date;
import java.util.UUID;

import com.google.inject.Inject;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.platform.history.HistoryLogEntryType;
import com.arcussmarthome.test.ListHistoryEntriesHandlerTestCase;

/**
 * 
 */
public class TestListDashboardEntriesHandler extends ListHistoryEntriesHandlerTestCase<Place> {
   @Inject ListDashboardEntriesHandler handler;

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#createContext()
    */
   @Override
   protected Place createContext() {
      Place place = Fixtures.createPlace();
      place.setId(UUID.randomUUID());
      place.setCreated(new Date());
      place.setModified(new Date());
      return place;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#handleRequest(com.iris.messages.model.BaseEntity, com.iris.messages.PlatformMessage)
    */
   @Override
   protected MessageBody handleRequest(Place context, PlatformMessage message) {
      return handler.handleRequest(context, message);
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#createListRequest(com.iris.messages.model.BaseEntity, java.lang.Integer, java.lang.String)
    */
   @Override
   protected MessageBody createListRequest(Place context, Integer limit, String token) {
      return 
            PlaceCapability.ListDashboardEntriesRequest
               .builder()
               .withLimit(limit)
               .withToken(token)
               .build();
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#getType()
    */
   @Override
   protected HistoryLogEntryType getType() {
      return HistoryLogEntryType.CRITICAL_PLACE_LOG;
   }

   /* (non-Javadoc)
    * @see com.iris.platform.services.ListHistoryEntriesHandlerTestCase#getDefaultLimit()
    */
   @Override
   protected int getDefaultLimit() {
      return ListHistoryEntriesHandler.DFLT_LIMIT;
   }
   
}

