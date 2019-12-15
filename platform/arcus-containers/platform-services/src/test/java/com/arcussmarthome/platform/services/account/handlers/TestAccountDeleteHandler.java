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
package com.arcussmarthome.platform.services.account.handlers;

import java.util.Collections;
import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.arcussmarthome.billing.client.BillingClient;
import com.arcussmarthome.billing.client.model.RecurlyError;
import com.arcussmarthome.billing.exception.RecurlyAPIErrorException;
import com.arcussmarthome.core.dao.AccountDAO;
import com.arcussmarthome.core.dao.AuthorizationGrantDAO;
import com.arcussmarthome.core.dao.PersonDAO;
import com.arcussmarthome.core.dao.PersonPlaceAssocDAO;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PreferencesDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.AccountCapability;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.model.Account;
import com.arcussmarthome.messages.model.Fixtures;
import com.arcussmarthome.messages.model.Person;
import com.arcussmarthome.messages.model.Place;
import com.arcussmarthome.messages.type.PlaceAccessDescriptor;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.platform.services.PersonDeleter;
import com.arcussmarthome.platform.services.PlaceDeleter;
import com.arcussmarthome.platform.subscription.SubscriptionUpdater;
import com.arcussmarthome.population.PlacePopulationCacheManager;
import com.arcussmarthome.security.authz.AuthorizationGrant;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({
   AccountDAO.class,
   PersonDAO.class,
   AuthorizationGrantDAO.class,
   PreferencesDAO.class,
   PersonPlaceAssocDAO.class,
   BillingClient.class,
   PlaceDAO.class,
   SubscriptionUpdater.class,
   PlacePopulationCacheManager.class
})
@Modules({InMemoryMessageModule.class})
public class TestAccountDeleteHandler extends IrisMockTestCase {

   private static final Address clientAddress = Address.clientAddress("test", "test");

   @Inject AccountDAO accountDao;
   @Inject PersonDAO personDao;
   @Inject AuthorizationGrantDAO authGrantDao;
   @Inject PreferencesDAO preferencesDao;
   @Inject BillingClient billingClient;
   @Inject PlaceDAO placeDao;
   @Inject PlacePopulationCacheManager mockPopulationCacheMgr;
   @Inject PersonPlaceAssocDAO personPlaceAssocDao;
   @Inject SubscriptionUpdater subUpdater;
   @Inject InMemoryPlatformMessageBus bus;

   private Account account;
   private Person person;
   private AccountDeleteHandler handler;
   private Place place;
   
   
   @Override
   public void setUp() throws Exception {
      super.setUp();
      account = new Account();
      account.setId(UUID.randomUUID());

      place = new Place();
      place.setAccount(account.getId());
      place.setId(UUID.randomUUID());
      
      account.setPlaceIDs(ImmutableSet.of(place.getId()));
      account.setOwner(UUID.randomUUID());

      person = Fixtures.createPerson();
      person.setId(account.getOwner());
      person.setAccountId(account.getId());

      PlaceDeleter placeDeleter = new PlaceDeleter(
            accountDao,
            placeDao,
            personDao,
            authGrantDao,
            preferencesDao,
            subUpdater,
            new PersonDeleter(accountDao, personDao, placeDao, authGrantDao, preferencesDao, bus, mockPopulationCacheMgr),
            bus);

      EasyMock.expect(mockPopulationCacheMgr.getPopulationByPlaceId(EasyMock.anyString())).andReturn(Population.NAME_GENERAL).anyTimes();
      
      handler = new AccountDeleteHandler(accountDao, personDao, personPlaceAssocDao, authGrantDao, preferencesDao, billingClient, placeDeleter,  bus, mockPopulationCacheMgr);
   }

   @Override
   public void tearDown() throws Exception {
      verify();
      super.tearDown();
   }

   @Test
   public void testDeleteWithNoBillingAccountDeletingLogin() throws Exception {
      assertSuccessfulDelete(true, false);
   }

   @Test
   public void testDeleteWithBillingAccountDeletingLogin() throws Exception {
      assertSuccessfulDelete(true, true);
   }

   @Test
   public void testDeleteWithNoBillingAccountNotLogin() throws Exception {
      assertSuccessfulDelete(false, false);
   }

   @Test
   public void testDeleteWithNoBillingAccountNotLoginNull() throws Exception {
      assertSuccessfulDelete(null, false);
   }

   @Test
   public void testDeleteWithBillingAccountNotLogin() throws Exception {
      assertSuccessfulDelete(false, true);
   }

   @Test
   public void testDeleteWithBillingAccountNotLoginNull() throws Exception {
      assertSuccessfulDelete(null, true);
   }

   @Test
   public void testFailsErrorFindingBillingAccount() throws Exception {
      EasyMock.expect(billingClient.getAccount(account.getId().toString())).andReturn(Futures.immediateFailedFuture(new Exception()));
      replay();

      MessageBody body = handler.handleRequest(account, createDelete(true));

      assertEquals("Error", body.getMessageType());
      assertEquals("account.close.failed", body.getAttributes().get("code"));
   }

