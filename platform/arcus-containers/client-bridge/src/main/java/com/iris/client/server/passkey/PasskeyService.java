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

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.iris.core.dao.PasskeyDAO;
import com.iris.core.dao.PasskeyDAO.PasskeyRequest;
import com.iris.core.dao.PersonDAO;
import com.iris.messages.model.Person;
import com.iris.security.passkey.Passkey;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;

/**
 * Registers passkeys and verifies passkey logins (WebAuthn). Each ceremony has two
 * steps: start returns options for the browser along with a request id, and finish
 * takes the browser's response to that request. Requests are single-use and expire.
 *
 * Passkeys are discoverable and require user verification, so login doesn't need a
 * username and the passkey stands in for both the password and a second factor.
 */
@Singleton
public class PasskeyService {
   private static final Logger logger = LoggerFactory.getLogger(PasskeyService.class);

   static final int MAX_NAME_LENGTH = 64;
   private static final String DEFAULT_NAME = "Passkey";

   private final PasskeyConfig config;
   private final PasskeyDAO passkeyDao;
   private final @Nullable RelyingParty relyingParty;
   private final SecureRandom random = new SecureRandom();

   @Inject
   public PasskeyService(PasskeyConfig config, PasskeyDAO passkeyDao, PersonDAO personDao) {
      this.config = config;
      this.passkeyDao = passkeyDao;
      this.relyingParty = !config.isEnabled() ? null : RelyingParty.builder()
            .identity(RelyingPartyIdentity.builder().id(config.getRpId()).name(config.getRpName()).build())
            .credentialRepository(new PasskeyCredentialRepository(passkeyDao, personDao))
            .origins(config.getOrigins())
            .build();
      if (config.isEnabled()) {
         logger.info("Passkeys enabled for [{}] from {}", config.getRpId(), config.getOrigins());
      }
   }

   public boolean isEnabled() {
      return relyingParty != null;
   }

