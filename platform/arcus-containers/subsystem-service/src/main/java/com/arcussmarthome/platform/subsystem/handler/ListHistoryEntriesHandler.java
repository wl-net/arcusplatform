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
package com.arcussmarthome.platform.subsystem.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.arcussmarthome.capability.attribute.transform.BeanAttributesTransformer;
import com.arcussmarthome.capability.attribute.transform.BeanListTransformer;
import com.arcussmarthome.common.subsystem.SubsystemContext;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.capability.AlarmIncidentCapability;
import com.arcussmarthome.messages.capability.SubsystemCapability.ListHistoryEntriesRequest;
import com.arcussmarthome.messages.capability.SubsystemCapability.ListHistoryEntriesResponse;
import com.arcussmarthome.messages.listener.annotation.Request;
import com.arcussmarthome.messages.model.subs.SubsystemModel;
import com.arcussmarthome.platform.PagedResults;
import com.arcussmarthome.platform.history.HistoryLogDAO;
import com.arcussmarthome.platform.history.HistoryLogDAO.ListEntriesQuery;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.history.HistoryLogEntryType;
import com.arcussmarthome.platform.history.SubsystemId;

@Singleton
public class ListHistoryEntriesHandler {
   public static final int DFLT_LIMIT = 10;
   
   private final BeanListTransformer<HistoryLogEntry> transformer;
   private final HistoryLogDAO logDao;

   @Inject
   public ListHistoryEntriesHandler(
         BeanAttributesTransformer<HistoryLogEntry> transformer,
         HistoryLogDAO logDao
   ) {
      this.transformer = new BeanListTransformer<>(transformer);
      this.logDao = logDao;
   }

   // Note, injecting the SubsystemContext instead of just looking at the address
   // ensures that the context exists and matches the right place
   @Request(ListHistoryEntriesRequest.NAME)
   public MessageBody handleRequest(SubsystemContext<? extends SubsystemModel> context, PlatformMessage msg) {
      MessageBody request = msg.getValue();
      String token = ListHistoryEntriesRequest.getToken(request);
      Integer limit = ListHistoryEntriesRequest.getLimit(request);
      if(limit == null) {
         limit = DFLT_LIMIT;
      }
      
      ListEntriesQuery query = new ListEntriesQuery();
      query.setType(HistoryLogEntryType.DETAILED_SUBSYSTEM_LOG);
      query.setId(new SubsystemId(context.model().getAddress()));
      query.setLimit(limit);
      query.setToken(token);
      
      PagedResults<HistoryLogEntry> results = logDao.listEntriesByQuery(query);
      if(!Boolean.TRUE.equals(ListHistoryEntriesRequest.getIncludeIncidents(request))) {
	      for(HistoryLogEntry result: results.getResults()) {
	      	if(result.getSubjectAddress().contains(AlarmIncidentCapability.NAMESPACE)) {
	      		result.setSubjectAddress(context.model().getAddress().getRepresentation());
	      	}
	      }
      }
      return
            ListHistoryEntriesResponse
               .builder()
               .withNextToken(results.getNextToken())
               .withResults(transformer.convertListToAttributes(results.getResults()))
               .build();
   }

}

