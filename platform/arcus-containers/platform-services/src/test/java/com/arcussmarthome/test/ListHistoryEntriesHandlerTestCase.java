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
package com.arcussmarthome.test;

import java.util.List;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.arcussmarthome.capability.attribute.transform.AttributeMapTransformModule;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.model.BaseEntity;
import com.arcussmarthome.messages.model.Entity;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.platform.PagedResults;
import com.arcussmarthome.platform.history.HistoryLogDAO;
import com.arcussmarthome.platform.history.HistoryLogDAO.ListEntriesQuery;
import com.arcussmarthome.platform.history.HistoryLogEntry;
import com.arcussmarthome.platform.history.HistoryLogEntryType;

/**
 * 
 */
@Mocks(HistoryLogDAO.class)
@Modules(AttributeMapTransformModule.class)
public abstract class ListHistoryEntriesHandlerTestCase<T extends Entity<?, T>> extends IrisMockTestCase {
   @Inject HistoryLogDAO mockLogDao;
   
   T context;
   
   @Before
   public void setUp() throws Exception {
      super.setUp();
      context = createContext();
   }
   
   protected abstract T createContext();
   
   protected abstract int getDefaultLimit();
   
   protected abstract MessageBody handleRequest(T context, PlatformMessage message);
   
   protected Address getAddress(T context) {
      return Address.fromString(((BaseEntity<?, ?>) context).getAddress());
   }
   
   protected ListEntriesQuery createQuery() {
      return createQuery(getDefaultLimit(), null);
   }
   
   protected ListEntriesQuery createQuery(Integer limit, String token) {
      ListEntriesQuery query = new ListEntriesQuery();
      query.setType(getType());
      query.setId(context.getId());
      query.setLimit(limit);
      query.setToken(token);
      return query;
   }
   
   protected PlatformMessage createRequest(T context) {
      return createRequest(context, null, null);
   }

   protected PlatformMessage createRequest(T context, Integer limit, String token) {
      MessageBody payload = createListRequest(context, limit, token);
      return
            PlatformMessage
               .builder()
               .from(Fixtures.createClientAddress())
               .to(getAddress(context))
               .withPayload(payload)
               .create();
   }
   
   protected abstract MessageBody createListRequest(T context, Integer limit, String token);
   
   protected abstract HistoryLogEntryType getType();

   protected void assertExpectedLogs(List<Map<String, Object>> results) {
      assertEquals(3, results.size());
      Map<String, Object> entry = results.get(0);
      assertNotNull(entry.get("timestamp"));
      // TODO other assertions
   }

   @Test
   public void testNullPlace() {
      replay();
      
      try {
         handleRequest(null, createRequest(context));
         fail();
      }
      catch(Exception e) {
         // expected
      }
      
      verify();
   }

   @Test
   public void testDefaults() {
      ListEntriesQuery query = createQuery();
      
      PagedResults<HistoryLogEntry> result = createResults();
      EasyMock
         .expect(mockLogDao.listEntriesByQuery(query))
         .andReturn(result)
         ;
      replay();
      
      PlatformMessage request = createRequest(context);
      MessageBody message = handleRequest(context, request);
      assertEquals(request.getMessageType() + "Response", message.getMessageType());
      assertEquals(result.getNextToken(), message.getAttributes().get("nextToken"));
      assertExpectedLogs((List<Map<String, Object>>)message.getAttributes().get("results"));
      
      verify();
   }
   
   @Test
   public void testSpecifics() {
      String token = String.valueOf(System.currentTimeMillis());
      ListEntriesQuery query = createQuery(3, token);
      
      PagedResults<HistoryLogEntry> result = createResults(token);
      EasyMock
         .expect(mockLogDao.listEntriesByQuery(query))
         .andReturn(result)
         ;
      replay();
      
      PlatformMessage request = createRequest(context, 3, token);
      MessageBody message = handleRequest(context, request);
      assertEquals(request.getMessageType() + "Response", message.getMessageType());
      assertEquals(result.getNextToken(), message.getAttributes().get("nextToken"));
      assertExpectedLogs((List<Map<String, Object>>)message.getAttributes().get("results"));
      
      verify();
   }
   
   private PagedResults<HistoryLogEntry> createResults() {
      return createResults(null);
   }
   
   private PagedResults<HistoryLogEntry> createResults(String nextToken) {
      HistoryLogEntry entry = new HistoryLogEntry();
      entry.setId(context.getId());
      entry.setType(getType());
      entry.setMessageKey("test.key");
      entry.setTimestamp(System.currentTimeMillis());
      
      return PagedResults.newPage(ImmutableList.of(entry.copy(), entry.copy(), entry.copy()), nextToken);
   }
}

