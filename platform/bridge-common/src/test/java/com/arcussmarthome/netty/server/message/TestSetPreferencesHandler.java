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
package com.arcussmarthome.netty.server.message;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static com.arcussmarthome.messages.errors.Errors.invalidRequest;
import static com.arcussmarthome.messages.type.CardPreference.ATTR_HIDECARD;
import static com.arcussmarthome.messages.type.CardPreference.ATTR_SERVICENAME;
import static com.arcussmarthome.messages.type.CardPreference.SERVICENAME_CAMERAS;
import static com.arcussmarthome.messages.type.CardPreference.SERVICENAME_CLIMATE;
import static com.arcussmarthome.messages.type.CardPreference.SERVICENAME_LIGHTS_N_SWITCHES;
import static com.arcussmarthome.messages.type.CardPreference.SERVICENAME_SANTA_TRACKER;
import static com.arcussmarthome.messages.type.Preferences.ATTR_DASHBOARDCARDS;
import static com.arcussmarthome.messages.type.Preferences.ATTR_HIDETUTORIALS;
import static com.arcussmarthome.netty.server.message.SetPreferencesHandler.ACTIVE_PLACE_NOT_SET;
import static com.arcussmarthome.netty.server.message.SetPreferencesHandler.NAME_EXECUTOR;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.arcussmarthome.bridge.server.client.Client;
import com.arcussmarthome.bridge.server.session.Session;
import com.arcussmarthome.core.dao.PlaceDAO;
import com.arcussmarthome.core.dao.PreferencesDAO;
import com.arcussmarthome.core.platform.PlatformMessageBus;
import com.arcussmarthome.messages.ClientMessage;
import com.arcussmarthome.messages.MessageBody;
import com.arcussmarthome.messages.PlatformMessage;
import com.arcussmarthome.messages.service.SessionService.PreferencesChangedEvent;
import com.arcussmarthome.messages.service.SessionService.SetPreferencesRequest;
import com.arcussmarthome.messages.type.Population;
import com.arcussmarthome.test.IrisMockTestCase;
import com.arcussmarthome.test.Mocks;

@Mocks({Session.class, Client.class, PreferencesDAO.class, PlatformMessageBus.class, PlaceDAO.class})
public class TestSetPreferencesHandler extends IrisMockTestCase
{
   private static final UUID ACTIVE_PLACE_ID = UUID.fromString("e972a1b7-ec80-448e-8ae7-77da6a034577");
   private static final UUID PRINCIPAL_ID = UUID.fromString("101e036b-3a2b-4524-8f56-6428d0514572");
   private static final String PRINCIPAL_NAME = "Joe User";

   @Inject
   private Session mockSession;

   @Inject
   private Client mockClient;

   @Inject
   private PreferencesDAO mockPreferencesDao;

   @Inject
   private PlatformMessageBus mockPlatformMessageBus;
   
   @Inject
   private PlaceDAO mockPlaceDao;

   @Inject
   private SetPreferencesHandler componentUnderTest;

   @Provides
   @Singleton
   @Named(NAME_EXECUTOR)
   public Executor executor()
   {
      return directExecutor();
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      expect(mockSession.getClient()).andReturn(mockClient).anyTimes();
      expect(mockClient.getPrincipalName()).andReturn(PRINCIPAL_NAME).anyTimes();
      expect(mockClient.getPrincipalId()).andReturn(PRINCIPAL_ID).anyTimes();
      expect(mockPlaceDao.getPopulationById(EasyMock.anyObject(UUID.class))).andReturn(Population.NAME_GENERAL).anyTimes();
   }

   @Test
   public void testActivePlaceNotSet()
   {
      expect(mockSession.getActivePlace()).andReturn(null).anyTimes();

      replay();

      ClientMessage request = ClientMessage.builder().create();

      ClientMessage response = componentUnderTest.handle(request, mockSession);

      assertThat(response.getPayload(), equalTo(ACTIVE_PLACE_NOT_SET));
   }

   @Test
   public void testUnexpectedPref()
   {
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE_ID.toString()).anyTimes();

      replay();

      Map<String, Object> requestPrefs = ImmutableMap.of("someUnexpectedKey", true);
      MessageBody requestBody = SetPreferencesRequest.builder().withPrefs(requestPrefs).build();
      ClientMessage request = ClientMessage.builder().withPayload(requestBody).create();

