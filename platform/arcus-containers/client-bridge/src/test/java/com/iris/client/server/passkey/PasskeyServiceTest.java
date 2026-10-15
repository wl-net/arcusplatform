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
package com.iris.client.server.passkey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.iris.client.server.passkey.PasskeyService.PasskeyException;
import com.iris.client.server.passkey.PasskeyService.PendingRequest;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.passkey.Passkey;

/**
 * Runs real WebAuthn ceremonies against a software authenticator, so the options
 * stored between steps, signatures and user handles are all checked for real.
 */
public class PasskeyServiceTest {
   private static final String RP_ID = "example.com";
   private static final String ORIGIN = "https://example.com";

   private InMemoryPasskeyDAO passkeyDao;
   private PersonDAO personDao;
   private PasskeyService service;
   private Person person;
   private SoftwareAuthenticator authenticator;

   @Before
   public void setUp() throws Exception {
      person = new Person();
      person.setId(UUID.randomUUID());
      person.setEmail("person@example.com");
      person.setFirstName("Pat");
      person.setLastName("Person");
      person.setHasLogin(true);

      personDao = EasyMock.createNiceMock(PersonDAO.class);
      EasyMock.expect(personDao.findByEmail(person.getEmail())).andReturn(person).anyTimes();
      EasyMock.expect(personDao.findById(person.getId())).andReturn(person).anyTimes();
      EasyMock.replay(personDao);

      passkeyDao = new InMemoryPasskeyDAO();
      service = new PasskeyService(config(RP_ID, ""), passkeyDao, personDao);
      authenticator = new SoftwareAuthenticator(ORIGIN);
   }

   @Test
   public void testRegisterThenLogin() throws Exception {
      Passkey passkey = register("Laptop");
      assertEquals(authenticator.getCredentialId(), passkey.getCredentialId());
      assertEquals("Laptop", passkey.getName());
      assertEquals(1, service.list(person.getId()).size());

      PendingRequest login = service.startLogin();
      assertEquals(person.getId(), service.finishLogin(login.getRequestId(), authenticator.get(login.getOptions())));

      Passkey stored = passkeyDao.findByCredentialId(passkey.getCredentialId());
      assertEquals(1, stored.getSignCount());
      assertNotNull(stored.getLastUsed());
   }

   @Test
   public void testTamperedSignatureIsRejected() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions(), ORIGIN, true));
   }

   @Test
   public void testWrongOriginIsRejected() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions(), "https://attacker.example", false));
   }

   @Test
   public void testLoginRequestIsSingleUse() throws Exception {
      register("Laptop");
      PendingRequest login = service.startLogin();
      String response = authenticator.get(login.getOptions());
      service.finishLogin(login.getRequestId(), response);
      assertLoginFails(login.getRequestId(), response);
   }

   @Test
   public void testUnknownPasskeyIsRejected() throws Exception {
      // the authenticator has a credential the server never registered
      register("Laptop");
      passkeyDao.passkeys.clear();
      PendingRequest login = service.startLogin();
      assertLoginFails(login.getRequestId(), authenticator.get(login.getOptions()));
   }

   @Test
   public void testRegistrationRequestBelongsToItsPerson() throws Exception {
      PendingRequest registration = service.startRegistration(person);
      String response = authenticator.create(registration.getOptions());
      Person other = new Person();
      other.setId(UUID.randomUUID());
      other.setEmail("other@example.com");
      try {
         service.finishRegistration(other, registration.getRequestId(), response, "Laptop");
         fail("registered a passkey from another person's request");
      }
      catch (PasskeyException expected) {
      }
      assertTrue(passkeyDao.passkeys.isEmpty());
   }

   @Test
   public void testRequestsCantCrossCeremonies() throws Exception {
      register("Laptop");
      PendingRequest registration = service.startRegistration(person);
      PendingRequest login = service.startLogin();
      // a registration request can't complete a login, and a login request can't register
      assertLoginFails(registration.getRequestId(), authenticator.get(login.getOptions()));
      try {
         service.finishRegistration(person, login.getRequestId(), authenticator.create(registration.getOptions()), "Phone");
         fail("registered with a login request");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testSamePasskeyCantBeRegisteredTwice() throws Exception {
      register("Laptop");
      PendingRequest registration = service.startRegistration(person);
      try {
         service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), "Again");
         fail("registered the same credential twice");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testDeleteOnlyOwnPasskeys() throws Exception {
      Passkey passkey = register("Laptop");
      assertFalse(service.delete(UUID.randomUUID(), passkey.getCredentialId()));
      assertEquals(1, passkeyDao.passkeys.size());
      assertTrue(service.delete(person.getId(), passkey.getCredentialId()));
      assertTrue(passkeyDao.passkeys.isEmpty());
   }

   @Test
   public void testDisabledWithoutRpId() throws Exception {
      PasskeyService disabled = new PasskeyService(config("", ""), passkeyDao, personDao);
      assertFalse(disabled.isEnabled());
      try {
         disabled.startLogin();
         fail("started a login with passkeys disabled");
      }
      catch (PasskeyException expected) {
      }
   }

   @Test
   public void testNormalizeName() {
      assertEquals("Passkey", PasskeyService.normalizeName(null));
      assertEquals("Passkey", PasskeyService.normalizeName("   "));
      assertEquals("Phone", PasskeyService.normalizeName(" Phone "));
      assertEquals(PasskeyService.MAX_NAME_LENGTH, PasskeyService.normalizeName(new String(new char[100]).replace('\0', 'x')).length());
   }

   @Test
   public void testUserHandleRoundTrip() {
      UUID id = UUID.randomUUID();
      assertEquals(id, PasskeyCredentialRepository.personId(PasskeyCredentialRepository.userHandle(id)).get());
   }

   private Passkey register(String name) throws Exception {
      PendingRequest registration = service.startRegistration(person);
      return service.finishRegistration(person, registration.getRequestId(), authenticator.create(registration.getOptions()), name);
   }

   private void assertLoginFails(String requestId, String response) {
      try {
         service.finishLogin(requestId, response);
         fail("login should have failed");
      }
      catch (PasskeyException expected) {
      }
   }

   private static PasskeyConfig config(String rpId, String origins) {
      PasskeyConfig config = new PasskeyConfig();
      config.setRpId(rpId);
      config.setOrigins(origins);
      return config;
   }
}
