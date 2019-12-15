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
package com.arcussmarthome.netty.bus;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.messaging.memory.InMemoryMessageModule;
import com.arcussmarthome.core.messaging.memory.InMemoryPlatformMessageBus;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.address.Address;
import com.arcussmarthome.messages.capability.Capability;
import com.arcussmarthome.messages.capability.PlaceCapability;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.population.PlacePopulationCacheValueChangeListener;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;
import com.arcussmarthome.test.Modules;

@Mocks({PlaceDAO.class})
@Modules({InMemoryMessageModule.class})
public class TestPlacePopulationCacheManager extends IrisMockTestCase {

	@Inject
	private PlacePopulationCacheValueChangeListener cacheMgr;
	@Inject
	private PlaceDAO mockPlaceDao;
	@Inject
	private InMemoryPlatformMessageBus mockMsgBus;
	
	@Test
	public void testGetExistingItem() throws Exception {
		UUID placeId = UUID.randomUUID();
		String population = Population.NAME_BETA;
		EasyMock.expect(mockPlaceDao.getPopulationById(placeId)).andReturn(population);
		replay();
		
		assertEquals(population, cacheMgr.getPopulationByPlaceId(placeId));
	}
	
	@Test
	public void testGetNonExistingItem() throws Exception {
		UUID placeId = UUID.randomUUID();
		
		EasyMock.expect(mockPlaceDao.getPopulationById(placeId)).andReturn(null);
		replay();
		
		assertEquals(Population.NAME_GENERAL, cacheMgr.getPopulationByPlaceId(placeId));
	}
	
	@Test
	public void testOnMessage() throws Exception {
		UUID placeId = UUID.randomUUID();
		String firstPopulation = Population.NAME_GENERAL;
		String secondPopulation = Population.NAME_BETA;
		EasyMock.expect(mockPlaceDao.getPopulationById(placeId)).andReturn(firstPopulation);  //1
		EasyMock.expect(mockPlaceDao.getPopulationById(placeId)).andReturn(secondPopulation);	//2
		replay();
		assertEquals(firstPopulation, cacheMgr.getPopulationByPlaceId(placeId));	//This call will trigger placeDao.getPopulationById at 1
		assertEquals(firstPopulation, cacheMgr.getPopulationByPlaceId(placeId));	//This call should not trigger placeDao.getPopulationById
		
		MessageBody body=MessageBody.buildMessage(Capability.EVENT_VALUE_CHANGE, 
				ImmutableMap.<String, Object>of(PlaceCapability.ATTR_POPULATION, secondPopulation));
           
         PlatformMessage event = PlatformMessage.buildEvent(body, Address.platformService(placeId, PlaceCapability.NAMESPACE))
               .to(Address.broadcastAddress())
               .withTimeToLive(86400000)
               .create();
		cacheMgr.onMessage(event);
		assertEquals(secondPopulation, cacheMgr.getPopulationByPlaceId(placeId));	//This call will trigger placeDao.getPopulationById at 2
	}
}