   public PendingRequest startRegistration(Person person) throws PasskeyException {
      RelyingParty rp = relyingParty();
      if (StringUtils.isBlank(person.getEmail())) {
         throw new PasskeyException("Only a person with a login can register a passkey");
      }
      String displayName = StringUtils.trimToNull(StringUtils.defaultString(person.getFirstName()) + " " + StringUtils.defaultString(person.getLastName()));
      PublicKeyCredentialCreationOptions options = rp.startRegistration(
            StartRegistrationOptions.builder()
               .user(UserIdentity.builder()
                     .name(person.getEmail())
                     .displayName(displayName != null ? displayName : person.getEmail())
                     .id(PasskeyCredentialRepository.userHandle(person.getId()))
                     .build())
               .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                     .residentKey(ResidentKeyRequirement.REQUIRED)
                     .userVerification(UserVerificationRequirement.REQUIRED)
                     .build())
               .build());
      try {
         return saveRequest(person.getId(), options.toJson(), options.toCredentialsCreateJson());
      }
      catch (IOException e) {
         throw new IllegalStateException("Unable to serialize registration options", e);
      }
   }

   public Passkey finishRegistration(Person person, String requestId, String credential, @Nullable String name) throws PasskeyException {
      RelyingParty rp = relyingParty();
      PasskeyRequest request = takeRequest(requestId);
      if (!Objects.equals(person.getId(), request.getPersonId())) {
         throw new PasskeyException("Unknown or expired request");
      }
      try {
         PublicKeyCredentialCreationOptions options = PublicKeyCredentialCreationOptions.fromJson(request.getRequest());
         RegistrationResult result = rp.finishRegistration(
               FinishRegistrationOptions.builder()
                  .request(options)
                  .response(com.yubico.webauthn.data.PublicKeyCredential.parseRegistrationResponseJson(credential))
                  .build());

         Passkey passkey = new Passkey();
         passkey.setPersonId(person.getId());
         passkey.setCredentialId(result.getKeyId().getId().getBase64Url());
         passkey.setUserHandle(options.getUser().getId().getBase64Url());
         passkey.setPublicKey(result.getPublicKeyCose().getBase64Url());
         passkey.setSignCount(result.getSignatureCount());
         passkey.setName(normalizeName(name));
         passkey.setCreated(Instant.now());
         passkeyDao.save(passkey);
         logger.info("person=[{}] registered passkey [{}]", person.getId(), passkey.getCredentialId()); // Audit event
         return passkey;
      }
      catch (RegistrationFailedException | IOException | IllegalArgumentException e) {
         logger.debug("Passkey registration failed for person [{}]", person.getId(), e);
         throw new PasskeyException("Passkey registration failed");
      }
   }

   public PendingRequest startLogin() throws PasskeyException {
      RelyingParty rp = relyingParty();
      AssertionRequest request = rp.startAssertion(
            StartAssertionOptions.builder()
               .userVerification(UserVerificationRequirement.REQUIRED)
               .build());
      try {
         return saveRequest(null, request.toJson(), request.toCredentialsGetJson());
      }
      catch (IOException e) {
         throw new IllegalStateException("Unable to serialize login options", e);
      }
   }

   /**
    * @return the id of the person the passkey belongs to
    */
   public UUID finishLogin(String requestId, String credential) throws PasskeyException {
      RelyingParty rp = relyingParty();
      PasskeyRequest request = takeRequest(requestId);
      if (request.getPersonId() != null) {
         // a registration request, not a login one
         throw new PasskeyException("Unknown or expired request");
      }
      try {
         AssertionResult result = rp.finishAssertion(
               FinishAssertionOptions.builder()
                  .request(AssertionRequest.fromJson(request.getRequest()))
                  .response(com.yubico.webauthn.data.PublicKeyCredential.parseAssertionResponseJson(credential))
                  .build());
         if (!result.isSuccess()) {
            throw new PasskeyException("Passkey login failed");
         }
         UUID personId = PasskeyCredentialRepository.personId(result.getCredential().getUserHandle())
               .orElseThrow(() -> new PasskeyException("Passkey login failed"));
         passkeyDao.updateUsage(personId, result.getCredential().getCredentialId().getBase64Url(), result.getSignatureCount(), Instant.now());
         return personId;
      }
      catch (AssertionFailedException | IOException | IllegalArgumentException e) {
         logger.debug("Passkey login failed", e);
         throw new PasskeyException("Passkey login failed");
      }
   }

   public List<Passkey> list(UUID personId) {
      return passkeyDao.findByPerson(personId);
   }

   /**
    * @return false if the person has no passkey with that id
    */
   public boolean delete(UUID personId, String credentialId) {
      boolean owned = passkeyDao.findByPerson(personId).stream().anyMatch(passkey -> passkey.getCredentialId().equals(credentialId));
      if (owned) {
         passkeyDao.delete(personId, credentialId);
         logger.info("person=[{}] removed passkey [{}]", personId, credentialId); // Audit event
      }
      return owned;
   }

   private RelyingParty relyingParty() throws PasskeyException {
      if (relyingParty == null) {
         throw new PasskeyException("Passkeys aren't enabled");
      }
      return relyingParty;
   }

   private PendingRequest saveRequest(@Nullable UUID personId, String request, String options) {
      byte[] id = new byte[32];
      random.nextBytes(id);
      String requestId = new ByteArray(id).getBase64Url();
      passkeyDao.saveRequest(requestId, personId, request, config.getRequestTtlSecs());
      return new PendingRequest(requestId, options);
   }

   private PasskeyRequest takeRequest(String requestId) throws PasskeyException {
      if (StringUtils.isBlank(requestId)) {
         throw new PasskeyException("Unknown or expired request");
      }
      return passkeyDao.takeRequest(requestId).orElseThrow(() -> new PasskeyException("Unknown or expired request"));
   }

   static String normalizeName(@Nullable String name) {
      String trimmed = StringUtils.trimToNull(name);
      if (trimmed == null) {
         return DEFAULT_NAME;
      }
      return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
   }

   /** Options to pass to the browser, and the id to send back with its response. */
   public static class PendingRequest {
      private final String requestId;
      private final String options;

      PendingRequest(String requestId, String options) {
         this.requestId = requestId;
         this.options = options;
      }

      public String getRequestId() {
         return requestId;
      }

      /**
       * The JSON for navigator.credentials.create() or .get(), {@code {"publicKey": {...}}},
       * with binary values base64url-encoded.
       */
      public String getOptions() {
         return options;
      }
   }

   @SuppressWarnings("serial")
   public static class PasskeyException extends Exception {
      public PasskeyException(String message) {
         super(message);
      }
   }
}