   @Test
   public void testFailsErrorClosingBillingAccount() throws Exception {
      EasyMock.expect(billingClient.getAccount(account.getId().toString())).andReturn(Futures.immediateFuture(new com.arcussmarthome.billing.client.model.Account()));
      EasyMock.expect(billingClient.closeAccount(account.getId().toString())).andReturn(Futures.immediateFailedFuture(new Exception()));

      replay();

      MessageBody body = handler.handleRequest(account, createDelete(true));

      assertEquals("Error", body.getMessageType());
      assertEquals("account.close.failed", body.getAttributes().get("code"));
   }

   @Test
   public void testFailsClosingBillingAccountReturnsFalse() throws Exception {
      EasyMock.expect(billingClient.getAccount(account.getId().toString())).andReturn(Futures.immediateFuture(new com.arcussmarthome.billing.client.model.Account()));
      EasyMock.expect(billingClient.closeAccount(account.getId().toString())).andReturn(Futures.immediateFuture(false));

      replay();

      MessageBody body = handler.handleRequest(account, createDelete(true));

      assertEquals("Error", body.getMessageType());
      assertEquals("account.close.failed", body.getAttributes().get("code"));
   }

   private void assertSuccessfulDelete(Boolean deleteLogin, boolean billingExists) throws Exception {
      if(billingExists) {
         EasyMock.expect(billingClient.getAccount(account.getId().toString())).andReturn(Futures.immediateFuture(new com.arcussmarthome.billing.client.model.Account()));
         EasyMock.expect(billingClient.closeAccount(account.getId().toString())).andReturn(Futures.immediateFuture(true));
      } else {
         RecurlyError error = new RecurlyError();
         error.setErrorSymbol("not_found");
         RecurlyAPIErrorException exception = new RecurlyAPIErrorException("foo");
         exception.getErrors().add(error);
         EasyMock.expect(billingClient.getAccount(account.getId().toString())).andReturn(Futures.immediateFailedFuture(exception));
      }
      accountDao.delete(account);
      EasyMock.expectLastCall();
      EasyMock.expect(personDao.findById(account.getOwner())).andReturn(person);
      EasyMock.expect(placeDao.findById(place.getId())).andReturn(place);
      EasyMock.expect(accountDao.findById(account.getId())).andReturn(null);
      EasyMock.expect(authGrantDao.findForPlace(place.getId())).andReturn(ImmutableList.<AuthorizationGrant>of());           
      if (Boolean.TRUE.equals(deleteLogin)) {
    	  EasyMock.expect(personPlaceAssocDao.listPlaceAccessForPerson(person.getId())).andReturn(ImmutableList.<PlaceAccessDescriptor>of());
      }
      authGrantDao.removeForPlace(place.getId());
      EasyMock.expectLastCall();
      placeDao.delete(place);
      EasyMock.expectLastCall();
      
      if(Boolean.TRUE.equals(deleteLogin)) {
      	preferencesDao.deleteForPerson(account.getOwner());
      	EasyMock.expectLastCall();
         authGrantDao.removeGrantsForEntity(account.getOwner());
         EasyMock.expectLastCall();
         personDao.delete(person);
         EasyMock.expectLastCall();
      } else {
         EasyMock.expect(personDao.update(person)).andReturn(person);
         EasyMock.expect(authGrantDao.findForEntity(account.getOwner())).andReturn(Collections.emptyList());
      }

      replay();

      MessageBody body = handler.handleRequest(account, createDelete(deleteLogin));

      assertEquals("EmptyMessage", body.getMessageType());
      PlatformMessage msg = null;
      boolean done = false;
      boolean foundAccountDeletedEvent = false;
      boolean foundPlaceDeletedEvent = false;
      while(!done) {   
    	  try{
    		  msg = bus.take();
	    	  if(msg.getMessageType().equalsIgnoreCase(Capability.EVENT_DELETED)){
	    		  if(account.getAddress().equals(msg.getSource().getRepresentation())) {
	    			  foundAccountDeletedEvent = true;
	    		  }else if(place.getAddress().equals(msg.getSource().getRepresentation())) {
	    			  foundPlaceDeletedEvent = true;
	    		  }
	    	  }
    	  }catch(Exception e) {
    		  done = true;
    	  }
      }
      assertTrue("There should be an Account Deleted event", foundAccountDeletedEvent);
      assertTrue("There should be a Place Deleted event", foundPlaceDeletedEvent);
      
   }

   private PlatformMessage createDelete(Boolean deleteLogin) {
      MessageBody body = AccountCapability.DeleteRequest.builder()
            .withDeleteOwnerLogin(deleteLogin)
            .build();
      return PlatformMessage.buildMessage(body, clientAddress, Address.fromString(account.getAddress()))
            .withCorrelationId("correlationid")
            .isRequestMessage(true)
            .withPlaceId(UUID.randomUUID())
            .withPopulation(Population.NAME_GENERAL)
            .create();
   }
}