      ClientMessage response = componentUnderTest.handle(request, mockSession);

      assertThat(response.getPayload(),
         equalTo(invalidRequest(SetPreferencesRequest.ATTR_PREFS + " contains unexpected keys")));
   }

   @Test
   public void testDuplicateCard()
   {
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE_ID.toString()).anyTimes();

      replay();

      Map<String, Object> requestPrefs = ImmutableMap.of(
         ATTR_DASHBOARDCARDS, ImmutableList.of(
            ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
            ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false),
            ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, false)));
      MessageBody requestBody = SetPreferencesRequest.builder().withPrefs(requestPrefs).build();
      ClientMessage request = ClientMessage.builder().withPayload(requestBody).create();

      ClientMessage response = componentUnderTest.handle(request, mockSession);

      assertThat(response.getPayload(),
         equalTo(invalidRequest("Duplicate dashboard card [" + SERVICENAME_LIGHTS_N_SWITCHES + "]")));
   }

   @Test
   public void testUnknownCard()
   {
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE_ID.toString()).anyTimes();

      replay();

      Map<String, Object> requestPrefs = ImmutableMap.of(
         ATTR_DASHBOARDCARDS, ImmutableList.of(
            ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
            ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false),
            ImmutableMap.of(ATTR_SERVICENAME, "SOME_UNKNOWN_CARD",           ATTR_HIDECARD, false)));
      MessageBody requestBody = SetPreferencesRequest.builder().withPrefs(requestPrefs).build();
      ClientMessage request = ClientMessage.builder().withPayload(requestBody).create();

      ClientMessage response = componentUnderTest.handle(request, mockSession);

      assertThat(response.getPayload(), equalTo(invalidRequest("Unrecognized dashboard card [SOME_UNKNOWN_CARD]")));
   }

   @Test
   public void testSetHideTutorials()
   {
      test(
         null,
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true)
         );
   }

   @Test
   public void testSetDashboardCards()
   {
      test(
         null,
         ImmutableMap.of(
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false))),
         ImmutableMap.of(
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false)))
         );
   }

   @Test
   public void testSetDashboardCardsWithExisting()
   {
      test(
         newHashMap(ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false)))),
         ImmutableMap.of(
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, true))),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, true)))
         );
   }

   @Test
   public void testSetAll()
   {
      test(
         null,
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false))),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false)))
         );
   }

   @Test
   public void testSetAllWithExisting()
   {
      test(
         newHashMap(ImmutableMap.of(
            ATTR_HIDETUTORIALS, true,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_LIGHTS_N_SWITCHES, ATTR_HIDECARD, true),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CLIMATE,           ATTR_HIDECARD, false)))),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, false,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, true))),
         ImmutableMap.of(
            ATTR_HIDETUTORIALS, false,
            ATTR_DASHBOARDCARDS, ImmutableList.of(
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_CAMERAS,           ATTR_HIDECARD, false),
               ImmutableMap.of(ATTR_SERVICENAME, SERVICENAME_SANTA_TRACKER,     ATTR_HIDECARD, true)))
         );
   }

   private void test(Map<String, Object> foundPrefs, Map<String, Object> requestPrefs,
      Map<String, Object> expectedPrefsToEmit)
   {
      expect(mockSession.getActivePlace()).andReturn(ACTIVE_PLACE_ID.toString()).anyTimes();

      expect(mockPreferencesDao.findById(PRINCIPAL_ID, ACTIVE_PLACE_ID)).andReturn(foundPrefs);

      mockPreferencesDao.merge(PRINCIPAL_ID, ACTIVE_PLACE_ID, requestPrefs);
      expectLastCall();

      Capture<PlatformMessage> messageCapture = newCapture();
      expect(mockPlatformMessageBus.send(capture(messageCapture))).andReturn(null);

      replay();

      MessageBody requestBody = SetPreferencesRequest.builder().withPrefs(requestPrefs).build();
      ClientMessage request = ClientMessage.builder().withPayload(requestBody).create();

      componentUnderTest.handle(request, mockSession);

      PlatformMessage message = messageCapture.getValue();
      assertThat(PreferencesChangedEvent.getPrefs(message.getValue()), equalTo(expectedPrefsToEmit));
   }

   @Override
   public void tearDown() throws Exception
   {
      verify();

      reset();

      super.tearDown();
   }
}

